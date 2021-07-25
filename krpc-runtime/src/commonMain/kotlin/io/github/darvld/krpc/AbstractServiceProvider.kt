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

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**Abstract base class for service providers.
 *
 * Custom implementations may use this for fine-grained control over gRPC method bindings on the server.
 *
 * @see AbstractServiceClient
 * @see AbstractServiceDescriptor
 * @see ServiceRegistrar*/
expect abstract class AbstractServiceProvider(
    context: CoroutineContext = EmptyCoroutineContext,
) {
    /**A descriptor for the provided service.*/
    protected abstract val descriptor: AbstractServiceDescriptor

    /**Bind method implementations using a [ServiceRegistrar].
     *
     * Providers should use this to register their implementations of the service methods. Failing to bind all
     * methods may cause undefined behavior.*/
    abstract fun ServiceRegistrar.bindMethods()
}