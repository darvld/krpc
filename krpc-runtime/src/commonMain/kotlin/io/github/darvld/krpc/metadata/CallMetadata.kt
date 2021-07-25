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

package io.github.darvld.krpc.metadata

import io.github.darvld.krpc.Transcoder

/**Metadata headers for gRPC calls, used to provide authentication, logging and other information in the form
 * of key-value pairs.
 *
 * The easiest way to create and consume custom metadata is to use [ClientMetadataInterceptor] and [ServerMetadataInterceptor].*/
expect class CallMetadata {
    /**Whether the given [key] is present in this metadata map.*/
    fun containsKey(key: CallMetadataKey<*>): Boolean

    /**Returns the value associated with [key], or `null` if none has been set.*/
    fun <T> get(key: CallMetadataKey<T>): T?
    /**Associates [value] with [key]. If there is already a value for the given key, this value is appended to the end.*/
    fun <T> put(key: CallMetadataKey<T>, value: T)

    /**Remove a [value] associated with [key]. Note that after this operation there might still be other values
     * associated with the given key.*/
    fun <T> remove(key: CallMetadataKey<T>, value: T): Boolean
    /**Discard all values associated with [key].*/
    fun <T> discardAll(key: CallMetadataKey<T>)
}

/**A key used to store and retrieve values in [CallMetadata] headers.*/
expect abstract class CallMetadataKey<T>

/**Returns a new instance of [CallMetadata] without any associated values.*/
expect fun callMetadata(): CallMetadata

/**Creates a new [CallMetadataKey] with the given [name], using [transcoder] to provide serialization support.
 * Use it to create new metadata keys from common code.*/
expect inline fun <reified T> metadataKey(name: String, transcoder: Transcoder<T>): CallMetadataKey<T>