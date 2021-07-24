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

package io.github.darvld.krpc.compiler.generators

import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.UNIT
import io.github.darvld.krpc.compiler.model.CompositeRequest
import io.github.darvld.krpc.compiler.model.NoRequest
import io.github.darvld.krpc.compiler.testing.assertContentEquals
import org.junit.Test

class ProviderGenerationTest : CodeGenerationTest() {
    @Test
    fun `unary method`() {
        val definition = serviceDefinition(methods = listOf(unaryMethod()))
        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream ->
            ServiceProviderGenerator.generateComponent(stream, definition)
        }

        generated.assertContentEquals(
            singleMethodProvider("registerUnaryMethod(definition.unary, ::unary)")
        )
    }

    @Test
    fun `unary method with multiple arguments`() {
        val args = mapOf("id" to LONG, "name" to STRING, "age" to INT)
        val method = unaryMethod(request = CompositeRequest(args, "UnaryRequest"))

        val definition = serviceDefinition(methods = listOf(method))
        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream ->
            ServiceProviderGenerator.generateComponent(stream, definition)
        }

        generated.assertContentEquals(
            singleMethodProvider("registerUnaryMethod(definition.unary) { unary(it.id, it.name, it.age) }")
        )
    }

    @Test
    fun `unary method no request nor response`() {
        val method = unaryMethod(request = NoRequest, returnType = UNIT)
        val definition = serviceDefinition(methods = listOf(method))

        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream ->
            ServiceProviderGenerator.generateComponent(stream, definition)
        }

        generated.assertContentEquals(
            singleMethodProvider("registerUnaryMethod(definition.unary) { unary() }")
        )
    }

    @Test
    fun `bidi stream`() {
        val method = bidiStreamMethod()
        val definition = serviceDefinition(methods = listOf(method))

        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream ->
            ServiceProviderGenerator.generateComponent(stream, definition)
        }

        generated.assertContentEquals(
            singleMethodProvider("registerBidiStreamMethod(definition.bidiStream, ::bidiStream)")
        )
    }

    private fun singleMethodProvider(block: String): String {
        return """
        package com.test.generated
    
        import io.github.darvld.krpc.AbstractServiceProvider
        import io.github.darvld.krpc.Generated
        import io.github.darvld.krpc.SerializationProvider
        import io.github.darvld.krpc.ServiceRegistrar
        import kotlin.Unit
        import kotlin.coroutines.CoroutineContext
        import kotlin.coroutines.EmptyCoroutineContext
        
        /**
         * Generated [TestService] provider. Subclass this stub and override the service methods to provide
         * your implementation of the service.
         */
        @Generated("io.github.darvld.krpc")
        public abstract class TestServiceProvider(
          serializationProvider: SerializationProvider,
          context: CoroutineContext = EmptyCoroutineContext
        ) : AbstractServiceProvider(context), TestService {
          protected final override val definition: TestServiceDescriptor =
              TestServiceDescriptor(serializationProvider)
        
          @Generated("io.github.darvld.krpc")
          public final override fun ServiceRegistrar.bindMethods(): Unit {
            $block
          }
        }
    
        """.trimIndent()
    }
}