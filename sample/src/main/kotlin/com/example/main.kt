package com.example

import com.example.backend.GpsServer
import com.example.backend.ProtoBufSerializationProvider
import com.example.model.Location
import com.example.model.Vehicle
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

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
        .addService(gpsServer)
        .build()

    server.start()

    // Client demo
    showcaseClient()

    // Shutdown the server and wait for it to terminate
    println("[Server] Shutting down")
    server.shutdown()
    server.awaitTermination()
    println("[Server] Shutdown complete")
}

suspend fun showcaseClient() {
    val channel = ManagedChannelBuilder.forAddress(SERVER_ADDRESS, SERVER_PORT)
        .usePlaintext()
        .build()

    val client = GpsClient(channel, ProtoBufSerializationProvider)
    val vehicle = Vehicle(id = 8, info = "SampleVehicle")

    // Retrieve location
    val lastLocation = client.locationForVehicle(vehicle)
    println("$vehicle is at $lastLocation")

    // Receive route stream, collect 5 values and cancel
    val route = client.trackVehicle(vehicle)
    println("Tracking $vehicle:")
    route.take(5).collect {
        println(" - $it")
    }
    println("Stopped tracking $vehicle")

    // Stream a route to the server (5 iterations, then stop)
    client.streamRoute(flow {
        var location = lastLocation
        repeat(5) {
            emit(location)

            // Move it ~10m
            location = location.copy(latitude = location.latitude + 0.001)

            // Simulate update delay
            Simulation.moderateDelay()
        }
    })

    // Continuous tracking mode
    val tracked = flow { // Track 3 different vehicles in total, for 5 seconds each
        repeat(3) {
            emit(Vehicle(1L + it, "SampleVehicle-#${it + 1}"))
            Simulation.longDelay()
        }
    }
    client.continuousTracking(tracked).collect {
        // For this example, this flag provides no useful information
        if (it == Location.TRACKING_CHANGED) return@collect

        println("Tracked vehicle is at $it")
    }

    println("Closing client channel")
    channel.shutdown()
    while (!channel.isTerminated) yield()
    println("Client terminated")
}
