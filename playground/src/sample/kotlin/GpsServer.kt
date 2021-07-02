import generated.GpsServiceProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random

class GpsServer(
    serializationProvider: SerializationProvider,
    context: CoroutineContext = EmptyCoroutineContext
) : GpsServiceProvider(serializationProvider, context) {
    companion object {
        fun randomLocation(): Location {
            return Location(Random.nextDouble(-180.0, 180.0), Random.nextDouble(-180.0, 180.0))
        }
    }

    override suspend fun locationForVehicle(vehicle: Vehicle): Location {
        return randomLocation()
    }

    override fun routeForVehicle(vehicle: Vehicle): Flow<Location> = flow {
        repeat(5) {
            emit(randomLocation())
            delay(200)
        }
    }

    override suspend fun streamRoute(route: Flow<Location>): Boolean {
        route.collect {
            println("Server received $it")
        }
        return true
    }

    override fun transformRoute(route: Flow<Location>): Flow<Location> {
        return route.map {
            println("Server received $it")
            randomLocation()
        }
    }
}