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

import io.grpc.ServerServiceDefinition
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.ServerCalls
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext

actual abstract class AbstractServiceProvider actual constructor(
    context: CoroutineContext,
) : AbstractCoroutineServerImpl(context) {

    protected actual abstract val definition: AbstractServiceDescriptor

    actual abstract fun ServiceRegistrar.bindMethods()

    final override fun bindService(): ServerServiceDefinition {
        return ServerServiceDefinition.builder(definition.serviceName).apply {
            asServiceDefinition(context).bindMethods()
        }.build()
    }
}

private fun ServerServiceDefinition.Builder.asServiceDefinition(context: CoroutineContext): ServiceRegistrar {
    return object : ServiceRegistrar {
        override fun <T, R> registerUnaryMethod(
            descriptor: MethodDescriptor<T, R>,
            implementation: suspend (T) -> R
        ) {
            addMethod(ServerCalls.unaryServerMethodDefinition(context, descriptor, implementation))
        }

        override fun <T, R> registerServerStreamMethod(
            descriptor: MethodDescriptor<T, R>,
            implementation: (T) -> Flow<R>
        ) {
            addMethod(ServerCalls.serverStreamingServerMethodDefinition(context, descriptor, implementation))
        }

        override fun <T, R> registerClientStreamMethod(
            descriptor: MethodDescriptor<T, R>,
            implementation: suspend (Flow<T>) -> R
        ) {
            addMethod(ServerCalls.clientStreamingServerMethodDefinition(context, descriptor, implementation))
        }

        override fun <T, R> registerBidiStreamMethod(
            descriptor: MethodDescriptor<T, R>,
            implementation: (Flow<T>) -> Flow<R>
        ) {
            addMethod(ServerCalls.bidiStreamingServerMethodDefinition(context, descriptor, implementation))
        }
    }
}