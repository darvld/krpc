package com.example

import com.github.darvld.krpc.Service
import com.github.darvld.krpc.UnaryCall

@Service
interface GpsService {
    @UnaryCall("customName")
    suspend fun doSomething(message: String): Int
}