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

/**Function visitor used by [ServiceVisitor] to extract service method definitions from annotated members inside
 * a @Service interface. The data passed in when visiting a declaration is the name of the service.
 *
 * This class should only be used to visit [KSFunctionDeclaration] nodes. Visiting any other type of node will throw
 * [IllegalStateException].
 *
 * @see ServiceVisitor
 * @see ServiceProcessor*/
class ServiceMethodVisitor : KSEmptyVisitor<String, ServiceMethodDefinition>() {

    /**Throws [IllegalStateException] with the given [message] and signalling [inFunction] as the source of the problem.*/
    private fun reportError(inFunction: KSFunctionDeclaration, message: String): Nothing {
        throw IllegalStateException("Error while processing service method ${inFunction.qualifiedName?.asString()}: $message")
    }

    /**If [required] is set to true and this declaration is not marked with the `suspend` modifier (or viceversa), an error will be reported.*/
    private fun KSFunctionDeclaration.requireSuspending(required: Boolean) {
        val isError = if (required) Modifier.SUSPEND !in modifiers else Modifier.SUSPEND in modifiers

        if (isError)
            reportError(this, "Unary and ClientStream rpc methods must be marked with the suspend modifier.")
    }

    override fun defaultHandler(node: KSNode, data: String): ServiceMethodDefinition {
        throw IllegalStateException("MethodVisitor should only be used to visit function declarations")
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: String): ServiceMethodDefinition {
        // Signature check
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
                ClientStream::class.simpleName -> {
                    function.requireSuspending(true)
                    MethodDescriptor.MethodType.CLIENT_STREAMING
                }
                ServerStream::class.simpleName -> {
                    function.requireSuspending(false)
                    MethodDescriptor.MethodType.SERVER_STREAMING
                }
                BidiStream::class.simpleName -> {
                    function.requireSuspending(false)
                    MethodDescriptor.MethodType.BIDI_STREAMING
                }
                else -> continue
            }

            // Support custom naming through annotation arguments
            annotation.arguments.first().value?.toString()?.takeUnless { it.isBlank() }?.let {
                methodName = it
            }
            break
        }

        if (type == MethodDescriptor.MethodType.UNKNOWN)
            reportError(function, "Method declarations inside @Service interfaces must provide a call type annotation.")

        return ServiceMethodDefinition(
            declaredName = function.simpleName.getShortName(),
            methodName = "$data/$methodName",
            returnType = function.returnType,
            request = function.parameters.singleOrNull()
                ?: reportError(function, "Service methods must have a single parameter."),
            methodType = type
        )
    }
}