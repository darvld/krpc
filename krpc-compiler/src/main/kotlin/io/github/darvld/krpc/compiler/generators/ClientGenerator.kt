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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.darvld.krpc.*
import io.github.darvld.krpc.compiler.*
import io.github.darvld.krpc.compiler.generators.DescriptorGenerator.Companion.SERIALIZATION_PROVIDER_PARAM
import io.github.darvld.krpc.compiler.model.*
import io.github.darvld.krpc.compiler.model.ServiceMethodDefinition.Companion.returnType
import java.io.OutputStream

internal class ClientGenerator : ServiceComponentGenerator() {

    override fun getFilename(service: ServiceDefinition): String {
        return service.clientName
    }

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
                    val requestType =
                        if (method.methodType == MethodType.CLIENT_STREAMING || method.methodType == MethodType.BIDI_STREAMING)
                            FLOW.parameterizedBy(method.request.type)
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
                MethodType.UNARY -> "unaryCall"
                MethodType.CLIENT_STREAMING -> "clientStreamCall"
                MethodType.SERVER_STREAMING -> "serverStreamCall"
                MethodType.BIDI_STREAMING -> "bidiStreamCall"
                else -> reportError(method, "Unknown method type")
            }

            val body = CodeBlock.builder().add(
                "%L($DESCRIPTOR_PARAM.%L, %L, $CALL_OPTIONS_PARAM)",
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
        private const val DESCRIPTOR_PARAM = "descriptor"
    }
}
