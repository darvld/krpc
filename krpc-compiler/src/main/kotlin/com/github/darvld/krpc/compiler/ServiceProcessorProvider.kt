package com.github.darvld.krpc.compiler

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**Provider used as an entry point for [ServiceProcessor]. Required by KSP to instantiate the processor.*/
class ServiceProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ServiceProcessor(environment)
    }
}