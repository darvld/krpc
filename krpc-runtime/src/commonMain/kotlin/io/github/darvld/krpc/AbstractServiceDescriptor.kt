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

/**Abstract base class for service descriptors.
 *
 * Descriptors are helper classes holding a service's method descriptors, transcoders, and other necessary
 * information.
 *
 * This class is not intended for general use, since the kRPC compiler will generate a custom descriptor for
 * every defined service. However, you may need descriptor instances when creating custom server/client implementations
 * in order to provide all the necessary information to the lower-level API.
 *
 * @see AbstractServiceProvider
 * @see AbstractServiceClient*/
expect abstract class AbstractServiceDescriptor() {

    /**The name of the described service.*/
    abstract val serviceName: String

    /**Creates a new [MethodDescriptor] with the given [name] and [type], using the provided
     * transcoders to serialize/deserialize requests and responses.
     *
     * The implementation of this method is platform-specific, see [MethodDescriptor] for details.*/
    protected fun <T, R> methodDescriptor(
        name: String,
        type: MethodType,
        requestTranscoder: Transcoder<T>,
        responseTranscoder: Transcoder<R>
    ): MethodDescriptor<T, R>

    object UnitTranscoder : Transcoder<Unit>
}