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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**Serialization providers are responsible for creating format-specific [Transcoder] instances for any type
 * given the type's [KSerializer].
 *
 * This interface is used by the service components to generically plug into the `kotlinx.serialization` API.*/
interface SerializationProvider {
    /**Returns a [Transcoder] that uses [serializer] to encode/decode between [T] and a platform-specific encoded format.*/
    fun <T> transcoderFor(serializer: KSerializer<T>): Transcoder<T>
}

/**Reified extension used to obtain a [Transcoder] from this provider without explicitly passing the KSerializer.*/
inline fun <reified T> SerializationProvider.transcoder(): Transcoder<T> {
    return transcoderFor(serializer())
}