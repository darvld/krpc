/*
 *    Copyright 2021 Dario Valdespino.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
