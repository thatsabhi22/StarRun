package com.theleafapps.pro.starrun.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.PolylineOptions
import com.theleafapps.pro.starrun.R
import com.theleafapps.pro.starrun.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.theleafapps.pro.starrun.other.Constants.POLYLINE_COLOR
import com.theleafapps.pro.starrun.other.Constants.POLYLINE_WIDTH
import com.theleafapps.pro.starrun.services.PolyLine
import com.theleafapps.pro.starrun.services.TrackingService
import com.theleafapps.pro.starrun.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.*
import timber.log.Timber

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private val viewModel : MainViewModel by viewModels()

    private var isTracking = false
    private var pathPoints = mutableListOf<PolyLine>()

    private var map: GoogleMap? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.onCreate(savedInstanceState)

        btnToggleRun.setOnClickListener{
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }

        mapView.getMapAsync {
            map = it
        }
    }

    // Joining the Second last Coordinates with the last location Coordinates
    // This function will only join the last and second last coordinates
    // but won't join all the polylines, when we rotate the device
    private fun addLatestPolyline(){
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1){
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polyLineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)
            map?.addPolyline(polyLineOptions)
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(),TrackingService::class.java).also{
            it.action = action
            requireContext().startService(it)
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }
}