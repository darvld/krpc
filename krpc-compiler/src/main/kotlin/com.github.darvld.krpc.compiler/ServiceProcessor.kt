package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.Service
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.validate

class ServiceProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    val serviceVisitor = ServiceVisitor()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotated = resolver.getSymbolsWithAnnotation(Service::class.qualifiedName!!)
        val unprocessed = annotated.filterNot { it.validate() }

        annotated.forEach { declaration ->
            val service = declaration.accept(serviceVisitor, Unit)

            // Each class annotated as a Service gets an internal helper object with method definitions
            environment.codeGenerator.createNewFile(
                Dependencies(false/*, (declaration as KSClassDeclaration).containingFile!!*/),
                service.packageName,
                service.definitionsHelperName
            ).use { stream ->
                resolver.generateDefinitionsHelper(stream, service)
            }
        }

        return unprocessed.toList()
    }
}

class ServiceProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ServiceProcessor(environment)
    }
}