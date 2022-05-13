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

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import io.github.darvld.krpc.Service
import io.github.darvld.krpc.compiler.model.ServiceDefinition

/**Class visitor used by [ServiceProcessor] to extract service definitions from annotated interfaces.
 *
 * This class should only be used to visit [KSClassDeclaration] nodes. Visiting any other node type will
 * result in [IllegalStateException] being thrown.
 *
 * @see ServiceMethodVisitor
 * @see [ServiceProcessor]*/
class ServiceVisitor(
    private val methodVisitor: ServiceMethodVisitor = ServiceMethodVisitor()
) : KSDefaultVisitor<Unit, ServiceDefinition>() {

    override fun defaultHandler(node: KSNode, data: Unit): ServiceDefinition {
        reportError(node, "Service visitor can only visit service definition interfaces")
    }

    private fun KSAnnotation.getArgument(name: String): String? {
        return arguments.find { it.name?.getShortName() == name }?.value as? String
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit): ServiceDefinition {
        if (classDeclaration.classKind != INTERFACE)
            reportError(classDeclaration, "Service definitions must be interfaces.")

        // The annotation arguments contain the names for the service, the provider and the client (if specified)
        val annotation = classDeclaration.annotations.find { it.shortName.getShortName() == Service::class.simpleName }
            ?: reportError(classDeclaration, "Service definitions must be annotated with @Service.")

        // Provide defaults for the names
        val serviceName = annotation.getArgument(Service::overrideName.name).takeUnless { it.isNullOrBlank() }
            ?: classDeclaration.simpleName.getShortName()

        val providerName = annotation.getArgument(Service::providerName.name).takeUnless { it.isNullOrBlank() }
            ?: "${serviceName}Provider"

        val clientName = annotation.getArgument(Service::clientName.name).takeUnless { it.isNullOrBlank() }
            ?: serviceName.replace(Regex("(.+)Service\\z")) { "${it.destructured.component1()}Client" }

        // Generate method definitions
        val methods = classDeclaration.getDeclaredFunctions().filter { it.validate() }.map {
            it.accept(methodVisitor, Unit)
        }

        return ServiceDefinition(
            declaredName = classDeclaration.simpleName.getShortName(),
            packageName = classDeclaration.packageName.asString(),
            serviceName = serviceName,
            clientName = clientName,
            providerName = providerName,
            methods = methods.toList()
        )
    }
}
