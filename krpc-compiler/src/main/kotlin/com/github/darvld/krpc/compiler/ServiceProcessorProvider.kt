package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.compiler.generators.ClientGenerator
import com.github.darvld.krpc.compiler.generators.DescriptorGenerator
import com.github.darvld.krpc.compiler.generators.ServiceProviderGenerator
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**Provider used as an entry point for [ServiceProcessor]. Required by KSP to instantiate the processor.*/
class ServiceProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val generators = listOf(
            DescriptorGenerator(),
            ClientGenerator(),
            ServiceProviderGenerator()
        )
        return ServiceProcessor(environment, generators = generators)
    }
}