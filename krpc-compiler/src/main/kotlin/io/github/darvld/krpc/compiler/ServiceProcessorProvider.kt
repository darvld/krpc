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

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import io.github.darvld.krpc.compiler.generators.ClientGenerator
import io.github.darvld.krpc.compiler.generators.DescriptorGenerator
import io.github.darvld.krpc.compiler.generators.ServiceProviderGenerator

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