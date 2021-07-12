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

import com.example.model.Location
import kotlinx.coroutines.delay
import kotlin.random.Random

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

    /**Causes a moderate delay (~1 second), or none if the current run is part of a CI build.*/
    suspend inline fun moderateDelay() = adjustedDelay(1_000)

    /**Causes a long delay (~5 seconds), or none if the current run is part of a CI build.*/
    suspend inline fun longDelay() = adjustedDelay(5_000)

    /**Returns a randomly constructed [Location].
     *
     * Note that the returned location could point anywhere, in the middle of the ocean for example. Don't know, don't care*/
    fun randomLocation(): Location {
        return Location(Random.nextDouble(-90.0, 90.0), Random.nextDouble(-180.0, 180.0))
    }
}