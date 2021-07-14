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

/**Base class for generated components holding a service's method descriptors and marshallers.
 *
 * This class should not be implemented manually, use the generated descriptor for your service instead.*/
abstract class AbstractServiceDescriptor {
    /**A no-op [MethodDescriptor.Marshaller] to avoid runtime serialization of the [Unit] type.*/
    object UnitMarshaller : MethodDescriptor.Marshaller<Unit> {
        override fun parse(stream: InputStream) = Unit
        override fun stream(value: Unit?): InputStream = InputStream.nullInputStream()
    }

    /**Shorthand extension to obtain a [MarshallingTranscoder] for a type given its serializer.*/
    @Suppress("nothing_to_inline")
    protected inline fun <T> SerializationProvider.marshallerFor(serializer: KSerializer<T>): MethodDescriptor.Marshaller<T> {
        return MarshallingTranscoder(transcoderFor(serializer))
    }
}