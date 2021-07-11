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