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

package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.testing.assertContentEquals
import org.junit.Test

class ProviderGenerationTest : CodeGenerationTest() {

    private val providerGenerator = ServiceProviderGenerator()

    @Test
    fun `unary method`() {
        val definition = serviceDefinition(methods = listOf(unaryMethod()))
        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream ->
            providerGenerator.generateServiceProviderBase(stream, definition)
        }

        generated.assertContentEquals(
            providerWithMethods(
                """.addMethod(
                  ServerCalls.unaryServerMethodDefinition(
                    context,
                    definitions.unary,
                    ::unary
                  )
                )"""
            )
        )
    }

    @Test
    fun `unary method no request nor response`() {
        val method = unaryMethod(requestName = "unit", requestType = UnitClassName, returnType = UnitClassName)
        val definition = serviceDefinition(methods = listOf(method))

        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream ->
            providerGenerator.generateServiceProviderBase(stream, definition)
        }

        generated.assertContentEquals(
            providerWithMethods(
                """.addMethod(
                  ServerCalls.unaryServerMethodDefinition(
                    context,
                    definitions.unary,
                    implementation = { unary() }
                  )
                )"""
            )
        )
    }

    @Test
    fun `bidi stream`() {
        val method = bidiStreamMethod()
        val definition = serviceDefinition(methods = listOf(method))

        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream ->
            providerGenerator.generateServiceProviderBase(stream, definition)
        }

        generated.assertContentEquals(
            providerWithMethods(
                """.addMethod(
                  ServerCalls.bidiStreamingServerMethodDefinition(
                    context,
                    definitions.bidiStream,
                    ::bidiStream
                  )
                )"""
            )
        )
    }

    private fun providerWithMethods(block: String): String {
        return """
        package com.test.generated
    
        import com.github.darvld.krpc.SerializationProvider
        import io.grpc.ServerServiceDefinition
        import io.grpc.kotlin.AbstractCoroutineServerImpl
        import io.grpc.kotlin.ServerCalls
        import javax.`annotation`.processing.Generated
        import kotlin.coroutines.CoroutineContext
        import kotlin.coroutines.EmptyCoroutineContext
        
        /**
         * Generated [TestService] provider. Subclass this stub and override the service methods to provide
         * your implementation of the service.
         */
        @Generated("com.github.darvld.krpc")
        public abstract class TestServiceProvider(
          serializationProvider: SerializationProvider,
          context: CoroutineContext = EmptyCoroutineContext
        ) : AbstractCoroutineServerImpl(context), TestService {
          private val definitions: TestServiceDescriptor = TestServiceDescriptor(serializationProvider)
        
          @Generated("com.github.darvld.krpc")
          public final override fun bindService(): ServerServiceDefinition = run {
            ServerServiceDefinition.builder("TestService")
                $block
                .build()
          }
        }
    
        """.trimIndent()
    }
}