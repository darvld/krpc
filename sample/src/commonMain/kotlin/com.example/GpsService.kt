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

package com.example

import com.example.model.Location
import com.example.model.Vehicle
import io.github.darvld.krpc.*
import kotlinx.coroutines.flow.Flow

/**A GPS tracking service used to manage the [Location] of different [Vehicle] instances.*/
@Service
interface GpsService {

    /**Authentication handshake using the gRPC Metadata API.*/
    @UnaryCall
    suspend fun handshake()

    /**Returns a list of all the vehicles currently tracked by this service.*/
    @UnaryCall
    suspend fun listVehicles(): List<Vehicle>

    /**Registers a new vehicle with the service.*/
    @UnaryCall
    suspend fun addVehicle(id: Long, info: String, location: Location): Boolean

    /**Returns the last known location of a given [vehicle].*/
    @UnaryCall
    suspend fun locationForVehicle(vehicle: Vehicle): Location

    /**Starts tracking the given [vehicle], the returned flow emits a new value every time the vehicle's
     * [Location] is updated.*/
    @ServerStream
    fun trackVehicle(vehicle: Vehicle): Flow<Location>

    /**Streams a [route] followed by a vehicle. The vehicle information should be provided through metadata.
     *
     * @return Whether the route was successfully received by the server.*/
    @ClientStream
    suspend fun streamRoute(route: Flow<Location>): Boolean

    /**Starts a continuous tracking session, where the client can change the tracked vehicle at any
     * given time.
     *
     * The returned flow will contain the last known position of the currently tracked vehicle. When changing the
     * tracked vehicle, the server will first send [Location.TRACKING_CHANGED], and then start sending the route
     * for the corresponding vehicle.*/
    @BidiStream
    fun continuousTracking(vehicles: Flow<Vehicle>): Flow<Location>

}