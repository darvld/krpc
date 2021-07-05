package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.SerializationProvider
import com.github.darvld.krpc.compiler.*
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor.MethodType.*
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream

/**Generates a client class implementing both the [service] and [AbstractCoroutineStub].
 *
 * The generated class is *final*.
 *
 * @see generateServiceProviderBase
 * @see generateDescriptorContainer*/
fun generateClientImplementation(output: OutputStream, service: ServiceDefinition) {
    buildFile(withPackage = service.packageName, fileName = service.clientName, output) {
        addClass {
            markAsGenerated()

            superclass(AbstractCoroutineStub::class.asTypeName().parameterizedBy(ClassName(packageName, name)))
                .addSuperclassConstructorParameter("channel, callOptions")

            FunSpec.constructorBuilder()
                .addModifiers(KModifier.PRIVATE)
                .addParameter(ParameterSpec("channel", Channel::class.asTypeName()))
                .addParameter(
                    ParameterSpec.builder("callOptions", CallOptions::class.asTypeName())
                        .defaultValue("CallOptions.DEFAULT")
                        .build()
                )
                .addParameter(ParameterSpec("descriptor", service.descriptorClassName))
                .build()
                .let(::primaryConstructor)

            PropertySpec.builder("descriptor", service.descriptorClassName)
                .addModifiers(KModifier.PRIVATE)
                .mutable(false)
                .initializer("descriptor")
                .build()
                .let(::addProperty)

            FunSpec.constructorBuilder()
                .markAsGenerated()
                .addParameter("channel", Channel::class)
                .addParameter(SERIALIZATION_PROVIDER_PARAM, SerializationProvider::class)
                .addParameter(
                    ParameterSpec.builder("callOptions", CallOptions::class)
                        .defaultValue("CallOptions.DEFAULT")
                        .build()
                )
                .callThisConstructor(
                    "channel",
                    "callOptions",
                    "${service.descriptorName}($SERIALIZATION_PROVIDER_PARAM)"
                )
                .build()
                .let(::addFunction)

            addFunction("build") {
                markAsGenerated()
                addModifiers(KModifier.OVERRIDE)

                addParameter("channel", Channel::class)
                addParameter("callOptions", CallOptions::class)

                returns(service.clientClassName)

                addCode("return ${service.clientName}(channel, callOptions, descriptor)")
            }

            addFunction("withSerializationProvider") {
                markAsGenerated()

                addParameter("channel", Channel::class)
                addParameter("callOptions", CallOptions::class)
                addParameter("provider", SerializationProvider::class)

                returns(service.clientClassName)

                addCode("return ${service.clientName}(channel, callOptions, ${service.descriptorName}(provider))")
            }

            for (method in service.methods) {
                addFunction(method.declaredName, KModifier.OVERRIDE) {
                    markAsGenerated()

                    addParameter(method.request.name!!.asString(), method.request.type.resolve().asClassName())

                    if (method.methodType == SERVER_STREAMING || method.methodType == BIDI_STREAMING) {
                        returns(Flow::class.asClassName().parameterizedBy(method.returnType))
                    } else {
                        addModifiers(KModifier.SUSPEND)
                        returns(method.returnType)
                    }


                    val callType = when (method.methodType) {
                        UNARY -> "unaryRpc"
                        CLIENT_STREAMING -> "clientStreamingRpc"
                        SERVER_STREAMING -> "serverStreamingRpc"
                        BIDI_STREAMING -> "bidiStreamingRpc"
                        UNKNOWN -> reportError(method, "Member type cannot be unknown")
                    }

                    val body = CodeBlock.builder().add(
                        "%T.%L(channel, descriptor.%L, %L, callOptions)",
                        ClientCalls::class, // Contains helper builders
                        callType, // The appropriate method to call from ClientCalls
                        method.declaredName, // The descriptor `val` for this method
                        method.request.name!!.asString() // Pass in the method's argument (request)
                    ).build()

                    if (!method.returnsUnit)
                        addCode("return %L", body)
                    else
                        addCode(body)
                }
            }

            addSuperinterface(service.className)
        }
    }
}