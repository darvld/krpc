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

package com.github.darvld.krpc

import io.grpc.ManagedChannel
import io.grpc.Server
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

/**Shuts down this server and suspends until it is terminated.
 *
 * If [forceTermination] is true then [Server.shutdownNow] will be called instead of [Server.shutdown].
 *
 * @return `this` [Server], to allow use in builder chains.
 * @throws TimeoutCancellationException if [timeout] is specified and the server fails to terminate within the timeout.
 * @see Server.isTerminated*/
suspend fun Server.shutdownAndJoin(forceTermination: Boolean = false, timeout: Long? = null): Server {
    if (forceTermination) shutdownNow() else shutdown()

    if (timeout != null) {
        withTimeout(timeout) { while (!isTerminated) yield() }
    } else {
        while (!isTerminated) yield()
    }

    return this
}

/**Shuts down this channel and suspends until it is terminated.
 *
 * If [forceTermination] is true then [ManagedChannel.shutdownNow] will be called instead of [ManagedChannel.shutdown].
 *
 * @return `this` [ManagedChannel], to allow use in builder chains.
 * @throws TimeoutCancellationException if [timeout] is specified and the channel fails to terminate within the timeout.
 * @see ManagedChannel.isTerminated*/
suspend fun ManagedChannel.shutdownAndJoin(forceTermination: Boolean = false, timeout: Long? = null): ManagedChannel {
    if (forceTermination) shutdownNow() else shutdown()

    if (timeout != null) {
        withTimeout(timeout) { while (!isTerminated) yield() }
    } else {
        while (!isTerminated) yield()
    }

    return this
}