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

import kotlinx.coroutines.flow.Flow

/**Registers rpc method implementations within a service provider.
 *
 * This interface is implemented on each platform, providing a way to bind service implementations within
 * [AbstractServiceProvider] instances from common code.*/
interface ServiceRegistrar {
    /**Registers the [implementation] for the unary rpc referenced by [descriptor]. The descriptor's method type
     * must be set to [MethodType.UNARY].*/
    fun <T, R> registerUnaryMethod(descriptor: MethodDescriptor<T, R>, implementation: suspend (T) -> R)

    /**Registers the [implementation] for the server-streaming rpc referenced by [descriptor]. The descriptor's method type
     * must be set to [MethodType.SERVER_STREAMING].*/
    fun <T, R> registerServerStreamMethod(descriptor: MethodDescriptor<T, R>, implementation: (T) -> Flow<R>)

    /**Registers the [implementation] for the client-streaming rpc referenced by [descriptor]. The descriptor's method type
     * must be set to [MethodType.CLIENT_STREAMING].*/
    fun <T, R> registerClientStreamMethod(descriptor: MethodDescriptor<T, R>, implementation: suspend (Flow<T>) -> R)

    /**Registers the [implementation] for the bidi-streaming rpc referenced by [descriptor]. The descriptor's method type
     * must be set to [MethodType.BIDI_STREAMING].*/
    fun <T, R> registerBidiStreamMethod(descriptor: MethodDescriptor<T, R>, implementation: (Flow<T>) -> Flow<R>)
}