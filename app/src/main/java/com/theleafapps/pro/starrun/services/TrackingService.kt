package com.theleafapps.pro.starrun.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.theleafapps.pro.starrun.R
import com.theleafapps.pro.starrun.other.Constants.ACTION_PAUSE_SERVICE
import com.theleafapps.pro.starrun.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.theleafapps.pro.starrun.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.theleafapps.pro.starrun.other.Constants.ACTION_STOP_SERVICE
import com.theleafapps.pro.starrun.other.Constants.NOTIFICATION_CHANNEL_ID
import com.theleafapps.pro.starrun.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.theleafapps.pro.starrun.other.Constants.NOTIFICATION_ID
import com.theleafapps.pro.starrun.ui.MainActivity
import timber.log.Timber

typealias PolyLine =  MutableList<LatLng>
typealias PolyLines =  MutableList<PolyLine>

class TrackingService : LifecycleService() {

    var isFirstRun = true

    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<PolyLines>()
    }

    // Adding Empty list to Livedata initially
    private fun postInitialValues(){
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
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
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused service")
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("Stopped service")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
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

        addEmptyPolyLine()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel(notificationManager)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
            .setContentTitle("Running App")
            .setContentText("00:00:00")
            .setContentIntent(getMainActivityPendingIntent())

        startForeground(NOTIFICATION_ID,notificationBuilder.build())
    }

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this,MainActivity::class.java).also{
            it.action = ACTION_SHOW_TRACKING_FRAGMENT
        },
        FLAG_UPDATE_CURRENT
    )

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