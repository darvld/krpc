package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.testing.assertContentEquals
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import org.junit.Test
import kotlin.test.assertEquals

class DescriptorGenerationTest : CodeGenerationTest() {
    private val descriptorGenerator = DescriptorGenerator()

    @Test
    fun `generates descriptor skeleton`() {
        val definition = serviceDefinition()
        val file = temporaryFolder.newFile()

        file.outputStream().use { stream ->
            descriptorGenerator.generateServiceDescriptor(stream, definition)
        }

        assertEquals(
            actual = file.readText(),
            expected = """
            package com.test.generated

            import com.github.darvld.krpc.SerializationProvider
            import javax.`annotation`.processing.Generated
            import kotlinx.serialization.serializer

            /**
             * Internal helper class generated by the kRPC compiler for the [TestService] interface.
             *      
             * This class provides method descriptors for other generated service components.
             * It should not be used in general code.
             *
             * @constructor Constructs a new [TestServiceDescriptor] using a [SerializationProvider]
             * to create the marshallers for method requests/responses.
             * @param serializationProvider A provider implementing a serialization format.
             * Used to generate marshallers for rpc methods.
             */
            @Generated("com.github.darvld.krpc")
            internal class TestServiceDescriptor(
              serializationProvider: SerializationProvider
            )
            
            """.trimIndent()
        )
    }

    @Test
    fun `generates marshaller for simple type`() {
        val generated = temporaryFolder.newObject("Marshallers") {
            addMarshaller(Int::class.asTypeName())
        }

        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.grpc.MethodDescriptor
            import javax.`annotation`.processing.Generated
            import kotlin.Int
            import kotlinx.serialization.serializer
            
            public object Marshallers {
              @Generated("com.github.darvld.krpc")
              private val intMarshaller: MethodDescriptor.Marshaller<Int> =
                  serializationProvider.marshallerFor(serializer())
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `uses built-in marshaller for Unit`() {
        val generated = temporaryFolder.newObject("Marshallers") {
            addMarshaller(UnitClassName)
        }

        // Should not generate any marshallers
        generated.assertContentEquals(
            """
            package com.test.generated
            
            public object Marshallers
            
            """.trimIndent()
        )
    }

    @Test
    fun `re-uses existing marshaller for same type`() {
        val generated = temporaryFolder.newObject("Marshallers") {
            Int::class.asTypeName().let {
                addMarshaller(it)
                addMarshaller(it)
            }
        }

        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.grpc.MethodDescriptor
            import javax.`annotation`.processing.Generated
            import kotlin.Int
            import kotlinx.serialization.serializer
            
            public object Marshallers {
              @Generated("com.github.darvld.krpc")
              private val intMarshaller: MethodDescriptor.Marshaller<Int> =
                  serializationProvider.marshallerFor(serializer())
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `generates unary method descriptor`() {
        val service = serviceDefinition()
        val method = unaryMethod()

        val generated = temporaryFolder.newObject("Descriptor") {
            // Placeholder properties so the marshallers are not generated (already covered by another test)
            addProperty(PropertySpec.builder("intMarshaller", Nothing::class).initializer("TODO()").build())
            addProperty(PropertySpec.builder("stringMarshaller", Nothing::class).initializer("TODO()").build())

            descriptorGenerator.buildMethodDescriptor(method, service).let(::addProperty)
        }

        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.grpc.MethodDescriptor
            import io.grpc.MethodDescriptor.MethodType.UNARY
            import java.lang.Void
            import javax.`annotation`.processing.Generated
            import kotlin.Int
            import kotlin.String
            
            public object Descriptor {
              public val intMarshaller: Void = TODO()

              public val stringMarshaller: Void = TODO()

              /**
               * Generated gRPC [MethodDescriptor] for the
               * [TestService.unary][com.test.generated.TestService.unary] method.
               *
               * This descriptor is used by generated service components and should not be used in general code.
               */
              @Generated("com.github.darvld.krpc")
              public val unary: MethodDescriptor<Int, String> = MethodDescriptor
                .newBuilder<Int, String>()
                .setFullMethodName("TestService/unaryTest")
                .setType(UNARY)
                .setRequestMarshaller(intMarshaller)
                .setResponseMarshaller(stringMarshaller)
                .build()

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
            // Placeholder properties so the marshallers are not generated (already covered by another test)
            addProperty(PropertySpec.builder("intMarshaller", Nothing::class).initializer("TODO()").build())
            addProperty(PropertySpec.builder("stringMarshaller", Nothing::class).initializer("TODO()").build())

            descriptorGenerator.buildMethodDescriptor(method, service).let(::addProperty)
        }

        generated.assertContentEquals(
            """
            package com.test.generated

            import io.grpc.MethodDescriptor
            import io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING
            import java.lang.Void
            import javax.`annotation`.processing.Generated
            import kotlin.Int
            import kotlin.String

            public object Descriptor {
              public val intMarshaller: Void = TODO()

              public val stringMarshaller: Void = TODO()

              /**
               * Generated gRPC [MethodDescriptor] for the
               * [TestService.bidiStream][com.test.generated.TestService.bidiStream] method.
               *
               * This descriptor is used by generated service components and should not be used in general code.
               */
              @Generated("com.github.darvld.krpc")
              public val bidiStream: MethodDescriptor<Int, String> = MethodDescriptor
                .newBuilder<Int, String>()
                .setFullMethodName("TestService/bidiStreamTest")
                .setType(BIDI_STREAMING)
                .setRequestMarshaller(intMarshaller)
                .setResponseMarshaller(stringMarshaller)
                .build()

            }
            
            """.trimIndent()
        )
    }
}