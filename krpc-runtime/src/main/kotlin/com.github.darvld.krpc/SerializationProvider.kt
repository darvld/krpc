package com.github.darvld.krpc

import io.grpc.MethodDescriptor
import kotlinx.serialization.KSerializer

interface SerializationProvider {
    fun <T> marshallerFor(serializer: KSerializer<T>): MethodDescriptor.Marshaller<T>
}
