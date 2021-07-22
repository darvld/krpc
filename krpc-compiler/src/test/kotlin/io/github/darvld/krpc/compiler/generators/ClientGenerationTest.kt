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
import io.github.darvld.krpc.compiler.model.NoRequest
import io.github.darvld.krpc.compiler.testing.assertContentEquals
import org.junit.Test

class ClientGenerationTest : CodeGenerationTest() {
    private val clientGenerator = ClientGenerator()

    @Test
    fun `generates client skeleton`() {
        val definition = serviceDefinition()
        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream -> clientGenerator.generateComponent(stream, definition) }

        generated.assertContentEquals(
            """
            package com.test.generated

            import io.github.darvld.krpc.AbstractServiceClient
            import io.github.darvld.krpc.CallOptions
            import io.github.darvld.krpc.Channel
            import io.github.darvld.krpc.Generated
            import io.github.darvld.krpc.SerializationProvider
            import io.github.darvld.krpc.defaultCallOptions

            /**
             * Generated [TestService] client implementation using a specific [SerializationProvider]
             * to marshall requests and responses.
             */
            @Generated("io.github.darvld.krpc")
            public class TestClient private constructor(
              channel: Channel,
              callOptions: CallOptions = defaultCallOptions(),
              private val descriptor: TestServiceDescriptor
            ) : AbstractServiceClient<TestClient>(channel, callOptions), TestService {
              @Generated("io.github.darvld.krpc")
              public constructor(
                channel: Channel,
                serializationProvider: SerializationProvider,
                callOptions: CallOptions = defaultCallOptions()
              ) : this(channel, callOptions, TestServiceDescriptor(serializationProvider))

              @Generated("io.github.darvld.krpc")
              public override fun build(channel: Channel, callOptions: CallOptions): TestClient =
                  TestClient(channel, callOptions, descriptor)

              /**
               * Returns a new client using [serializationProvider] to marshall requests and responses.
               */
              @Generated("io.github.darvld.krpc")
              public fun withSerializationProvider(serializationProvider: SerializationProvider): TestClient =
                  TestClient(channel, callOptions, TestServiceDescriptor(serializationProvider))
            }

            """.trimIndent()
        )
    }

    @Test
    fun `overrides unary method`() {
        val definition = serviceDefinition()
        val generated = temporaryFolder.newObject("Client") {
            addSuperinterface(definition.className)

            addFunction(clientGenerator.buildServiceMethodOverride(unaryMethod(), definition.descriptorName))
        }

        generated.assertContentEquals(
            """
            package com.test.generated

            import io.github.darvld.krpc.Generated
            import kotlin.Int
            import kotlin.String
            
            public object Client : TestService {
              @Generated("io.github.darvld.krpc")
              public override suspend fun unary(request: Int): String = unaryCall(descriptor.unary, request,
                  callOptions)
            }

            """.trimIndent()
        )
    }

    @Test
    fun `overrides unary method with generic request and response`() {
        val method = unaryMethod(
            request = simpleRequest(type = LIST.parameterizedBy(INT)),
            returnType = LIST.parameterizedBy(STRING)
        )
        val definition = serviceDefinition(methods = listOf(method))

        val generated = temporaryFolder.newObject("Client") {
            addSuperinterface(definition.className)

            addFunction(clientGenerator.buildServiceMethodOverride(method, definition.descriptorName))
        }

        generated.assertContentEquals(
            """
            package com.test.generated

            import io.github.darvld.krpc.Generated
            import kotlin.Int
            import kotlin.String
            import kotlin.collections.List
            
            public object Client : TestService {
              @Generated("io.github.darvld.krpc")
              public override suspend fun unary(request: List<Int>): List<String> = unaryCall(descriptor.unary,
                  request, callOptions)
            }

            """.trimIndent()
        )
    }

    @Test
    fun `overrides unary method with multiple arguments`() {
        val args = mapOf("id" to LONG, "name" to STRING, "age" to INT)
        val method = unaryMethod(request = CompositeRequest(args, "UnaryRequest"))

        val definition = serviceDefinition(methods = listOf(method))
        val generated = temporaryFolder.newObject("Client") {
            addSuperinterface(definition.className)

            addFunction(clientGenerator.buildServiceMethodOverride(method, definition.descriptorName))
        }

        generated.assertContentEquals(
            """
            package com.test.generated

            import io.github.darvld.krpc.Generated
            import kotlin.Int
            import kotlin.Long
            import kotlin.String
            
            public object Client : TestService {
              @Generated("io.github.darvld.krpc")
              public override suspend fun unary(
                id: Long,
                name: String,
                age: Int
              ): String = unaryCall(descriptor.unary, TestServiceDescriptor.UnaryRequest(id, name, age),
                  callOptions)
            }

            """.trimIndent()
        )
    }

    @Test
    fun `overrides unary method (no request, no response)`() {
        val definition = serviceDefinition()
        val generated = temporaryFolder.newObject("Client") {
            addSuperinterface(definition.className)

            clientGenerator.buildServiceMethodOverride(
                unaryMethod(request = NoRequest, returnType = UNIT), definition.descriptorName
            ).let(::addFunction)
        }

        generated.assertContentEquals(
            """
            package com.test.generated

            import io.github.darvld.krpc.Generated
            import kotlin.Unit
            
            public object Client : TestService {
              @Generated("io.github.darvld.krpc")
              public override suspend fun unary(): Unit {
                unaryCall(descriptor.unary, Unit, callOptions)
              }
            }

            """.trimIndent()
        )
    }

    @Test
    fun `overrides bidi stream method`() {
        val definition = serviceDefinition()
        val generated = temporaryFolder.newObject("Client") {
            addSuperinterface(definition.className)

            addFunction(clientGenerator.buildServiceMethodOverride(bidiStreamMethod(), definition.descriptorName))
        }

        generated.assertContentEquals(
            """
            package com.test.generated

            import io.github.darvld.krpc.Generated
            import kotlin.Int
            import kotlin.String
            import kotlinx.coroutines.flow.Flow
            
            public object Client : TestService {
              @Generated("io.github.darvld.krpc")
              public override fun bidiStream(request: Flow<Int>): Flow<String> =
                  bidiStreamCall(descriptor.bidiStream, request, callOptions)
            }

            """.trimIndent()
        )
    }

    @Test
    fun `overrides bidi stream method with generic request and response`() {
        val method = bidiStreamMethod(
            request = simpleRequest(type = LIST.parameterizedBy(INT)),
            returnType = LIST.parameterizedBy(STRING)
        )
        val definition = serviceDefinition(methods = listOf(method))

        val generated = temporaryFolder.newObject("Client") {
            addSuperinterface(definition.className)

            addFunction(clientGenerator.buildServiceMethodOverride(method, definition.descriptorName))
        }

        generated.assertContentEquals(
            """
            package com.test.generated

            import io.github.darvld.krpc.Generated
            import kotlin.Int
            import kotlin.String
            import kotlin.collections.List
            import kotlinx.coroutines.flow.Flow
            
            public object Client : TestService {
              @Generated("io.github.darvld.krpc")
              public override fun bidiStream(request: Flow<List<Int>>): Flow<List<String>> =
                  bidiStreamCall(descriptor.bidiStream, request, callOptions)
            }

            """.trimIndent()
        )
    }
}