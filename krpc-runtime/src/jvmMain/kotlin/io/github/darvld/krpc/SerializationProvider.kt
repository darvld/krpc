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
import kotlinx.serialization.KSerializer
import java.io.InputStream

/**Abstraction used by kRPC to create format-agnostic [MethodDescriptor.Marshaller] implementations.
 *
 * Every [SerializationProvider] is required to provide a factory method used to create a marshaller for
 * a serializable type. You could implement a provider like the following:
 * ```
 * object ProtoBufSerializationProvider : SerializationProvider {
 *    override fun <T> marshallerFor(serializer: KSerializer<T>): MethodDescriptor.Marshaller<T> {
 *      return object : MethodDescriptor.Marshaller<T> {
 *          override fun parse(stream: InputStream): T {
 *              return ProtoBuf.decodeFromByteArray(serializer, stream.readAllBytes())
 *          }
 *
 *          override fun stream(value: T): InputStream {
 *              return ByteArrayInputStream(ProtoBuf.encodeToByteArray(serializer, value))
 *          }
 *      }
 *    }
 * }
 * ```
 * */
actual interface SerializationProvider {
    /**Contract method used to generate a marshaller for a serializable type.*/
    fun <T> marshallerFor(serializer: KSerializer<T>): MethodDescriptor.Marshaller<T>

    /**A noop marshaller used for the [Unit] type.*/
    object UnitMarshaller : MethodDescriptor.Marshaller<Unit> {
        override fun parse(stream: InputStream?) = Unit

        override fun stream(value: Unit?): InputStream = InputStream.nullInputStream()
    }
}
