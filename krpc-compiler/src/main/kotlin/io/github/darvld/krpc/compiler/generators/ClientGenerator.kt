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

package io.github.darvld.krpc.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.darvld.krpc.AbstractServiceClient
import io.github.darvld.krpc.MethodType
import io.github.darvld.krpc.compiler.*
import io.github.darvld.krpc.compiler.DESCRIPTOR_PROPERTY
import io.github.darvld.krpc.compiler.FLOW
import io.github.darvld.krpc.compiler.SERIALIZATION_PROVIDER
import io.github.darvld.krpc.compiler.SERIALIZATION_PROVIDER_PARAM
import io.github.darvld.krpc.compiler.dsl.*
import io.github.darvld.krpc.compiler.model.*
import io.github.darvld.krpc.compiler.model.ServiceMethodDefinition.Companion.returnType
import java.io.OutputStream

internal class ClientGenerator : ServiceComponentGenerator() {

    override fun getFilename(service: ServiceDefinition): String = service.clientName

    override fun generateComponent(output: OutputStream, service: ServiceDefinition) {
        buildFile(withPackage = service.packageName, fileName = service.clientName, output) {
            addClass {
                markAsGenerated()

                addKdoc(
                    """
                    Generated [%T] client implementation using a specific [SerializationProvider]
                    to marshall requests and responses.
                    """.trimIndent(),
                    service.className
                )

                addSuperinterface(service.className)

                superclass(AbstractServiceClient::class.asTypeName().parameterizedBy(service.clientClassName))
                addSuperclassConstructorParameter("$CHANNEL_PARAM, $CALL_OPTIONS_PARAM")

                // Primary constructor (private)
                constructor(primary = true) {
                    addModifiers(PRIVATE)

                    parameter(CHANNEL_PARAM, CHANNEL)
                    parameter(DESCRIPTOR_PROPERTY, service.descriptorClassName)
                    parameter(CALL_OPTIONS_PARAM, CALL_OPTIONS, DEFAULT_CALL_OPTIONS)
                }

                // Service descriptor val (to be merged into constructor)
                property(DESCRIPTOR_PROPERTY, service.descriptorClassName, PRIVATE) {
                    initializer(DESCRIPTOR_PROPERTY)
                }

                // Secondary constructor (public)
                constructor {
                    markAsGenerated()

                    parameter(CHANNEL_PARAM, CHANNEL)
                    parameter(SERIALIZATION_PROVIDER_PARAM, SERIALIZATION_PROVIDER)
                    parameter(CALL_OPTIONS_PARAM, CALL_OPTIONS, DEFAULT_CALL_OPTIONS)

                    callThisConstructor(
                        CHANNEL_PARAM,
                        "${service.declaredName}($SERIALIZATION_PROVIDER_PARAM)",
                        CALL_OPTIONS_PARAM
                    )
                }

                // Build method override
                // TODO: Abstract this into the Common module (currently this will work on JVM only)
                function("build", OVERRIDE) {
                    markAsGenerated()

                    addParameter(CHANNEL_PARAM, CHANNEL)
                    addParameter(CALL_OPTIONS_PARAM, CALL_OPTIONS)

                    returns(service.clientClassName)

                    addCode("return ${service.clientName}($CHANNEL_PARAM, $CALL_OPTIONS_PARAM, $DESCRIPTOR_PROPERTY)")
                }

                // Implement delegated service methods
                for (method in service.methods) {
                    addFunction(buildServiceMethodOverride(method, service.descriptorName))
                }
            }
        }
    }

    internal fun buildServiceMethodOverride(method: ServiceMethodDefinition, serviceDescriptorName: String): FunSpec {
        return FunSpec.builder(method.declaredName).apply {
            addModifiers(OVERRIDE)
            markAsGenerated()

            if (method.isSuspending) addModifiers(SUSPEND)

            val callArgument: String

            when (method.request) {
                is SimpleRequest -> {
                    val requestType =
                        if (method.methodType == MethodType.CLIENT_STREAMING || method.methodType == MethodType.BIDI_STREAMING)
                            FLOW.parameterizedBy(method.request.type)
                        else
                            method.request.type

                    addParameter(method.request.parameterName, requestType)
                    callArgument = method.request.parameterName
                }
                is CompositeRequest -> {
                    for ((name, type) in method.request.parameters) {
                        addParameter(name, type)
                    }
                    val wrapperReference = "$serviceDescriptorName.${method.request.wrapperName}"
                    callArgument = "$wrapperReference(${method.request.parameters.keys.joinToString()})"
                }
                NoRequest -> {
                    callArgument = "Unit"
                }
            }

            returns(method.returnType)

            val builderName: String = when (method.methodType) {
                MethodType.UNARY -> "unaryCall"
                MethodType.CLIENT_STREAMING -> "clientStreamCall"
                MethodType.SERVER_STREAMING -> "serverStreamCall"
                MethodType.BIDI_STREAMING -> "bidiStreamCall"
                else -> reportError(message = "Unknown method type (in method ${method.declaredName})")
            }

            val body = CodeBlock.builder().add(
                "%L($DESCRIPTOR_PROPERTY.%L, %L, $CALL_OPTIONS_PARAM)",
                // The appropriate method to call from ClientCalls
                builderName,
                // The descriptor `val` for this method
                method.declaredName,
                // Pass in the method's argument (request)
                callArgument
            ).build()

            if (method.responseType != UNIT) addCode("return %L", body) else addCode(body)
        }.build()
    }

    companion object {
        private const val CHANNEL_PARAM = "channel"
        private const val CALL_OPTIONS_PARAM = "callOptions"

        private val CHANNEL = ClassName("io.github.darvld.krpc", "Channel")
        private val CALL_OPTIONS = ClassName("io.github.darvld.krpc", "CallOptions")

        private val DEFAULT_CALL_OPTIONS = buildCode(
            "%M()",
            MemberName("io.github.darvld.krpc", "defaultCallOptions")
        )
    }
}
