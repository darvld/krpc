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

package com.example.backend

import com.example.GpsService
import io.github.darvld.krpc.metadata.CallMetadata
import io.github.darvld.krpc.metadata.ClientMetadataInterceptor

/**A metadata interceptor used to provide the client's [authorization token][GpsService.AUTH_TOKEN].*/
class ClientAuthInterceptor(val authToken: String) : ClientMetadataInterceptor() {
    override fun intercept(metadata: CallMetadata): CallMetadata {
        metadata.put(GpsService.AUTH_TOKEN, authToken)

        return metadata
    }
}