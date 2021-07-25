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

import io.grpc.MethodDescriptor
import kotlinx.serialization.*
import java.io.ByteArrayInputStream
import java.io.InputStream

/**A [Transcoder] wrapper used to provide a [MethodDescriptor.Marshaller] for service components.
 *
 * There is usually no reason to use this class manually, unless you are implementing your own
 * transcoding API.*/
@JvmInline
value class MarshallingTranscoder<T>(val transcoder: Transcoder<T>) : MethodDescriptor.Marshaller<T> {
    override fun parse(stream: InputStream): T = transcoder.decode(stream)
    override fun stream(value: T): InputStream = transcoder.encode(value)
}