package com.engyh.friendhub.presentation.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.util.Locale

object GeocodingHelper {

    fun getCityFromLatLng(
        context: Context,
        lat: Double,
        lon: Double,
        onResult: (String?) -> Unit
    ) {

        val locale = Locale.Builder()
            .setLanguage("en")
            .build()

        val geocoder = Geocoder(context, locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            geocoder.getFromLocation(
                lat,
                lon,
                1,
                object : Geocoder.GeocodeListener {

                    override fun onGeocode(addresses: MutableList<Address>) {
                        onResult(extractCity(addresses))
                    }

                    override fun onError(errorMessage: String?) {
                        onResult(null)
                    }
                }
            )

        } else {

            @Suppress("DEPRECATION")
            try {
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                onResult(extractCity(addresses))
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    private fun extractCity(addresses: List<Address>?): String? {
        if (addresses.isNullOrEmpty()) return null

        val address = addresses[0]

        return address.adminArea
            ?: address.locality
            ?: address.subAdminArea
    }

}
