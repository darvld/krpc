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
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoroutineExtensionsTest {

    private inline fun withServer(crossinline block: suspend (Server) -> Unit): Unit = runBlocking {
        ServerBuilder.forPort(SERVER_PORT)
            .build()
            .start()
            .also { block(it) }
            .shutdownAndJoin()
    }

    private fun testChannel(): ManagedChannel {
        return ManagedChannelBuilder.forAddress(SERVER_ADDRESS, SERVER_PORT)
            .usePlaintext()
            .build()
    }

    @Test
    fun `managed channel shutdownAndJoin waits for channel termination`() = withServer {
        val channel = testChannel()
        assertFalse(channel.isShutdown)
        assertFalse(channel.isTerminated)

        channel.shutdownAndJoin()

        assertTrue(channel.isShutdown)
        assertTrue(channel.isTerminated)
    }

    @Test
    fun `server shutdownAndJoin waits for termination`() = runBlocking {
        val server = ServerBuilder.forPort(SERVER_PORT)
            .build()
            .start()

        assertFalse(server.isShutdown)
        assertFalse(server.isTerminated)

        server.shutdownAndJoin()

        assertTrue(server.isShutdown)
        assertTrue(server.isTerminated)
    }

    private companion object {
        const val SERVER_ADDRESS = "localhost"
        const val SERVER_PORT = 8080
    }
}