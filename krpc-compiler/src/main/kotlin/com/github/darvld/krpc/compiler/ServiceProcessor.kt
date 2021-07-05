package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.Service
import com.github.darvld.krpc.compiler.generators.generateClientImplementation
import com.github.darvld.krpc.compiler.generators.generateDescriptorContainer
import com.github.darvld.krpc.compiler.generators.generateServiceProviderBase
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.validate

/**Main symbol processor used by the KRPC compiler.
 *
 * This processor selects all declarations marked with the [@Service][Service] annotation and generates the
 * corresponding descriptor, provider and client.
 *
 * @see ServiceVisitor*/
class ServiceProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    /**A visitor used to extract service declarations from annotated interfaces.*/
    private val serviceVisitor = ServiceVisitor()

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
                generateDescriptorContainer(stream, service)
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

/**Provider used as an entry point for [ServiceProcessor]. Required by KSP to instantiate the processor.*/
class ServiceProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ServiceProcessor(environment)
    }
}