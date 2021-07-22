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
import io.github.darvld.krpc.metadata.ServerMetadataInterceptor
import io.github.darvld.krpc.metadata.contextKey
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException

object ServerAuthInterceptor : ServerMetadataInterceptor() {
    val SessionToken = contextKey<String>("session_token")

    override fun intercept(context: Context, metadata: Metadata): Context {
        val token = metadata.get(GpsService.SESSION_TOKEN)
            ?: throw StatusRuntimeException(Status.UNAUTHENTICATED)

        return context.withValue(SessionToken, token)
    }
}

