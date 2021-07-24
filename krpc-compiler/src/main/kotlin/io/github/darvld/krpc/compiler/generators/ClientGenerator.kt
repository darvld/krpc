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
import io.github.darvld.krpc.compiler.dsl.*
import io.github.darvld.krpc.compiler.model.*
import io.grpc.MethodDescriptor.MethodType.*
import java.io.OutputStream

/**Generates a service client implementation.
 *
 * The generated component is *final*, and can be directly instantiated.
 * ```
 * val channel = ManagedChannelBuilder.forAddress(SERVER_ADDRESS, SERVER_PORT)
 *     .usePlaintext() // Disable TLS for this example
 *     .build()
 *
 * val client = GpsClient(channel, BinarySerializationProvider(ProtoBuf))
 * ```*/
object ClientGenerator : ServiceComponentGenerator {
    private const val CHANNEL_PARAM = "channel"
    private const val CALL_OPTIONS_PARAM = "callOptions"

    private val CHANNEL = ClassName("io.github.darvld.krpc", "Channel")
    private val CALL_OPTIONS = ClassName("io.github.darvld.krpc", "CallOptions")

    private val DEFAULT_CALL_OPTIONS = buildCode(
        "%M()",
        MemberName("io.github.darvld.krpc", "defaultCallOptions")
    )

    private fun methodBuilderForType(methodType: MethodType): String = when (methodType) {
        UNARY -> "unaryCall"
        CLIENT_STREAMING -> "clientStreamCall"
        SERVER_STREAMING -> "serverStreamCall"
        BIDI_STREAMING -> "bidiStreamCall"
        else -> reportError(null, "Unknown method type: ${methodType.name})")
    }

    private fun wrapRequest(methodType: MethodType, requestType: TypeName): TypeName {
        return if (methodType.clientSendsOneMessage()) requestType else FLOW.parameterizedBy(requestType)
    }

    override fun getFilename(service: ServiceDefinition): String = service.clientName

    override fun generateComponent(output: OutputStream, service: ServiceDefinition) {
        writeFile(service.packageName, service.clientName, output) {
            addClass {
                markAsGenerated()

                addKdoc(
                    """
                    Generated [${service.declaredName}] client implementation using a specific [SerializationProvider]
                    to marshall requests and responses.
                    """.trimIndent()
                )

                addSuperinterface(service.className)

                superclass(AbstractServiceClient::class.asTypeName().parameterizedBy(service.clientClassName))
                addSuperclassConstructorParameter("$CHANNEL_PARAM, $CALL_OPTIONS_PARAM")

                // Primary constructor (private)
                addConstructor(primary = true) {
                    addModifiers(PRIVATE)

                    addParameter(CHANNEL_PARAM, CHANNEL)
                    addParameter(DESCRIPTOR_PROPERTY, service.descriptorClassName)
                    addParameter(CALL_OPTIONS_PARAM, CALL_OPTIONS, DEFAULT_CALL_OPTIONS)
                }

                // Service descriptor val (to be merged into constructor)
                addProperty(DESCRIPTOR_PROPERTY, service.descriptorClassName, PRIVATE) {
                    initializer(DESCRIPTOR_PROPERTY)
                }

                // Secondary constructor (public)
                addConstructor {
                    markAsGenerated()

                    addParameter(CHANNEL_PARAM, CHANNEL)
                    addParameter(SERIALIZATION_PROVIDER_PARAM, SERIALIZATION_PROVIDER)
                    addParameter(CALL_OPTIONS_PARAM, CALL_OPTIONS, DEFAULT_CALL_OPTIONS)

                    callThisConstructor(
                        CHANNEL_PARAM,
                        "${service.declaredName}($SERIALIZATION_PROVIDER_PARAM)",
                        CALL_OPTIONS_PARAM
                    )
                }

                // Builder method override
                addFunction("buildWith", OVERRIDE) {
                    markAsGenerated()

                    addParameter(CHANNEL_PARAM, CHANNEL)
                    addParameter(CALL_OPTIONS_PARAM, CALL_OPTIONS)

                    returns(service.clientClassName)

                    addCode("return ${service.clientName}($CHANNEL_PARAM, $CALL_OPTIONS_PARAM, $DESCRIPTOR_PROPERTY)")
                }

                // Implement delegated service methods
                for (method in service.methods) {
                    if (method.request is CompositeRequest)
                        addImport(service.packageName, service.descriptorName, method.request.wrapperName)

                    addFunction(buildServiceMethodOverride(method))
                }
            }
        }
    }

    fun buildServiceMethodOverride(method: ServiceMethodDefinition): FunSpec = buildFunction(method.declaredName) {
        returns(method.returnType)
        markAsGenerated()

        addModifiers(OVERRIDE)
        if (method.isSuspending) addModifiers(SUSPEND)

        val callArgument: String
        when (method.request) {
            is SimpleRequest -> {
                val requestType = wrapRequest(method.methodType, method.request.type)

                addParameter(method.request.parameterName, requestType)
                callArgument = method.request.parameterName
            }
            is CompositeRequest -> {
                for ((name, type) in method.request.parameters) {
                    addParameter(name, type)
                }

                // call the wrapper's constructor: MethodWrapper(foo, bar)
                callArgument = "${method.request.wrapperName}(${method.request.parameters.keys.joinToString()})"
            }
            NoRequest -> callArgument = "Unit"
        }

        val body = buildCode(
            "%L($DESCRIPTOR_PROPERTY.%L, %L, $CALL_OPTIONS_PARAM)",
            // Template arguments: unaryCall¹(descriptor.foo², request³, callOptions)
            methodBuilderForType(method.methodType), method.declaredName, callArgument
        )

        if (method.responseType == UNIT) addCode(body) else addCode("return %L", body)
    }
}
