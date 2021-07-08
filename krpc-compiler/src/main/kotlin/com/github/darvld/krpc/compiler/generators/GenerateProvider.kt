package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.SerializationProvider
import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.addClass
import com.github.darvld.krpc.compiler.buildFile
import com.github.darvld.krpc.compiler.markAsGenerated
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.squareup.kotlinpoet.*
import io.grpc.MethodDescriptor.MethodType.*
import io.grpc.ServerServiceDefinition
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.ServerCalls
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**Generates a service provider, an abstract class implementing both the [service] and [AbstractCoroutineServerImpl].
 * The class is written to a file using [output].
 *
 * @see generateClientImplementation
 * @see generateServiceDescriptor*/
fun generateServiceProviderBase(output: OutputStream, service: ServiceDefinition) {
    buildFile(service.packageName, service.providerName, output) {
        addClass {
            markAsGenerated()

            addModifiers(KModifier.ABSTRACT)
            superclass(AbstractCoroutineServerImpl::class)

            // Primary constructor
            FunSpec.constructorBuilder()
                .addParameter(SERIALIZATION_PROVIDER_PARAM, SerializationProvider::class)
                .addParameter(
                    ParameterSpec.builder("context", CoroutineContext::class)
                        .defaultValue("%T", EmptyCoroutineContext::class)
                        .build()
                )
                .build()
                .let(::primaryConstructor)

            // Pass the coroutine context to the parent class
            addSuperclassConstructorParameter("context")

            // Implement the service interface
            addSuperinterface(ClassName(service.packageName, service.declaredName))

            // The method definitions are pulled from here
            val helperType = ClassName(service.packageName, service.descriptorName)
            PropertySpec.builder("definitions", helperType)
                .addModifiers(KModifier.PRIVATE)
                .mutable(false)
                .initializer("%T($SERIALIZATION_PROVIDER_PARAM)", helperType)
                .build()
                .let(::addProperty)

            // The `bindService` implementation
            addServiceBinder(service)
        }
    }
}

private fun TypeSpec.Builder.addServiceBinder(service: ServiceDefinition) {
    val serverCalls = ServerCalls::class

    val methodBuilders = service.methods.fold(CodeBlock.builder()) { block, method ->
        val definitionBuilderName = when (method.methodType) {
            UNARY -> "unaryServerMethodDefinition"
            CLIENT_STREAMING -> "clientStreamingServerMethodDefinition"
            SERVER_STREAMING -> "serverStreamingServerMethodDefinition"
            BIDI_STREAMING -> "bidiStreamingServerMethodDefinition"
            UNKNOWN -> throw IllegalStateException("Method type cannot be UNKNOWN")
        }

        val implementation = if (method.requestType == UnitClassName) {
            "implementation = { ${method.declaredName}() }"
        } else {
            "::${method.declaredName}"
        }

        block.addStatement(
            """
            addMethod(
                %T.%L(
                  context,
                  definitions.%L,
                  %L
                )
              )""".trimIndent(),
            serverCalls,
            definitionBuilderName,
            method.declaredName,
            implementation
        )
    }.build()

    FunSpec.builder("bindService")
        .markAsGenerated()
        .addModifiers(KModifier.FINAL, KModifier.OVERRIDE)
        .addCode(
            """
            return ServerServiceDefinition.builder(%S).run {
                  %L
                  build()
                }
            """.trimIndent(),
            service.serviceName, methodBuilders
        )
        .returns(ServerServiceDefinition::class)
        .build()
        .let(::addFunction)
}
