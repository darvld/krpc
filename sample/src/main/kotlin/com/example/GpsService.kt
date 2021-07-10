package com.example

import com.example.model.Location
import com.example.model.Vehicle
import com.github.darvld.krpc.*
import kotlinx.coroutines.flow.Flow

/**A GPS tracking service used to manage the [Location] of different [Vehicle] instances.*/
@Service
interface GpsService {

    /**Returns a list of all the vehicles currently tracked by this service.*/
    @UnaryCall
    suspend fun listVehicles(): List<Vehicle>

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