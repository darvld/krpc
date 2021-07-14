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

import io.github.darvld.krpc.metadata.ServerMetadataInterceptor
import io.grpc.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> metadataKey(name: String): Metadata.Key<T> {
    val marshaller = object : Metadata.BinaryMarshaller<T> {
        override fun parseBytes(serialized: ByteArray): T {
            return ProtoBuf.decodeFromByteArray(serialized)
        }

        override fun toBytes(value: T): ByteArray {
            return ProtoBuf.encodeToByteArray(value)
        }
    }
    return Metadata.Key.of("$name-bin", marshaller)
}

object ServerAuthInterceptor : ServerMetadataInterceptor() {
    val AuthTokenMetadata = metadataKey<String>("auth_token")
    val SessionToken: Context.Key<String> = Context.key("Identity")

    override fun intercept(context: Context, metadata: Metadata): Context {
        val token = metadata.get(AuthTokenMetadata)
            ?: throw StatusRuntimeException(Status.UNAUTHENTICATED)

        return context.withValue(SessionToken, token)
    }
}