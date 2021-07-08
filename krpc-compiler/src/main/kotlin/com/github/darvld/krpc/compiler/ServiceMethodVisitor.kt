package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.compiler.model.*
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSEmptyVisitor

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
        throw IllegalStateException("MethodVisitor should only be used to visit function declarations")
    }
    
    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit): ServiceMethodDefinition {
        for (annotation in function.annotations) {
            val definition = when (annotation.shortName.getShortName()) {
                UnaryMethod.AnnotationName -> UnaryMethod.extractFrom(function, annotation)
                ClientStreamMethod.AnnotationName -> ClientStreamMethod.extractFrom(function, annotation)
                ServerStreamMethod.AnnotationName -> ServerStreamMethod.extractFrom(function, annotation)
                BidiStreamMethod.AnnotationName -> BidiStreamMethod.extractFrom(function, annotation)
                else -> null
            }
            if (definition != null) return definition
        }
        reportError(function, "Service methods must provide the corresponding type annotation")
    }
}