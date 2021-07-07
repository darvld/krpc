import com.example.GpsClient
import com.example.GpsServer
import com.example.GpsService
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
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