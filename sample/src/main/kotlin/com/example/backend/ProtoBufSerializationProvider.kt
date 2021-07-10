/*
 *    Copyright 2021 Dario Valdespino.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.example.backend

import com.github.darvld.krpc.SerializationProvider
import io.grpc.MethodDescriptor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.ByteArrayInputStream
import java.io.InputStream

/**A simple implementation of a [SerializationProvider] using [ProtoBuf] to create gRPC marshallers.*/
@OptIn(ExperimentalSerializationApi::class)
object ProtoBufSerializationProvider : SerializationProvider {
    override fun <T> marshallerFor(serializer: KSerializer<T>) = object : MethodDescriptor.Marshaller<T> {
        override fun parse(stream: InputStream): T {
            return ProtoBuf.decodeFromByteArray(serializer, stream.readAllBytes())
        }

        override fun stream(value: T): InputStream {
            return ByteArrayInputStream(ProtoBuf.encodeToByteArray(serializer, value))
        }
    }
}