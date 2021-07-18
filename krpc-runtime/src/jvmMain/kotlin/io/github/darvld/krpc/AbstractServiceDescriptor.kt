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

import java.io.InputStream

/**Base class for generated components holding a service's method descriptors and marshallers.
 *
 * This class should not be implemented manually, use the generated descriptor for your service instead.*/
actual abstract class AbstractServiceDescriptor {

    actual abstract val serviceName: String

    /**A no-op [Transcoder] to avoid runtime serialization of the [Unit] type.*/
    actual object UnitTranscoder : Transcoder<Unit> {
        override fun encode(value: Unit): InputStream = InputStream.nullInputStream()
        override fun decode(stream: InputStream) = Unit
    }

    protected actual fun <T, R> methodDescriptor(
        name: String,
        type: MethodType,
        requestTranscoder: Transcoder<T>,
        responseTranscoder: Transcoder<R>
    ): MethodDescriptor<T, R> {
        return MethodDescriptor.newBuilder<T, R>()
            .setFullMethodName("$serviceName/$name")
            .setType(type)
            .setRequestMarshaller(MarshallingTranscoder(requestTranscoder))
            .setResponseMarshaller(MarshallingTranscoder(responseTranscoder))
            .build()
    }
}