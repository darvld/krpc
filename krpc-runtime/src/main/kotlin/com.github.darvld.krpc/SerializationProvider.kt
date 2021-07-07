package com.github.darvld.krpc

import io.grpc.MethodDescriptor
import kotlinx.serialization.KSerializer
import java.io.InputStream

/**Abstraction used by KRPC to create format-agnostic [MethodDescriptor.Marshaller] implementations.
 *
 * Every [SerializationProvider] is required to provide a factory method used to create a marshaller for
 * a serializable type. You could implement a provider like the following:
 * ```
 * object ProtoBufSerializationProvider : SerializationProvider {
 *    override fun <T> marshallerFor(serializer: KSerializer<T>): MethodDescriptor.Marshaller<T> {
 *      return object : MethodDescriptor.Marshaller<T> {
 *          override fun parse(stream: InputStream): T {
 *              return ProtoBuf.decodeFromByteArray(serializer, stream.readAllBytes())
 *          }
 *
 *          override fun stream(value: T): InputStream {
 *              return ByteArrayInputStream(ProtoBuf.encodeToByteArray(serializer, value))
 *          }
 *      }
 *    }
 * }
 * ```
 * */
interface SerializationProvider {
    /**Contract method used to generate a marshaller for a serializable type.*/
    fun <T> marshallerFor(serializer: KSerializer<T>): MethodDescriptor.Marshaller<T>

    /**A noop marshaller used for the [Unit] type.*/
    object UnitMarshaller : MethodDescriptor.Marshaller<Unit> {
        override fun parse(stream: InputStream?) = Unit

        override fun stream(value: Unit?): InputStream = InputStream.nullInputStream()
    }
}
