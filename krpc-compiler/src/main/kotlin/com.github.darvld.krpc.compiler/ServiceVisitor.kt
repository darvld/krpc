package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.Service
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSDefaultVisitor

class ServiceVisitor : KSDefaultVisitor<Unit, ServiceDefinition>() {
    private val methodVisitor = MethodVisitor()

    override fun defaultHandler(node: KSNode, data: Unit): ServiceDefinition {
        throw IllegalStateException("Service visitor can only visit service definition interfaces")
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit): ServiceDefinition {
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
        val clientName = client ?: serviceName

        // Generate method definitions
        val methods = classDeclaration.getDeclaredFunctions().filter { it.validate() }.map {
            it.accept(methodVisitor, Unit)
        }

        return ServiceDefinition(
            packageName = classDeclaration.packageName.asString(),
            serviceName = serviceName,
            clientName = clientName,
            providerName = providerName,
            methods = methods.toList()
        )
    }
}