package generated

import GpsService
import SerializationProvider
import io.grpc.ServerServiceDefinition
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.ServerCalls
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class GpsServiceProvider(
    serializationProvider: SerializationProvider,
    context: CoroutineContext = EmptyCoroutineContext
) : AbstractCoroutineServerImpl(context), GpsService {
    private val definitions = GpsServiceDefinitions(serializationProvider)

    final override fun bindService(): ServerServiceDefinition {
        return ServerServiceDefinition.builder("generated.GpsService")
            .addMethod(
                ServerCalls.unaryServerMethodDefinition(
                    context = context,
                    descriptor = definitions.locationForVehicle,
                    implementation = ::locationForVehicle
                )
            )
            .addMethod(
                ServerCalls.serverStreamingServerMethodDefinition(
                    context = context,
                    descriptor = definitions.routeForVehicle,
                    implementation = ::routeForVehicle
                )
            )
            .addMethod(
                ServerCalls.clientStreamingServerMethodDefinition(
                    context = context,
                    descriptor = definitions.streamRoute,
                    implementation = ::streamRoute,
                )
            )
            .addMethod(
                ServerCalls.bidiStreamingServerMethodDefinition(
                    context = context,
                    descriptor = definitions.transformRoute,
                    implementation = ::transformRoute
                )
            )
            .build()
    }
}