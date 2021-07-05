package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.SerializationProvider
import com.github.darvld.krpc.compiler.addClass
import com.github.darvld.krpc.compiler.buildFile
import com.github.darvld.krpc.compiler.markAsGenerated
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.squareup.kotlinpoet.*
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.ServerCalls
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**Code block body used to add a method to the service descriptor.*/
const val ADD_METHOD_CODE_BLOCK = """
addMethod(
            %T.%L(
               context = context,
               descriptor = definitions.%L,
               implementation = ::%L
            )
        )
"""

/**Code block body used for the server descriptor builder.*/
const val SERVICE_DEFINITION_BLOCK = """
return ServerServiceDefinition.builder(%S).run {
        %L
    
        build()    
    }
"""

/**Generates a service provider, an abstract class implementing both the [service] and [AbstractCoroutineServerImpl].
 * The class is written to a file using [output].
 *
 * @see generateClientImplementation
 * @see generateDescriptorContainer*/
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

/**Adds the [AbstractCoroutineServerImpl.bindService] implementation.*/
private fun TypeSpec.Builder.addServiceBinder(service: ServiceDefinition) {
    val serverCalls = ServerCalls::class

    val methodBuilders = service.methods.fold(CodeBlock.builder()) { block, it ->
        val definitionBuilderName = when (it.methodType) {
            MethodDescriptor.MethodType.UNARY -> "unaryServerMethodDefinition"
            MethodDescriptor.MethodType.CLIENT_STREAMING -> "clientStreamingServerMethodDefinition"
            MethodDescriptor.MethodType.SERVER_STREAMING -> "serverStreamingServerMethodDefinition"
            MethodDescriptor.MethodType.BIDI_STREAMING -> "bidiStreamingServerMethodDefinition"
            MethodDescriptor.MethodType.UNKNOWN -> throw IllegalStateException("Method type cannot be UNKNOWN")
        }

        block.add(
            ADD_METHOD_CODE_BLOCK.trimMargin(),
            serverCalls,
            definitionBuilderName,
            it.declaredName,
            it.declaredName
        )
    }.build()

    FunSpec.builder("bindService")
        .markAsGenerated()
        .addModifiers(KModifier.FINAL, KModifier.OVERRIDE)
        .addCode(SERVICE_DEFINITION_BLOCK.trimMargin(), service.serviceName, methodBuilders)
        .returns(ServerServiceDefinition::class)
        .build()
        .let(::addFunction)
}
