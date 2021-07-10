package com.example

import kotlinx.coroutines.delay

/**Helper object used to simulate some execution behaviours.*/
object Simulation {
    /** When running CI tests, this can be set to false to speed up test execution.*/
    var ciRun = false

    /**Calls [delay] only if the current environment isn't part of a CI build.*/
    suspend fun adjustedDelay(reference: Long) {
        if (!ciRun) delay(reference)
    }

    /**Causes a short delay (~250 milliseconds), or none if the current run is part of a CI build.*/
    suspend inline fun shortDelay() = adjustedDelay(250)

    /**Causes a short delay (~1 second), or none if the current run is part of a CI build.*/
    suspend inline fun moderateDelay() = adjustedDelay(1_000)

    /**Causes a short delay (~5 seconds), or none if the current run is part of a CI build.*/
    suspend inline fun longDelay() = adjustedDelay(5_000)
}