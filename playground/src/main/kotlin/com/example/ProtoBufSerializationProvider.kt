import com.github.darvld.krpc.SerializationProvider
import io.grpc.MethodDescriptor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.ByteArrayInputStream
import java.io.InputStream

@OptIn(ExperimentalSerializationApi::class)
object ProtoBufSerializationProvider : SerializationProvider {
    override fun <T> marshallerFor(serializer: KSerializer<T>): MethodDescriptor.Marshaller<T> {
        return object : MethodDescriptor.Marshaller<T> {
            override fun parse(stream: InputStream): T {
                return ProtoBuf.decodeFromByteArray(serializer, stream.readAllBytes())
            }

            override fun stream(value: T): InputStream {
                return ByteArrayInputStream(ProtoBuf.encodeToByteArray(serializer, value))
            }
        }
    }
}