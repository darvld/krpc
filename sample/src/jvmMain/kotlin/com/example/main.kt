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

package com.example

import com.example.Simulation.longDelay
import com.example.Simulation.moderateDelay
import com.example.Simulation.randomLocation
import com.example.backend.GpsServer
import com.example.backend.ProtoBufSerializationProvider
import com.example.backend.ServerAuthInterceptor
import com.example.model.Location
import io.github.darvld.krpc.shutdownAndJoin
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking

private const val SERVER_ADDRESS = "localhost"
private const val SERVER_PORT = 8980

fun main(vararg args: String) = runBlocking {
    if ("-ci" in args) {
        println("---[Running sample in CI mode]---")
        println("In this mode, delays for the Simulation methods are disabled in order to speed up execution.")
        Simulation.ciRun = true
    }

    // Create a GpsServer using ProtoBuf to encode/decode requests and responses
    val gpsServer = GpsServer(ProtoBufSerializationProvider)

    // Setup the server and bind it
    val server = ServerBuilder.forPort(SERVER_PORT)
        .intercept(ServerAuthInterceptor)
        .addService(gpsServer)
        .build()

    server.start()

    // Client demo
    runClient()

    // Shutdown the server and wait for it to terminate
    println("[Server] Shutting down")
    server.shutdownAndJoin()
    println("[Server] Shutdown complete")
}

suspend fun runClient() {
    val channel = ManagedChannelBuilder.forAddress(SERVER_ADDRESS, SERVER_PORT)
        .usePlaintext() // Disable TLS for this example
        .build()

    val client = GpsClient(channel, ProtoBufSerializationProvider)
    // .withInterceptors(ClientAuthInterceptor("Bob"))

    // Handshake
    val handshakeResult = runCatching {
        client.handshake()
    }
    handshakeResult.onFailure {
        println("Handshake failed")
        channel.shutdownAndJoin()
        return
    }

    // Register a vehicle with the server (multiple arguments use case)
    client.addVehicle(4, "ManuallyAdded-MA1", randomLocation())

    // Get a list of vehicles to work with
    val vehicles = client.listVehicles()

    println("Vehicles tracked by the server:")
    vehicles.forEach { println("  - $it") }

    with(vehicles.random()) {
        // Retrieve location
        val lastLocation = client.locationForVehicle(this)
        println("$this is at $lastLocation")
    }

    // Receive route stream, collect 5 values and cancel
    with(vehicles.random()) {
        val route = client.trackVehicle(this)
        println("Tracking $this:")
        route.take(5).collect {
            println(" - $it")
        }
        println("Stopped tracking $this")
    }

    // Stream a route to the server (5 iterations, then stop)
    client.streamRoute(flow {
        var location = randomLocation()
        repeat(5) {
            emit(location)

            // Move it ~10m
            location = location.copy(latitude = location.latitude + 0.001)

            // Simulate update delay
            moderateDelay()
        }
    })

    // Continuous tracking mode
    val tracked = flow { // Track 3 different vehicles in total, for ~5 seconds each
        repeat(3) {
            emit(vehicles.random())
            longDelay()
        }
    }
    client.continuousTracking(tracked).collect {
        // For this example, this flag provides no useful information
        if (it == Location.TRACKING_CHANGED) return@collect

        println("Tracked vehicle is at $it")
    }

    println("Closing client channel")
    channel.shutdownAndJoin()
    println("Client terminated")
}
