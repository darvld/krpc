package com.github.darvld.krpc.compiler.model

import com.github.darvld.krpc.UnaryCall
import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.resolveAsClassName
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import io.grpc.MethodDescriptor.MethodType.UNARY

/**Contains information about a service method annotated with [UnaryCall].*/
class UnaryMethod(
    declaredName: String,
    methodName: String,
    requestName: String,
    override val returnType: ClassName,
    override val requestType: ClassName
) : ServiceMethodDefinition(
    declaredName,
    methodName,
    requestName,
    isSuspending = true,
    methodType = UNARY
) {
    companion object {
        val AnnotationName = UnaryCall::class.simpleName!!

        /**Extracts a [UnaryMethod] from a function [declaration] given the corresponding [UnaryCall] annotation.*/
        fun extractFrom(declaration: KSFunctionDeclaration, annotation: KSAnnotation): UnaryMethod {
            val methodName = declaration.extractMethodName(annotation)
            val returnType = declaration.returnType?.resolveAsClassName() ?: UnitClassName

            val (requestName, requestType) = declaration.extractRequestInfo { it.resolveAsClassName() }
                ?: "unit" to UnitClassName

            return UnaryMethod(
                declaredName = declaration.simpleName.asString(),
                methodName,
                requestName,
                returnType,
                requestType
            )
        }
    }
}