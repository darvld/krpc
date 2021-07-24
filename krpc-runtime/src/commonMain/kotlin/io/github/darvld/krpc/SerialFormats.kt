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

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer

/**A [SerializationProvider] that uses a [BinaryFormat], such as ProtoBuf, to provide [Transcoder] instances.*/
@OptIn(ExperimentalSerializationApi::class)
expect value class BinarySerializationProvider(val format: BinaryFormat) : SerializationProvider

/**Creates a new [Transcoder] using this serialization format.*/
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> BinaryFormat.transcoder(): Transcoder<T> {
    return BinarySerializationProvider(this).transcoderFor(serializer())
}

/**A [SerializationProvider] that uses a [StringFormat], such as Json, to provide [Transcoder] instances.*/
@OptIn(ExperimentalSerializationApi::class)
expect value class StringSerializationProvider(val format: StringFormat) : SerializationProvider

/**Creates a new [Transcoder] using this serialization format.*/
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> StringFormat.transcoder(): Transcoder<T> {
    return StringSerializationProvider(this).transcoderFor(serializer())
}