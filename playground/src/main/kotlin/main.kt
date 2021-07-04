import com.example.GpsServer
import com.example.GpsService
import com.example.GpsServiceDescriptor
import com.github.darvld.krpc.SerializationProvider
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {

    val server = ServerBuilder.forPort(8980)
        .addService(GpsServer(ProtoBufSerializationProvider))
        .build()

    server.start()

    val channel = ManagedChannelBuilder.forAddress("localhost", 8980).build()
    val client = GpsClient(channel, ProtoBufSerializationProvider)

    repeat(5) {
        val code = client.doSomething("Message #$it")
        println("Server returned $code")
    }

    server.shutdown()
    server.awaitTermination()
}

class GpsClient private constructor(
    channel: Channel,
    callOptions: CallOptions = CallOptions.DEFAULT,
    private val definitions: GpsServiceDescriptor,
) : AbstractCoroutineStub<GpsClient>(channel, callOptions), GpsService {

    constructor(channel: Channel, serializationProvider: SerializationProvider) : this(
        channel,
        CallOptions.DEFAULT,
        GpsServiceDescriptor(serializationProvider),
    )

    fun withSerializationProvider(provider: SerializationProvider): GpsClient {
        return GpsClient(channel, callOptions, GpsServiceDescriptor(provider))
    }

    override fun build(channel: Channel, callOptions: CallOptions): GpsClient {
        return GpsClient(channel, callOptions, definitions)
    }

    override suspend fun doSomething(message: String): Int {
        return ClientCalls.unaryRpc(channel, definitions.doSomething, message, callOptions)
    }
}
