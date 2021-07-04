package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.BidiStream
import com.github.darvld.krpc.ClientStream
import com.github.darvld.krpc.ServerStream
import com.github.darvld.krpc.UnaryCall
import com.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import io.grpc.MethodDescriptor

class ServiceMethodVisitor : KSEmptyVisitor<Unit, ServiceMethodDefinition>() {
    override fun defaultHandler(node: KSNode, data: Unit): ServiceMethodDefinition {
        throw IllegalStateException("com.github.darvld.krpc.compiler.model.MethodVisitor should only be used for method generation")
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit): ServiceMethodDefinition {
        var type: MethodDescriptor.MethodType = MethodDescriptor.MethodType.UNKNOWN
        var methodName = function.simpleName.asString()

        for (annotation in function.annotations) {
            type = when (annotation.shortName.getShortName()) {
                UnaryCall::class.simpleName -> MethodDescriptor.MethodType.UNARY
                ServerStream::class.simpleName -> MethodDescriptor.MethodType.SERVER_STREAMING
                ClientStream::class.simpleName -> MethodDescriptor.MethodType.CLIENT_STREAMING
                BidiStream::class.simpleName -> MethodDescriptor.MethodType.BIDI_STREAMING
                else -> continue
            }
            annotation.arguments.first().value.toString().takeUnless { it.isBlank() }?.let {
                methodName = it
            }
            break
        }
        check(type != MethodDescriptor.MethodType.UNKNOWN) {
            "Method declarations inside @Service interfaces must provide a call type annotation." +
                    "(In method: ${function.qualifiedName?.asString()})"
        }

        return ServiceMethodDefinition(
            declaredName = methodName,
            methodName = function.simpleName.getShortName(),
            returnType = function.returnType,
            request = function.parameters.singleOrNull()
                ?: throw IllegalStateException("Service methods must declare a single parameter"),
            methodType = type
        )
    }
}