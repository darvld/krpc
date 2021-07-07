package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.SerializationProvider
import com.github.darvld.krpc.compiler.*
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor.MethodType.*
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls
import java.io.OutputStream

private const val CHANNEL_PARAM = "channel"
private const val CALL_OPTIONS_PARAM = "callOptions"
private const val DESCRIPTOR_PARAM = "descriptor"

/**Generates a client class implementing both the [service] and [AbstractCoroutineStub].
 *
 * The generated class is *final*.
 *
 * @see generateServiceProviderBase
 * @see generateServiceDescriptor*/
fun generateClientImplementation(output: OutputStream, service: ServiceDefinition) {
    buildFile(withPackage = service.packageName, fileName = service.clientName, output) {
        addClass {
            markAsGenerated()

            addSuperinterface(service.className)
            superclass(AbstractCoroutineStub::class.asTypeName().parameterizedBy(ClassName(packageName, name)))
                .addSuperclassConstructorParameter("$CHANNEL_PARAM, $CALL_OPTIONS_PARAM")

            // Primary constructor (private)
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

                addParameter(CHANNEL_PARAM, Channel::class)
                addParameter(CALL_OPTIONS_PARAM, CallOptions::class)
                addParameter(SERIALIZATION_PROVIDER_PARAM, SerializationProvider::class)

                returns(service.clientClassName)

                addCode("return ${service.clientName}($CHANNEL_PARAM, $CALL_OPTIONS_PARAM, ${service.descriptorName}($SERIALIZATION_PROVIDER_PARAM))")
            }

            // Implement surrogate service methods
            service.methods.forEach(::overrideServiceMethod)
        }
    }
}

private fun TypeSpec.Builder.overrideServiceMethod(method: ServiceMethodDefinition) {
    addFunction(method.declaredName, KModifier.OVERRIDE) {
        markAsGenerated()

        if (method.isSuspending) addModifiers(KModifier.SUSPEND)

        addParameter(method.requestName, method.requestType)
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
            ClientCalls::class, // Contains helper builders
            builderName, // The appropriate method to call from ClientCalls
            method.declaredName, // The descriptor `val` for this method
            method.requestName // Pass in the method's argument (request)
        ).build()

        if (method.returnType != UnitClassName) addCode("return %L", body) else addCode(body)
    }
}
