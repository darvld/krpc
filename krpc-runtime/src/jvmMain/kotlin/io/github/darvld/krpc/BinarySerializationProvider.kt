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

package io.github.darvld.krpc

import kotlinx.serialization.*
import java.io.ByteArrayInputStream
import java.io.InputStream


@JvmInline
@OptIn(ExperimentalSerializationApi::class)
actual value class BinarySerializationProvider actual constructor(
    actual val format: BinaryFormat
) : SerializationProvider {
    override fun <T> transcoderFor(serializer: KSerializer<T>): Transcoder<T> {
        return object : Transcoder<T> {
            override fun decode(stream: InputStream): T {
                return format.decodeFromByteArray(serializer, stream.readAllBytes())
            }

            override fun encode(value: T): InputStream {
                return ByteArrayInputStream(format.encodeToByteArray(serializer, value))
            }
        }
    }
}

@JvmInline
@OptIn(ExperimentalSerializationApi::class)
actual value class StringSerializationProvider actual constructor(
    actual val format: StringFormat
) : SerializationProvider {
    override fun <T> transcoderFor(serializer: KSerializer<T>): Transcoder<T> {
        return object : Transcoder<T> {
            override fun decode(stream: InputStream): T {
                val string = stream.readAllBytes().decodeToString()
                return format.decodeFromString(serializer, string)
            }

            override fun encode(value: T): InputStream {
                return ByteArrayInputStream(format.encodeToString(serializer, value).encodeToByteArray())
            }
        }
    }
}