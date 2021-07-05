package com.example

import com.github.darvld.krpc.ServerStream
import com.github.darvld.krpc.Service
import com.github.darvld.krpc.UnaryCall
import kotlinx.coroutines.flow.Flow

@Service
interface GpsService {
    @UnaryCall("customName")
    suspend fun doSomething(message: String): Int

    @UnaryCall
    suspend fun fireAndForget(message: String)

    @ServerStream
    fun getStream(message: String): Flow<String>
}