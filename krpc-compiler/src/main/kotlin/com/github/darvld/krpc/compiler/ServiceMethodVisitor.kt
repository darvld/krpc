package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.BidiStream
import com.github.darvld.krpc.ClientStream
import com.github.darvld.krpc.ServerStream
import com.github.darvld.krpc.UnaryCall
import com.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import io.grpc.MethodDescriptor

class ServiceMethodVisitor : KSEmptyVisitor<Unit, ServiceMethodDefinition>() {

    private fun reportError(inFunction: KSFunctionDeclaration, message: String): Nothing {
        throw IllegalStateException("Error while processing service method ${inFunction.qualifiedName?.asString()}: $message")
    }

    private fun KSFunctionDeclaration.requireSuspending(required: Boolean) {
        val isError = if (required) Modifier.SUSPEND !in modifiers else Modifier.SUSPEND in modifiers

        if (isError)
            reportError(this, "Unary and ClientStream rpc methods must be marked with the suspend modifier.")
    }

    override fun defaultHandler(node: KSNode, data: Unit): ServiceMethodDefinition {
        throw IllegalStateException("MethodVisitor should only be used to visit function declarations")
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit): ServiceMethodDefinition {
        if (function.parameters.size != 1) reportError(function, "Service methods must have exactly one parameter")
        if (function.returnType == null) reportError(function, "Service methods must declare a return type.")

        var type: MethodDescriptor.MethodType = MethodDescriptor.MethodType.UNKNOWN
        var methodName = function.simpleName.asString()

        for (annotation in function.annotations) {
            type = when (annotation.shortName.getShortName()) {
                UnaryCall::class.simpleName -> {
                    function.requireSuspending(true)
                    MethodDescriptor.MethodType.UNARY
                }
                ServerStream::class.simpleName -> {
                    function.requireSuspending(false)
                    MethodDescriptor.MethodType.SERVER_STREAMING
                }
                ClientStream::class.simpleName -> {
                    function.requireSuspending(true)
                    MethodDescriptor.MethodType.CLIENT_STREAMING
                }
                BidiStream::class.simpleName -> {
                    function.requireSuspending(false)
                    MethodDescriptor.MethodType.BIDI_STREAMING
                }
                else -> continue
            }

            annotation.arguments.first().value?.toString()?.takeUnless { it.isBlank() }?.let {
                methodName = it
            }
            break
        }
        if (type == MethodDescriptor.MethodType.UNKNOWN)
            reportError(function, "Method declarations inside @Service interfaces must provide a call type annotation.")

        return ServiceMethodDefinition(
            declaredName = function.simpleName.getShortName(),
            methodName = methodName,
            returnType = function.returnType,
            request = function.parameters.singleOrNull()
                ?: reportError(function, "Service methods must have a single parameter."),
            methodType = type
        )
    }
}