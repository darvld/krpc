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

expect class CallMetadata {
    fun containsKey(key: CallMetadataKey<*>): Boolean

    fun <T> get(key: CallMetadataKey<T>): T?
    fun <T> put(key: CallMetadataKey<T>, value: T)

    fun <T> remove(key: CallMetadataKey<T>, value: T): Boolean
    fun <T> discardAll(key: CallMetadataKey<T>)
}

expect abstract class CallMetadataKey<T>

expect inline fun <reified T> metadataKey(name: String, transcoder: Transcoder<T>): CallMetadataKey<T>