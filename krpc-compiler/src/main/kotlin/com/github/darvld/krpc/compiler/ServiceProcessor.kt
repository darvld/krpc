package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.Service
import com.github.darvld.krpc.compiler.generators.generateClientImplementation
import com.github.darvld.krpc.compiler.generators.generateServiceDescriptor
import com.github.darvld.krpc.compiler.generators.generateServiceProviderBase
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.validate

/**Main symbol processor used by the KRPC compiler.
 *
 * This processor selects all declarations marked with the [@Service][Service] annotation and generates the
 * corresponding descriptor, provider and client.
 *
 * @see ServiceVisitor*/
class ServiceProcessor(
    private val environment: SymbolProcessorEnvironment,
    private val serviceVisitor: ServiceVisitor = ServiceVisitor()
) : SymbolProcessor {

    // TODO: Support incremental processing adding the appropriate source dependencies
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotated = resolver.getSymbolsWithAnnotation(Service::class.qualifiedName!!)
        val unprocessed = annotated.filterNot { it.validate() }

        annotated.forEach { declaration ->
            // Extract the service definition using the visitor
            val service = declaration.accept(serviceVisitor, Unit)

            // Each class annotated as a Service gets a helper class with method descriptors and marshallers
            environment.codeGenerator.createNewFile(
                Dependencies(true),
                service.packageName,
                service.descriptorName
            ).use { stream ->
                generateServiceDescriptor(stream, service)
            }

            // Generate the abstract service provider base class
            environment.codeGenerator.createNewFile(
                Dependencies(true),
                service.packageName,
                service.providerName
            ).use { stream ->
                generateServiceProviderBase(stream, service)
            }

            // Generate the client
            environment.codeGenerator.createNewFile(
                Dependencies(false),
                service.packageName,
                service.clientName
            ).use { stream ->
                generateClientImplementation(stream, service)
            }

        }

        return unprocessed.toList()
    }
}

