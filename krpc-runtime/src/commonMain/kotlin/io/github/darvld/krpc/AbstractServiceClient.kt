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
import io.github.darvld.krpc.metadata.callMetadata
import kotlinx.coroutines.flow.Flow

/**Abstract base class used by generated service clients.
 *
 * Manual client implementations may use this for low-level access to the underlying gRPC implementation.
 *
 * @see AbstractServiceProvider
 * @see AbstractServiceDescriptor*/
expect abstract class AbstractServiceClient(channel: Channel) {
    /**Performs a unary rpc, suspending until the response is received.
     *
     * @param method The descriptor for the unary method to be performed.
     * @param request The request to be sent by the client.
     * @param options Custom options for this call.
     * @param metadata Optional call metadata.*/
    suspend fun <T, R> unaryCall(
        method: MethodDescriptor<T, R>,
        request: T,
        options: CallOptions = defaultCallOptions(),
        metadata: CallMetadata = callMetadata()
    ): R

    /**Performs a server-streaming rpc, returning a [Flow] representing the server stream.
     *
     * @param method The descriptor for the unary method to be performed.
     * @param request The request to be sent by the client.
     * @param options Custom options for this call.
     * @param metadata Optional call metadata.*/
    fun <T, R> serverStreamCall(
        method: MethodDescriptor<T, R>,
        request: T,
        options: CallOptions = defaultCallOptions(),
        metadata: CallMetadata = callMetadata()
    ): Flow<R>

    /**Performs a client-streaming rpc, suspending until the response is received.
     *
     * @param method The descriptor for the unary method to be performed.
     * @param requests The stream of requests to be sent to the server.
     * @param options Custom options for this call.
     * @param metadata Optional call metadata.*/
    suspend fun <T, R> clientStreamCall(
        method: MethodDescriptor<T, R>,
        requests: Flow<T>,
        options: CallOptions = defaultCallOptions(),
        metadata: CallMetadata = callMetadata()
    ): R

    /**Performs a bidi-streaming rpc, returning a [Flow] with the responses streamed from the server.
     *
     * @param method The descriptor for the unary method to be performed.
     * @param requests The stream of requests to be sent by the client.
     * @param options Custom options for this call.
     * @param metadata Optional call metadata.*/
    fun <T, R> bidiStreamCall(
        method: MethodDescriptor<T, R>,
        requests: Flow<T>,
        options: CallOptions = defaultCallOptions(),
        metadata: CallMetadata = callMetadata()
    ): Flow<R>
}