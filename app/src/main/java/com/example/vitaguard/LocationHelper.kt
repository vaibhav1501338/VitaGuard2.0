package com.example.vitaguard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Request settings for getting location updates (used if we wanted continuous updates)
    private val locationRequest: LocationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000 // Update interval in milliseconds
    ).setMinUpdateDistanceMeters(10f).build() // Minimum distance change in meters

    /**
     * Gets the last known location and executes a callback function upon success.
     */
    fun getLastLocation(onSuccess: (Location) -> Unit, onFailure: () -> Unit) {
        // 1. Check for necessary permissions
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onFailure()
            return
        }

        // 2. Request last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                onSuccess(location)
            } else {
                // If last location is null (e.g., service restart), try requesting a fresh update
                requestSingleLocationUpdate(onSuccess, onFailure)
            }
        }.addOnFailureListener {
            onFailure()
        }
    }

    /**
     * Requests a single, fresh location update when last location is unavailable.
     */
    private fun requestSingleLocationUpdate(onSuccess: (Location) -> Unit, onFailure: () -> Unit) {
        // Location callback to handle the fresh update
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Stop updates immediately after receiving one result
                fusedLocationClient.removeLocationUpdates(this)

                locationResult.lastLocation?.let {
                    onSuccess(it)
                } ?: onFailure()
            }
        }

        // Request a single update
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            onFailure()
        }
    }
}
