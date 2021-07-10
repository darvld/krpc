package com.example.model

import com.example.model.Location.Companion.TRACKING_CHANGED
import kotlinx.serialization.Serializable

/**Location returned by a [GpsService][com.example.GpsService], usually corresponding to a [Vehicle] tracked by the service.*/
@Serializable
data class Location(
    /**The latitude component of this location.*/
    val latitude: Double,
    /**The longitude component of this location.*/
    val longitude: Double,
    /**An optional tag used as a metadata field, for example, in the [TRACKING_CHANGED] constant.*/
    val tag: String = ""
) {
    companion object {
        /**A special [Location] instance used to signal the change of the currently tracked vehicle when
         *  [continuous tracking][com.example.GpsService.continuousTracking] is used.*/
        val TRACKING_CHANGED = Location(0.0, 0.0, "TrackingChanged")
    }
}
