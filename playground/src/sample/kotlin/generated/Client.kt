package generated

import GpsService
import Location
import SerializationProvider
import Vehicle
import io.grpc.ManagedChannel
import io.grpc.kotlin.ClientCalls
import kotlinx.coroutines.flow.Flow

fun GpsService(
    channel: ManagedChannel,
    serializationProvider: SerializationProvider
): GpsService = object : GpsService {
    private val definitions = GpsServiceDefinitions(serializationProvider)

    override suspend fun locationForVehicle(vehicle: Vehicle): Location {
        return ClientCalls.unaryRpc(channel, definitions.locationForVehicle, vehicle)
    }

    override fun routeForVehicle(vehicle: Vehicle): Flow<Location> {
        return ClientCalls.serverStreamingRpc(channel, definitions.routeForVehicle, vehicle)
    }

    override suspend fun streamRoute(route: Flow<Location>): Boolean {
        return ClientCalls.clientStreamingRpc(channel, definitions.streamRoute, route)
    }

    override fun transformRoute(route: Flow<Location>): Flow<Location> {
        return ClientCalls.bidiStreamingRpc(channel, definitions.transformRoute, route)
    }
}