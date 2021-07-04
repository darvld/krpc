package com.example

import com.github.darvld.krpc.SerializationProvider

class GpsServer(serializationProvider: SerializationProvider) : GpsServiceProvider(serializationProvider) {
    override suspend fun doSomething(message: String): Int {
        println("[Server $this] Client says: $message")
        return message.hashCode()
    }
}