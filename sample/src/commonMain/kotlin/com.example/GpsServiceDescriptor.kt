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
import com.example.model.Vehicle
import io.github.darvld.krpc.*

/*
class GpsServiceDescriptor(serialProvider: SerializationProvider) : AbstractServiceDescriptor() {
    override val serviceName: String = "GpsService"

    val vehicleListTranscoder: Transcoder<List<Vehicle>> = serialProvider.transcoder()

    val addVehicleRequestTranscoder: Transcoder<AddVehicleRequest> = serialProvider.transcoder()

    val booleanTranscoder: Transcoder<Boolean> = serialProvider.transcoder()

    val vehicleTranscoder: Transcoder<Vehicle> = serialProvider.transcoder()

    val handshake: MethodDescriptor<Unit, Unit> = methodDescriptor(
        "handshake",
        MethodType.UNARY,
        requestTranscoder = UnitTranscoder,
        responseTranscoder = UnitTranscoder
    )

    val listVehicles: MethodDescriptor<Unit, List<Vehicle>> = methodDescriptor(
        "listVehicles",
        MethodType.UNARY,
        requestTranscoder = UnitTranscoder,
        responseTranscoder = vehicleListTranscoder
    )

    val addVehicle: MethodDescriptor<AddVehicleRequest, Boolean> = methodDescriptor(
        "addVehicle",
        MethodType.UNARY,
        requestTranscoder = addVehicleRequestTranscoder,
        responseTranscoder = booleanTranscoder
    )

    val locationForVehicle: MethodDescriptor<Vehicle, Location> = methodDescriptor(
        "locationForVehicle",
        MethodType.UNARY
    )

    data class AddVehicleRequest(val id: Long, val info: String, val location: Location)
}
*/