package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.Service
import com.github.darvld.krpc.compiler.generators.ServiceComponentGenerator
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
internal class ServiceProcessor(
    private val environment: SymbolProcessorEnvironment,
    private val serviceVisitor: ServiceVisitor = ServiceVisitor(),
    private val generators: List<ServiceComponentGenerator>,
) : SymbolProcessor {
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotated = resolver.getSymbolsWithAnnotation(Service::class.qualifiedName!!)
        val unprocessed = annotated.filterNot { it.validate() }
        
        annotated.forEach { declaration ->
            // Extract the service definition using the visitor
            val service = declaration.accept(serviceVisitor, Unit)
            
            for (generator in generators) {
                generator.generate(environment.codeGenerator, service)
            }
        }
        
        return unprocessed.toList()
    }
}

