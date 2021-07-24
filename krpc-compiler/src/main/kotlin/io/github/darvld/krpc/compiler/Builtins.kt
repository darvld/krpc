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

package io.github.darvld.krpc.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import io.github.darvld.krpc.SerializationProvider
import io.github.darvld.krpc.Transcoder
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext

internal const val SERIALIZATION_PROVIDER_PARAM = "serializationProvider"
internal const val COROUTINE_CONTEXT_PARAM = "context"

internal const val DESCRIPTOR_PROPERTY = "descriptor"
internal const val SERVICE_NAME_PROPERTY = "serviceName"

internal val FLOW = Flow::class.asClassName()
internal val COROUTINE_CONTEXT = CoroutineContext::class.asClassName()

internal val SERIALIZATION_PROVIDER = SerializationProvider::class.asClassName()
internal val TRANSCODER = Transcoder::class.asClassName()

internal val METHOD_DESCRIPTOR = ClassName("io.github.darvld.krpc", "MethodDescriptor")

internal val TRANSCODER_EXTENSION_MEMBER = MemberName("io.github.darvld.krpc", "transcoder", isExtension = true)
