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

import io.github.darvld.krpc.metadata.CallMetadata
import io.grpc.Channel
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls
import kotlinx.coroutines.flow.Flow

actual abstract class AbstractServiceClient<T : AbstractServiceClient<T>> actual constructor(
    channel: Channel,
    options: CallOptions
) : AbstractCoroutineStub<T>(channel, options) {

    final override fun build(channel: Channel, callOptions: CallOptions): T = buildWith(channel, callOptions)

    actual abstract fun buildWith(channel: Channel, callOptions: CallOptions): T

    actual suspend fun <T, R> unaryCall(
        method: MethodDescriptor<T, R>,
        request: T,
        options: CallOptions,
        metadata: CallMetadata
    ): R {
        return ClientCalls.unaryRpc(channel, method, request, options, metadata)
    }

    actual fun <T, R> serverStreamCall(
        method: MethodDescriptor<T, R>,
        request: T,
        options: CallOptions,
        metadata: CallMetadata
    ): Flow<R> {
        return ClientCalls.serverStreamingRpc(channel, method, request, options, metadata)
    }

    actual suspend fun <T, R> clientStreamCall(
        method: MethodDescriptor<T, R>,
        requests: Flow<T>,
        options: CallOptions,
        metadata: CallMetadata
    ): R {
        return ClientCalls.clientStreamingRpc(channel, method, requests, options, metadata)
    }

    actual fun <T, R> bidiStreamCall(
        method: MethodDescriptor<T, R>,
        requests: Flow<T>,
        options: CallOptions,
        metadata: CallMetadata
    ): Flow<R> {
        return ClientCalls.bidiStreamingRpc(channel, method, requests, options, metadata)
    }
}