import com.example.GpsServer
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Test

class Sample {
    @Test
    fun sample() = runBlocking {

        val server = ServerBuilder.forPort(8980)
            .addService(GpsServer(ProtoBufSerializationProvider))
            .build()

        server.start()

        val channel = ManagedChannelBuilder.forAddress("localhost", 8980)
            .usePlaintext()
            .build()

        repeat(5) {
            val client = GpsClient(channel, ProtoBufSerializationProvider)
            val code = client.doSomething("Message #$it")
            println("Server returned $code")
        }

        server.shutdown()
        server.awaitTermination()
    }
}