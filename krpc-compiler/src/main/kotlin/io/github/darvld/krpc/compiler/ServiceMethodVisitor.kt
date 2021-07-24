/*
 *    Copyright 2021 Dario Valdespino.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.darvld.krpc.compiler

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import com.squareup.kotlinpoet.UNIT
import io.github.darvld.krpc.*
import io.github.darvld.krpc.compiler.model.RequestInfo
import io.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import io.grpc.MethodDescriptor.MethodType.*

/**Function visitor used by [ServiceVisitor] to extract service method definitions from annotated members inside
 * a @Service interface. The data passed in when visiting a declaration is the name of the service.
 *
 * This class should only be used to visit [KSFunctionDeclaration] nodes. Visiting any other type of node will throw
 * [IllegalStateException].
 *
 * @see ServiceVisitor
 * @see ServiceProcessor*/
class ServiceMethodVisitor : KSEmptyVisitor<Unit, ServiceMethodDefinition>() {

    override fun defaultHandler(node: KSNode, data: Unit): ServiceMethodDefinition {
        reportError(node, "MethodVisitor should only be used to visit function declarations")
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit): ServiceMethodDefinition {
        for (annotation in function.annotations) {
            extractDefinition(function, annotation)?.let { return it }
        }

        reportError(function, "Service methods must provide a valid type annotation")
    }

    private fun KSFunctionDeclaration.requireSuspending(required: Boolean) {
        if (required && Modifier.SUSPEND !in modifiers) {
            reportError(this, "Unary and client-streaming methods must be marked with the 'suspend' modifier.")
        } else if (!required && Modifier.SUSPEND in modifiers) {
            reportError(this, "Server-streaming rpc methods must not be marked with 'suspend' modifier.")
        }
    }

    private fun KSFunctionDeclaration.extractMethodName(annotation: KSAnnotation): String {
        return annotation.arguments.first().value?.toString()?.takeUnless { it.isBlank() } ?: simpleName.asString()
    }

    private fun extractDefinition(
        declaration: KSFunctionDeclaration,
        annotation: KSAnnotation
    ): ServiceMethodDefinition? {
        val methodType: MethodType = when (annotation.shortName.asString()) {
            UnaryCall::class.simpleName -> UNARY
            ServerStream::class.simpleName -> SERVER_STREAMING
            ClientStream::class.simpleName -> CLIENT_STREAMING
            BidiStream::class.simpleName -> BIDI_STREAMING
            else -> return null
        }
        declaration.requireSuspending(methodType.serverSendsOneMessage())
        val requestType = RequestInfo.extractFrom(declaration, flowExpected = !methodType.clientSendsOneMessage())

        val responseType = if (methodType.serverSendsOneMessage()) {
            declaration.returnType?.resolveAsTypeName() ?: UNIT
        } else {
            declaration.returnType?.resolveAsParameterizedName()?.typeArguments?.singleOrNull()
                ?: reportError(
                    declaration,
                    message = "Server-streaming rpc methods must return a Flow of a serializable type."
                )
        }

        return ServiceMethodDefinition(
            declaredName = declaration.simpleName.asString(),
            methodName = declaration.extractMethodName(annotation),
            isSuspending = methodType.serverSendsOneMessage(),
            methodType = methodType,
            request = requestType,
            responseType = responseType
        )
    }
}