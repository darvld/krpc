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

import io.github.darvld.krpc.*
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.StringFormat
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/*
abstract class GpsServiceProvider(
    provider: SerializationProvider,
    context: CoroutineContext = EmptyCoroutineContext
) : AbstractServiceProvider(context), GpsService {

    @OptIn(ExperimentalSerializationApi::class)
    constructor(
        format: StringFormat,
        context: CoroutineContext = EmptyCoroutineContext
    ) : this(StringSerializationProvider(format), context)

    @OptIn(ExperimentalSerializationApi::class)
    constructor(
        format: BinaryFormat,
        context: CoroutineContext = EmptyCoroutineContext
    ) : this(BinarySerializationProvider(format), context)

    final override val descriptor: GpsServiceDescriptor = GpsServiceDescriptor(provider)

    final override fun ServiceRegistrar.bindMethods() {
        // handshake
        registerUnaryMethod(descriptor.handshake) { handshake() }

        // listVehicles
        registerUnaryMethod(descriptor.listVehicles) { listVehicles() }

        // addVehicle
        registerUnaryMethod(descriptor.addVehicle) { addVehicle(it.id, it.info, it.location) }

        // locationForVehicle

    }
}

 */