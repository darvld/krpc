package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.testing.assertContentEquals
import org.junit.Test

class ClientGenerationTest : CodeGenerationTest() {
    private val clientGenerator = ClientGenerator()
    
    @Test
    fun `generates client skeleton`() {
        val definition = serviceDefinition()
        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream -> clientGenerator.generateClientImplementation(stream, definition) }
        
        generated.assertContentEquals(
            """
            package com.test.generated

            import com.github.darvld.krpc.SerializationProvider
            import io.grpc.CallOptions
            import io.grpc.Channel
            import io.grpc.kotlin.AbstractCoroutineStub
            import javax.`annotation`.processing.Generated

            @Generated("com.github.darvld.krpc")
            public class TestClient private constructor(
              channel: Channel,
              callOptions: CallOptions = CallOptions.DEFAULT,
              private val descriptor: TestServiceDescriptor
            ) : AbstractCoroutineStub<TestClient>(channel, callOptions), TestService {
              @Generated("com.github.darvld.krpc")
              public constructor(
                channel: Channel,
                serializationProvider: SerializationProvider,
                callOptions: CallOptions = CallOptions.DEFAULT
              ) : this(channel, callOptions, TestServiceDescriptor(serializationProvider))

              @Generated("com.github.darvld.krpc")
              public override fun build(channel: Channel, callOptions: CallOptions): TestClient =
                  TestClient(channel, callOptions, descriptor)

              @Generated("com.github.darvld.krpc")
              public fun withSerializationProvider(
                channel: Channel,
                callOptions: CallOptions,
                serializationProvider: SerializationProvider
              ): TestClient = TestClient(channel, callOptions, TestServiceDescriptor(serializationProvider))
            }

            """.trimIndent()
        )
    }
    
    @Test
    fun `overrides unary method`() {
        val definition = serviceDefinition()
        val generated = temporaryFolder.newObject("Client") {
            addSuperinterface(definition.className)
            
            addFunction(clientGenerator.buildServiceMethodOverride(unaryMethod()))
        }
        
        generated.assertContentEquals(
            """
            package com.test.generated

            import io.grpc.kotlin.ClientCalls
            import javax.`annotation`.processing.Generated
            import kotlin.Int
            import kotlin.String
            
            public object Client : TestService {
              @Generated("com.github.darvld.krpc")
              public override suspend fun unary(request: Int): String = ClientCalls.unaryRpc(channel,
                  descriptor.unary, request, callOptions)
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
                unaryMethod(
                    requestName = "unit",
                    requestType = UnitClassName,
                    returnType = UnitClassName
                )
            ).let(::addFunction)
        }
        
        generated.assertContentEquals(
            """
            package com.test.generated

            import io.grpc.kotlin.ClientCalls
            import javax.`annotation`.processing.Generated
            import kotlin.Unit
            
            public object Client : TestService {
              @Generated("com.github.darvld.krpc")
              public override suspend fun unary(): Unit {
                ClientCalls.unaryRpc(channel, descriptor.unary, Unit, callOptions)
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
            
            addFunction(clientGenerator.buildServiceMethodOverride(bidiStreamMethod()))
        }
        
        generated.assertContentEquals(
            """
            package com.test.generated

            import io.grpc.kotlin.ClientCalls
            import javax.`annotation`.processing.Generated
            import kotlin.Int
            import kotlin.String
            import kotlinx.coroutines.flow.Flow
            
            public object Client : TestService {
              @Generated("com.github.darvld.krpc")
              public override fun bidiStream(request: Flow<Int>): Flow<String> =
                  ClientCalls.bidiStreamingRpc(channel, descriptor.bidiStream, request, callOptions)
            }

            """.trimIndent()
        )
    }
}