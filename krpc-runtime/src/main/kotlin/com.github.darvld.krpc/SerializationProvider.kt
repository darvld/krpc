package com.github.darvld.krpc

import io.grpc.MethodDescriptor
import kotlinx.serialization.KSerializer
import java.io.InputStream

interface SerializationProvider {
    fun <T> marshallerFor(serializer: KSerializer<T>): MethodDescriptor.Marshaller<T>

    /**A noop marshaller used for the [Unit] type.*/
    object UnitSerializer : MethodDescriptor.Marshaller<Unit> {
        override fun parse(stream: InputStream?) = Unit

        override fun stream(value: Unit?): InputStream = InputStream.nullInputStream()
    }
}
