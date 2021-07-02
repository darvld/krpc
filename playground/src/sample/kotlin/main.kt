import generated.GpsService
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val server = ServerBuilder.forPort(8980)
        .addService(GpsServer(ProtoBufSerializationProvider))
        .build()

    server.start()

    launch {
        val channel = ManagedChannelBuilder.forAddress("localhost", 8980)
            .usePlaintext()
            .build()


        val service = GpsService(channel, ProtoBufSerializationProvider)
        val vehicle = Vehicle(14, "*****")

        println("Current location: ${service.locationForVehicle(vehicle)}")

        println("Route:")
        service.routeForVehicle(vehicle).collect {
            println(it)
        }

        val route = flow {
            repeat(5) {
                emit(GpsServer.randomLocation())
                delay(100)
            }
        }

        println("Streaming route")
        service.streamRoute(route)

        println("Mapping route")
        service.transformRoute(route).collect {
            println("Transformed: $it")
        }
    }.join()

    server.shutdown()
    server.awaitTermination()
}