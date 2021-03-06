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

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.darvld.krpc.compiler.model.CompositeRequest
import io.github.darvld.krpc.compiler.model.RequestInfo.Companion.requestTypeFor
import io.github.darvld.krpc.compiler.testing.assertContentEquals
import org.junit.Test

class DescriptorGenerationTest : CodeGenerationTest() {

    @Test
    fun `generates descriptor skeleton`() {
        val definition = serviceDefinition()
        val file = temporaryFolder.newFile()

        file.outputStream().use { stream ->
            DescriptorGenerator.generateComponent(stream, definition)
        }

        file.assertContentEquals(
            """
            package com.test.generated

            import io.github.darvld.krpc.AbstractServiceDescriptor
            import io.github.darvld.krpc.Generated
            import io.github.darvld.krpc.SerializationProvider
            import kotlin.String

            /**
             * Descriptor generated by the kRPC compiler for the [com.test.generated.TestService] interface.
             *      
             * This class provides method descriptors for other generated service components, you can
             * use it to build your own service components instead of using the ones generated by the
             * compiler.
             */
            @Generated("io.github.darvld.krpc")
            public class TestServiceDescriptor(
              serializationProvider: SerializationProvider,
            ) : AbstractServiceDescriptor() {
              public override val serviceName: String = "TestService"
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `generates wrapper for multiple-parameters request`() {
        val args = mapOf("request" to INT, "content" to STRING, "extra" to STRING)
        val method = unaryMethod(request = CompositeRequest(args, "UnaryRequest"))

        val generated = temporaryFolder.newObject("WrapperTest") {
            addType(DescriptorGenerator.buildRequestWrapper(method))
        }

        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.github.darvld.krpc.Generated
            import kotlin.Int
            import kotlin.String
            import kotlinx.serialization.Serializable
            
            public object WrapperTest {
              /**
               * Internal wrapper class generated by the kRPC compiler.
               *  
               * This wrapper is used internally to support multiple arguments in service methods,
               * it should not be used in general code.
               */
              @Generated("io.github.darvld.krpc")
              @Serializable
              internal data class UnaryRequest(
                public val request: Int,
                public val content: String,
                public val extra: String,
              )
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `generates marshaller for simple type`() {
        val generated = temporaryFolder.newObject("Transcoders") {
            addTranscoder(INT)
        }

        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.github.darvld.krpc.Generated
            import io.github.darvld.krpc.Transcoder
            import io.github.darvld.krpc.transcoder
            import kotlin.Int
            
            public object Transcoders {
              @Generated("io.github.darvld.krpc")
              private val intTranscoder: Transcoder<Int> = serializationProvider.transcoder()
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `generates marshaller for generic type`() {
        val generated = temporaryFolder.newObject("Transcoders") {
            addTranscoder(LIST.parameterizedBy(INT))
        }

        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.github.darvld.krpc.Generated
            import io.github.darvld.krpc.Transcoder
            import io.github.darvld.krpc.transcoder
            import kotlin.Int
            import kotlin.collections.List
            
            public object Transcoders {
              @Generated("io.github.darvld.krpc")
              private val intListTranscoder: Transcoder<List<Int>> = serializationProvider.transcoder()
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `generates marshaller for complex generic type`() {
        val generated = temporaryFolder.newObject("Transcoders") {
            addTranscoder(Map::class.asTypeName().parameterizedBy(Long::class.asTypeName(), LIST.parameterizedBy(INT)))
        }

        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.github.darvld.krpc.Generated
            import io.github.darvld.krpc.Transcoder
            import io.github.darvld.krpc.transcoder
            import kotlin.Int
            import kotlin.Long
            import kotlin.collections.List
            import kotlin.collections.Map
            
            public object Transcoders {
              @Generated("io.github.darvld.krpc")
              private val longIntListMapTranscoder: Transcoder<Map<Long, List<Int>>> =
                  serializationProvider.transcoder()
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `uses built-in marshaller for Unit`() {
        val generated = temporaryFolder.newObject("Transcoders") {
            addTranscoder(UNIT)
        }

        // Should not generate any marshallers
        generated.assertContentEquals(
            """
            package com.test.generated
            
            public object Transcoders
            
            """.trimIndent()
        )
    }

    @Test
    fun `re-uses existing marshaller for same type`() {
        val generated = temporaryFolder.newObject("Transcoders") {
            addTranscoder(INT)
            addTranscoder(INT)
        }

        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.github.darvld.krpc.Generated
            import io.github.darvld.krpc.Transcoder
            import io.github.darvld.krpc.transcoder
            import kotlin.Int
            
            public object Transcoders {
              @Generated("io.github.darvld.krpc")
              private val intTranscoder: Transcoder<Int> = serializationProvider.transcoder()
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `generates unary method descriptor`() {
        val service = serviceDefinition()
        val method = unaryMethod()

        val generated = temporaryFolder.newObject("Descriptor") {
            DescriptorGenerator.buildMethodDescriptor(method, service.requestTypeFor(method))
                .let(::addProperty)
        }

        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.github.darvld.krpc.Generated
            import io.github.darvld.krpc.MethodDescriptor
            import io.github.darvld.krpc.MethodType.UNARY
            import kotlin.Int
            import kotlin.String
            
            public object Descriptor {
              @Generated("io.github.darvld.krpc")
              internal val unary: MethodDescriptor<Int, String> = methodDescriptor(
                    name="unaryTest",
                    type=UNARY,
                    requestTranscoder=intTranscoder,
                    responseTranscoder=stringTranscoder
                  )
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `generates unary method descriptor with generic request and response`() {
        val method = unaryMethod(
            request = simpleRequest(type = LIST.parameterizedBy(INT)),
            returnType = LIST.parameterizedBy(STRING)
        )

        val service = serviceDefinition(methods = listOf(method))

        val generated = temporaryFolder.newObject("Descriptor") {
            DescriptorGenerator.buildMethodDescriptor(method, service.requestTypeFor(method))
                .let(::addProperty)
        }

        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.github.darvld.krpc.Generated
            import io.github.darvld.krpc.MethodDescriptor
            import io.github.darvld.krpc.MethodType.UNARY
            import kotlin.Int
            import kotlin.String
            import kotlin.collections.List
            
            public object Descriptor {
              @Generated("io.github.darvld.krpc")
              internal val unary: MethodDescriptor<List<Int>, List<String>> = methodDescriptor(
                    name="unaryTest",
                    type=UNARY,
                    requestTranscoder=intListTranscoder,
                    responseTranscoder=stringListTranscoder
                  )
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `generates bidi stream method descriptor`() {
        // NOTE: This case covers both client-stream and server-stream, since the only special requirement
        // for any streaming method is that the generator must extract the Flow type parameter.

        val service = serviceDefinition()
        val method = bidiStreamMethod()

        val generated = temporaryFolder.newObject("Descriptor") {
            DescriptorGenerator.buildMethodDescriptor(method, service.requestTypeFor(method))
                .let(::addProperty)
        }

        generated.assertContentEquals(
            """
            package com.test.generated

            import io.github.darvld.krpc.Generated
            import io.github.darvld.krpc.MethodDescriptor
            import io.github.darvld.krpc.MethodType.BIDI_STREAMING
            import kotlin.Int
            import kotlin.String

            public object Descriptor {
              @Generated("io.github.darvld.krpc")
              internal val bidiStream: MethodDescriptor<Int, String> = methodDescriptor(
                    name="bidiStreamTest",
                    type=BIDI_STREAMING,
                    requestTranscoder=intTranscoder,
                    responseTranscoder=stringTranscoder
                  )
            }
            
            """.trimIndent()
        )
    }
}
