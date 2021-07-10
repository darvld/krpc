package com.example.backend

import com.example.GpsServiceProvider
import com.example.Simulation.moderateDelay
import com.example.Simulation.randomLocation
import com.example.Simulation.shortDelay
import com.example.model.Location
import com.example.model.Vehicle
import com.github.darvld.krpc.SerializationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlin.random.Random

/**A basic implementation of a [GpsServiceProvider], used for illustration purposes.*/
class GpsServer(
    serializationProvider: SerializationProvider,
) : GpsServiceProvider(serializationProvider) {

    override suspend fun listVehicles(): List<Vehicle> {
        // Return a list of randomly generated vehicles
        return List(5) {
            Vehicle(Random.nextLong(1000, 9999), "SampleVehicle-SV$it")
        }
    }

    override suspend fun locationForVehicle(vehicle: Vehicle): Location {
        // Pretend we're doing something here
        shortDelay()
        return randomLocation()
    }

    override fun trackVehicle(vehicle: Vehicle): Flow<Location> = flow {
        // Avoid randomizing every emission
        var location = randomLocation()

        // Endlessly track this vehicle until the rpc is cancelled
        while (true) {
            // Add delay between location updates
            moderateDelay()
            // Move it a little (~10m) in a straight line
            emit(location.copy(latitude = location.latitude + 0.001).also { location = it })
        }
    }

    override suspend fun streamRoute(route: Flow<Location>): Boolean {
        val routeCode = route.hashCode()
        println("[Server] Now receiving a route stream (#$routeCode)")

        route.collect {
            println("[Server] Received $it from route stream #$routeCode")
            // Pretend we did something useful with the location
            shortDelay()
        }
        return true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun continuousTracking(vehicles: Flow<Vehicle>): Flow<Location> = flow {
        println("[Server] Now entering continuous tracking mode")

        emitAll(vehicles.flatMapLatest { current ->
            println("[Server] Now tracking $current")

            flow {
                // Notify the client that the next location belongs to another vehicle
                emit(Location.TRACKING_CHANGED)
                emitAll(trackVehicle(current).take(5))
            }
        })

        println("[Server] Now exiting continuous tracking mode")
    }
}