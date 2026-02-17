package com.engyh.friendhub.presentation.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(
    private val context: Context,
    caller: ActivityResultCaller,
    private val onLocationReady: (Location) -> Unit,
    private val onLocationDenied: () -> Unit
    ) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var cancellationTokenSource: CancellationTokenSource? = null

    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        caller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fine || coarse) checkGpsAndFetch()
            else {
                (context as? Activity)?.showSnackbar("Location permission denied")
                onLocationDenied()
            }
        }

    fun checkAndRequestLocation() {
        if (hasLocationPermission()) {
            checkGpsAndFetch()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun checkGpsAndFetch() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!enabled) {
            onLocationDenied()
            AlertDialog.Builder(context)
                .setTitle("Enable Location Services")
                .setMessage("Location services are required. Enable now?")
                .setPositiveButton("Open Settings") { _, _ ->
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (!hasLocationPermission()) {
            checkAndRequestLocation()
            return
        }

        cancellationTokenSource?.cancel()
        cancellationTokenSource = CancellationTokenSource()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedClient
        val token = cancellationTokenSource?.token ?: return
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token)
            .addOnSuccessListener { location ->
                if (location != null) onLocationReady(location)
                else {
                    onLocationDenied()
                    (context as? Activity)?.showSnackbar("Unable to get location")
                }
            }
            .addOnFailureListener { e ->
                onLocationDenied()
                (context as? Activity)?.showSnackbar("Failed: ${e.message}")
            }
    }

    fun release() {
        cancellationTokenSource?.cancel()
        cancellationTokenSource = null
    }
}