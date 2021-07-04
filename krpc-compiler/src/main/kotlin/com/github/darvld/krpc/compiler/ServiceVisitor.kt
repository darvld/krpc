package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.Service
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSDefaultVisitor

class ServiceVisitor : KSDefaultVisitor<Unit, ServiceDefinition>() {
    private val methodVisitor = ServiceMethodVisitor()

    private fun reportError(inClass: KSClassDeclaration, message: String) {
        throw IllegalStateException("Error while processing service definition ${inClass.qualifiedName?.asString()}: $message")
    }

    override fun defaultHandler(node: KSNode, data: Unit): ServiceDefinition {
        throw IllegalStateException("Service visitor can only visit service definition interfaces")
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit): ServiceDefinition {
        if (classDeclaration.classKind != INTERFACE && Modifier.ABSTRACT !in classDeclaration.modifiers)
            reportError(classDeclaration, "Service definitions must be interfaces or abstract classes")

        // The annotation arguments contain the names for the service, the provider and the client (if specified)
        val annotationArgs = classDeclaration.annotations.find {
            it.shortName.getShortName() == Service::class.simpleName
        }!!.arguments

        val (service, provider, client) = annotationArgs.map { name ->
            name.value?.toString()?.takeUnless { it.isBlank() }
        }

        // Provide defaults for the names
        val serviceName = service ?: classDeclaration.simpleName.getShortName()
        val providerName = provider ?: "${serviceName}Provider"
        val clientName = client ?: serviceName.replace(Regex("(.+)Service\\z")) {
            "${it.destructured.component1()}Client"
        }

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