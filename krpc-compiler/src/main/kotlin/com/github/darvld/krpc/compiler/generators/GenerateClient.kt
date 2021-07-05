package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.SerializationProvider
import com.github.darvld.krpc.compiler.addClass
import com.github.darvld.krpc.compiler.addFunction
import com.github.darvld.krpc.compiler.asClassName
import com.github.darvld.krpc.compiler.buildFile
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls
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
                addModifiers(KModifier.OVERRIDE)

                addParameter("channel", Channel::class)
                addParameter("callOptions", CallOptions::class)

                returns(service.clientClassName)

                addCode("return ${service.clientName}(channel, callOptions, descriptor)")
            }

            addFunction("withSerializationProvider") {
                addParameter("channel", Channel::class)
                addParameter("callOptions", CallOptions::class)
                addParameter("provider", SerializationProvider::class)

                returns(service.clientClassName)

                addCode("return ${service.clientName}(channel, callOptions, ${service.descriptorName}(provider))")
            }

            for (method in service.methods) {
                addFunction(method.methodName, KModifier.OVERRIDE, KModifier.SUSPEND) {
                    addParameter(method.request.name!!.asString(), method.request.type.resolve().asClassName())
                    returns(method.returnType!!.resolve().asClassName())

                    val callType = when (method.methodType) {
                        MethodDescriptor.MethodType.UNARY -> "unaryRpc"
                        MethodDescriptor.MethodType.CLIENT_STREAMING -> "clientStreamingRpc"
                        MethodDescriptor.MethodType.SERVER_STREAMING -> "serverStreamingRpc"
                        MethodDescriptor.MethodType.BIDI_STREAMING -> "bidiStreamingRpc"
                        MethodDescriptor.MethodType.UNKNOWN -> throw IllegalStateException("Member type cannot be unknown")
                    }
                    addCode(
                        "return %T.%L(channel, descriptor.%L, %L, callOptions)",
                        ClientCalls::class, callType, method.methodName, method.request.name!!.asString()
                    )
                }
            }

            addSuperinterface(service.className)
        }
    }
}