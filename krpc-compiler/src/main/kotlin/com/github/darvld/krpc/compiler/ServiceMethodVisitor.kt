package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.BidiStream
import com.github.darvld.krpc.ClientStream
import com.github.darvld.krpc.ServerStream
import com.github.darvld.krpc.UnaryCall
import com.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
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

    /**If [required] is set to true and this declaration is not marked with the `suspend` modifier (or viceversa), an error will be reported.*/
    private fun KSFunctionDeclaration.requireSuspending(required: Boolean) {
        val isError = if (required) Modifier.SUSPEND !in modifiers else Modifier.SUSPEND in modifiers

        if (isError)
            reportError(this, "Unary and ClientStream rpc methods must be marked with the suspend modifier.")
    }

    /**Extracts the type parameter for this type reference if it is a flow, returns null otherwise.*/
    private fun KSTypeReference.extractFlowType(): ClassName? {
        return resolve().let {
            if (it.declaration.simpleName.asString() != "Flow") return null

            it.arguments.single().type!!.resolve().asClassName()
        }
    }

    private fun KSFunctionDeclaration.extractRequest(flow: Boolean): Pair<String, ClassName> {
        return if (flow) {
            parameters.singleOrNull()?.let {
                it.name!!.asString() to (it.type.extractFlowType() ?: return@let null)
            } ?: reportError(
                this,
                "ClientStream and BidiStream methods must have a single Flow parameter"
            )
        } else {
            parameters.singleOrNull()?.let {
                it.name!!.asString() to it.type.resolve().asClassName()
            } ?: "unit" to UnitClassName
        }
    }

    override fun defaultHandler(node: KSNode, data: String): ServiceMethodDefinition {
        throw IllegalStateException("MethodVisitor should only be used to visit function declarations")
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: String): ServiceMethodDefinition {
        // Signature check
        if (function.parameters.size != 1) reportError(function, "Service methods must have exactly one parameter")

        var type: MethodDescriptor.MethodType = MethodDescriptor.MethodType.UNKNOWN
        var methodName = function.simpleName.asString()
        var returnType: ClassName? = null
        var request: Pair<String, ClassName>? = null

        for (annotation in function.annotations) {
            when (annotation.shortName.getShortName()) {
                UnaryCall::class.simpleName -> {
                    function.requireSuspending(true)

                    returnType = function.returnType?.resolve()?.asClassName() ?: UnitClassName
                    request = function.extractRequest(false)
                    type = MethodDescriptor.MethodType.UNARY
                }
                ClientStream::class.simpleName -> {
                    function.requireSuspending(true)

                    returnType = function.returnType?.resolve()?.asClassName() ?: Unit::class.asClassName()
                    request = function.extractRequest(true)
                    type = MethodDescriptor.MethodType.CLIENT_STREAMING
                }
                ServerStream::class.simpleName -> {
                    function.requireSuspending(false)

                    returnType = function.returnType?.extractFlowType()
                        ?: reportError(function, "ServerStream and BidiStream rpc methods must return Flow.")

                    request = function.extractRequest(false)
                    type = MethodDescriptor.MethodType.SERVER_STREAMING
                }
                BidiStream::class.simpleName -> {
                    function.requireSuspending(false)

                    returnType = function.returnType?.extractFlowType()
                        ?: reportError(function, "ServerStream and BidiStream rpc methods must return Flow.")

                    request = function.extractRequest(true)
                    type = MethodDescriptor.MethodType.BIDI_STREAMING
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

        if (returnType == null) reportError(function, "Unsupported return type")
        if (request == null) reportError(function, "Unsupported argument type")

        return ServiceMethodDefinition(
            declaredName = function.simpleName.getShortName(),
            methodName = "$data/$methodName",
            returnType = returnType,
            request = request,
            methodType = type
        )
    }
}