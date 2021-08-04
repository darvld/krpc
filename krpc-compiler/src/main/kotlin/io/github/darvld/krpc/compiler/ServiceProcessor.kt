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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.validate
import io.github.darvld.krpc.Service
import io.github.darvld.krpc.compiler.generators.ServiceComponentGenerator

/**Main symbol processor used by the kRPC compiler.
 *
 * This processor selects all declarations marked with the [@Service][Service] annotation and generates the
 * corresponding descriptor, provider and client.
 *
 * @see ServiceVisitor*/
class ServiceProcessor(
    private val environment: SymbolProcessorEnvironment,
    private val serviceVisitor: ServiceVisitor = ServiceVisitor(),
    private val generators: List<ServiceComponentGenerator>,
) : SymbolProcessor {

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotated = resolver.getSymbolsWithAnnotation(Service::class.qualifiedName!!)
            .map { it as KSDeclaration }
        val unprocessed = annotated.filterNot { it.validate() }

        annotated.forEach { declaration ->
            try {
                // Extract the service definition using the visitor
                val service = declaration.accept(serviceVisitor, Unit)

                generators.forEach { component ->
                    // Report errors through the logger instead of unhandled exceptions

                    environment.codeGenerator.createNewFile(
                        dependencies = Dependencies(aggregating = true, declaration.containingFile!!),
                        packageName = service.packageName,
                        fileName = component.getFilename(service)
                    ).use { stream ->
                        component.generateComponent(stream, service)
                    }
                }
            } catch (e: ProcessingError) {
                environment.logger.error(e.message, e.symbol)
            }
        }

        return unprocessed.toList()
    }
}

