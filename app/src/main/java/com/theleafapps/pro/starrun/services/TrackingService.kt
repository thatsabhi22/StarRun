package com.theleafapps.pro.starrun.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.LocationRequest
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.theleafapps.pro.starrun.R
import com.theleafapps.pro.starrun.other.Constants.ACTION_PAUSE_SERVICE
import com.theleafapps.pro.starrun.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.theleafapps.pro.starrun.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.theleafapps.pro.starrun.other.Constants.ACTION_STOP_SERVICE
import com.theleafapps.pro.starrun.other.Constants.FASTEST_LOCATION_INTERVAL
import com.theleafapps.pro.starrun.other.Constants.LOCATION_UPDATE_INTERVAL
import com.theleafapps.pro.starrun.other.Constants.NOTIFICATION_CHANNEL_ID
import com.theleafapps.pro.starrun.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.theleafapps.pro.starrun.other.Constants.NOTIFICATION_ID
import com.theleafapps.pro.starrun.other.Constants.TIMER_UPDATE_INTERVAL
import com.theleafapps.pro.starrun.other.TrackingUtility
import com.theleafapps.pro.starrun.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

typealias PolyLine =  MutableList<LatLng>
typealias PolyLines =  MutableList<PolyLine>

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    var isFirstRun = true

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var curNotificationBuilder: NotificationCompat.Builder

    private val timeRunInSeconds = MutableLiveData<Long>()

    companion object {
        val timeRunInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<PolyLines>()
    }

    // Adding Empty list to Livedata initially
    private fun postInitialValues(){
        timeRunInSeconds.postValue(0L)
        timeRunInMillis.postValue(0L)
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
    }

    override fun onCreate() {
        super.onCreate()
        curNotificationBuilder = baseNotificationBuilder
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        isTracking.observe(this, Observer {
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(isFirstRun){
                        startForegroundService()
                        isFirstRun = false
                    } else {
                        Timber.d("Resuming service")
                        startTimer()
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused service")
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("Stopped service")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L

    private fun startTimer(){
        addEmptyPolyLine()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true

        CoroutineScope(Dispatchers.Main).launch {
            while(isTracking.value!!){
                lapTime = System.currentTimeMillis() - timeStarted

                //post new lapTime
                timeRunInMillis.postValue(timeRun + lapTime)

                if(timeRunInMillis.value!! >= lastSecondTimestamp + 1000L){
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay (TIMER_UPDATE_INTERVAL)
            }
            timeRun += lapTime
        }
    }

    // Updating the notification every second
    private fun updateNotificationTrackingState(isTracking: Boolean){
        val notificationActionText = if(isTracking) "Pause" else "Resume"
        val pendingIntent = if(isTracking){
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this,1,pauseIntent, FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, FLAG_UPDATE_CURRENT)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

         curNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
             isAccessible = true
             set(curNotificationBuilder, ArrayList<NotificationCompat.Action>())
             curNotificationBuilder = baseNotificationBuilder
                 .addAction(R.drawable.ic_pause_black_24dp, notificationActionText, pendingIntent)
             notificationManager.notify(NOTIFICATION_ID, curNotificationBuilder.build())

         }
    }

    private fun pauseService(){
        isTracking.postValue(false)
        isTimerEnabled = false
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean){
        if(isTracking){
            if(TrackingUtility.hasLocationPermission(this)){
                val locationRequest = LocationRequest().apply {
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FASTEST_LOCATION_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    // This callback is an event like As soon as location is recieved
    // addPathPoint() is called that posts the location value to the
    // Pathpoint live data
    val locationCallback = object : LocationCallback(){
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if(isTracking.value!!){
                result?.locations?.let { locations ->
                    for(location in locations){
                        addPathPoint(location)
                        Timber.d("New Location: ${location.latitude}, ${location.longitude}")
                    }
                }
            }
        }
    }

    // Posting Latlong values to the pathpoint livedata,
    // so that it gets observed
    private fun addPathPoint(location: Location?){
        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(pos)
                pathPoints.postValue(this)
            }
        }
    }

    // Adding Empty polyline on start of the tracking service
    private fun addEmptyPolyLine() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    private fun startForegroundService(){
        startTimer()
        isTracking.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel(notificationManager)
        }

        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        timeRunInSeconds.observe(this, Observer {
            val notification = curNotificationBuilder
                .setContentText(TrackingUtility.getFormattedStopWatchTime(it * 1000))
            notificationManager.notify(NOTIFICATION_ID, notification.build())
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW      // As the notification will keep on updating, this should be
                                // kept with importance low, otherwise notification ringing
                                // continuously will annoy the user
        )
        notificationManager.createNotificationChannel(channel)
    }
}