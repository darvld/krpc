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

package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.SerializationProvider
import com.github.darvld.krpc.compiler.*
import com.github.darvld.krpc.compiler.generators.DescriptorGenerator.Companion.SERIALIZATION_PROVIDER_PARAM
import com.github.darvld.krpc.compiler.generators.ServiceComponentGenerator.Companion.returnType
import com.github.darvld.krpc.compiler.model.*
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor.MethodType.*
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls
import java.io.OutputStream

internal class ClientGenerator : ServiceComponentGenerator {
    override fun generate(codeGenerator: CodeGenerator, definition: ServiceDefinition) {
        codeGenerator.createNewFile(
            Dependencies(true),
            definition.packageName,
            definition.clientName
        ).use { stream ->
            generateClientImplementation(stream, definition)
        }
    }

    internal fun generateClientImplementation(output: OutputStream, service: ServiceDefinition) {
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
                superclass(AbstractCoroutineStub::class.asTypeName().parameterizedBy(ClassName(packageName, name)))
                    .addSuperclassConstructorParameter("$CHANNEL_PARAM, $CALL_OPTIONS_PARAM")

                // Primary constructor (private)
                FunSpec.constructorBuilder()
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter(ParameterSpec(CHANNEL_PARAM, Channel::class.asTypeName()))
                    .addParameter(
                        ParameterSpec.builder(CALL_OPTIONS_PARAM, CallOptions::class.asTypeName())
                            .defaultValue("CallOptions.DEFAULT")
                            .build()
                    )
                    .addParameter(ParameterSpec(DESCRIPTOR_PARAM, service.descriptorClassName))
                    .build()
                    .let(::primaryConstructor)

                // Service descriptor val (to be merged into constructor)
                PropertySpec.builder(DESCRIPTOR_PARAM, service.descriptorClassName)
                    .addModifiers(KModifier.PRIVATE)
                    .mutable(false)
                    .initializer(DESCRIPTOR_PARAM)
                    .build()
                    .let(::addProperty)

                // Secondary constructor (public)
                FunSpec.constructorBuilder()
                    .markAsGenerated()
                    .addParameter(CHANNEL_PARAM, Channel::class)
                    .addParameter(SERIALIZATION_PROVIDER_PARAM, SerializationProvider::class)
                    .addParameter(
                        ParameterSpec.builder(CALL_OPTIONS_PARAM, CallOptions::class)
                            .defaultValue("CallOptions.DEFAULT")
                            .build()
                    )
                    .callThisConstructor(
                        CHANNEL_PARAM,
                        CALL_OPTIONS_PARAM,
                        "${service.descriptorName}($SERIALIZATION_PROVIDER_PARAM)"
                    )
                    .build()
                    .let(::addFunction)

                // Build method override
                addFunction("build") {
                    markAsGenerated()
                    addModifiers(KModifier.OVERRIDE)

                    addParameter(CHANNEL_PARAM, Channel::class)
                    addParameter(CALL_OPTIONS_PARAM, CallOptions::class)

                    returns(service.clientClassName)

                    addCode("return ${service.clientName}($CHANNEL_PARAM, $CALL_OPTIONS_PARAM, $DESCRIPTOR_PARAM)")
                }

                // withSerializationProvider builder
                addFunction("withSerializationProvider") {
                    markAsGenerated()

                    addKdoc("Returns a new client using [serializationProvider] to marshall requests and responses.")

                    addParameter(SERIALIZATION_PROVIDER_PARAM, SerializationProvider::class)

                    returns(service.clientClassName)

                    addCode("return ${service.clientName}($CHANNEL_PARAM, $CALL_OPTIONS_PARAM, ${service.descriptorName}($SERIALIZATION_PROVIDER_PARAM))")
                }

                // Implement surrogate service methods
                for (method in service.methods) {
                    addFunction(buildServiceMethodOverride(method, service.descriptorName))
                }
            }
        }
    }

    internal fun buildServiceMethodOverride(method: ServiceMethodDefinition, serviceDescriptorName: String): FunSpec {
        return FunSpec.builder(method.declaredName).apply {
            addModifiers(KModifier.OVERRIDE)
            markAsGenerated()

            if (method.isSuspending) addModifiers(KModifier.SUSPEND)

            val callArgument = when (method.request) {
                is SimpleRequest -> {
                    val requestType = if (method.methodType == CLIENT_STREAMING || method.methodType == BIDI_STREAMING)
                        FlowClassName.parameterizedBy(method.request.type)
                    else
                        method.request.type

                    addParameter(method.request.parameterName, requestType)
                    method.request.parameterName
                }
                is CompositeRequest -> {
                    for ((name, type) in method.request.parameters) {
                        addParameter(name, type)
                    }
                    val wrapperReference = "$serviceDescriptorName.${method.request.wrapperName}"
                    "$wrapperReference(${method.request.parameters.keys.joinToString()})"
                }
                NoRequest -> {
                    "Unit"
                }
            }

            returns(method.returnType)

            val builderName: String = when (method.methodType) {
                UNARY -> "unaryRpc"
                CLIENT_STREAMING -> "clientStreamingRpc"
                SERVER_STREAMING -> "serverStreamingRpc"
                BIDI_STREAMING -> "bidiStreamingRpc"
                UNKNOWN -> reportError(method, "Unknown method type")
            }

            val body = CodeBlock.builder().add(
                "%T.%L($CHANNEL_PARAM, $DESCRIPTOR_PARAM.%L, %L, $CALL_OPTIONS_PARAM)",
                // Contains helper builders
                ClientCalls::class,
                // The appropriate method to call from ClientCalls
                builderName,
                // The descriptor `val` for this method
                method.declaredName,
                // Pass in the method's argument (request)
                callArgument
            ).build()

            if (method.responseType != UnitClassName) addCode("return %L", body) else addCode(body)
        }.build()
    }

    companion object {
        private const val CHANNEL_PARAM = "channel"
        private const val CALL_OPTIONS_PARAM = "callOptions"
        private const val DESCRIPTOR_PARAM = "descriptor"
    }
}