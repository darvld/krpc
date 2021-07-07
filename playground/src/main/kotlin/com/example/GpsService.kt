package com.example

import com.github.darvld.krpc.*
import kotlinx.coroutines.flow.Flow

@Service
interface GpsService {
    @UnaryCall("customName")
    suspend fun doSomething(message: String): Int

    @UnaryCall
    suspend fun fireAndForget()

    @ClientStream
    suspend fun sendStream(messages: Flow<String>)

    @ServerStream
    fun getStream(message: String): Flow<String>

    @BidiStream
    fun sendAndReceive(messages: Flow<String>): Flow<String>
}