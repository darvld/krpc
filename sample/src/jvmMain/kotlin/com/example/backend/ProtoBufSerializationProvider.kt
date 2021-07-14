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

import io.github.darvld.krpc.SerializationProvider
import io.github.darvld.krpc.Transcoder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import java.io.ByteArrayInputStream
import java.io.InputStream

/**A simple implementation of a [SerializationProvider] using [ProtoBuf] to create gRPC marshallers.*/
@OptIn(ExperimentalSerializationApi::class)
actual object ProtoBufSerializationProvider : SerializationProvider {
    override fun <T> transcoderFor(serializer: KSerializer<T>): Transcoder<T> {
        return object : Transcoder<T> {
            override fun decode(from: InputStream): T {
                return ProtoBuf.decodeFromByteArray(serializer, from.readAllBytes())
            }

            override fun encode(value: T): InputStream {
                return ByteArrayInputStream(ProtoBuf.encodeToByteArray(serializer, value))
            }
        }
    }

    actual inline fun <reified T> transcoder(): Transcoder<T> {
        return transcoderFor(serializer())
    }
}