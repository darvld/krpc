package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**Throws [IllegalStateException] with the given [message] and signalling [inFunction] as the source of the problem.*/
fun reportError(inFunction: KSFunctionDeclaration, message: String): Nothing {
    throw IllegalStateException("Error while processing service method ${inFunction.qualifiedName?.asString()}: $message")
}


/**Throws [IllegalStateException] with the given [message] and signalling [inMethod] as the source of the problem.*/
fun reportError(inMethod: ServiceMethodDefinition, message: String): Nothing {
    throw IllegalStateException("Error while processing service method ${inMethod.methodName}: $message")
}

/**Throws [IllegalStateException] with the given [message] and signalling [inClass] as the source of the problem.*/
fun reportError(inClass: KSClassDeclaration, message: String): Nothing {
    throw IllegalStateException("Error while processing service definition ${inClass.qualifiedName?.asString()}: $message")
}