import com.github.darvld.krpc.compiler.FlowClassName
import com.github.darvld.krpc.compiler.generators.addServiceMethodDescriptor
import com.github.darvld.krpc.compiler.generators.generateServiceDescriptor
import com.github.darvld.krpc.compiler.generators.getOrAddMarshaller
import com.github.darvld.krpc.compiler.model.BidiStreamMethod
import com.github.darvld.krpc.compiler.model.UnaryMethod
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

class DescriptorTest : CompilerTest() {
    
    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()
    
    @Test
    fun `generates descriptor skeleton`() {
        val definition = serviceDefinition()
        val file = temporaryFolder.newFile()
        
        file.outputStream().use { stream -> generateServiceDescriptor(stream, definition) }
        
        assertEquals(
            actual = file.readText(),
            expected = """
            package com.test.generated

            import com.github.darvld.krpc.SerializationProvider
            import javax.`annotation`.processing.Generated
            import kotlinx.serialization.serializer

            /**
             * Internal helper class generated by the Krpc compiler, This class is intended to be used only by
             * generated declarations and should not be used in general code.
             *
             * @param serializationProvider A provider implementing a serialization format. Used to generate
             * marshallers for rpc methods.
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
            getOrAddMarshaller(Int::class.asTypeName())
        }
        
        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.grpc.MethodDescriptor
            import javax.`annotation`.processing.Generated
            import kotlin.Int
            import kotlinx.serialization.serializer
            
            public object Marshallers {
              /**
               * A generated [MethodDescriptor.Marshaller] obtained using the `serializationProvider`
               * constructor parameter.
               */
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
            assertEquals("SerializationProvider.UnitMarshaller", getOrAddMarshaller(Unit::class.asTypeName()))
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
            getOrAddMarshaller(Int::class.asTypeName())
            assertEquals("intMarshaller", getOrAddMarshaller(Int::class.asTypeName()))
        }
        
        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.grpc.MethodDescriptor
            import javax.`annotation`.processing.Generated
            import kotlin.Int
            import kotlinx.serialization.serializer
            
            public object Marshallers {
              /**
               * A generated [MethodDescriptor.Marshaller] obtained using the `serializationProvider`
               * constructor parameter.
               */
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
        val method = UnaryMethod(
            declaredName = "unary",
            methodName = "officialName",
            requestName = "request",
            requestType = Int::class.asClassName(),
            returnType = String::class.asClassName()
        )
        
        val generated = temporaryFolder.newObject("Descriptor") {
            // Placeholder properties so the marshallers are not generated (already covered by another test)
            addProperty(PropertySpec.builder("intMarshaller", Nothing::class).initializer("TODO()").build())
            addProperty(PropertySpec.builder("stringMarshaller", Nothing::class).initializer("TODO()").build())
            
            addServiceMethodDescriptor(service, method)
        }
        
        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.grpc.MethodDescriptor
            import java.lang.Void
            import javax.`annotation`.processing.Generated
            import kotlin.Int
            import kotlin.String
            
            public object Descriptor {
              public val intMarshaller: Void = TODO()

              public val stringMarshaller: Void = TODO()

              /**
               * A generated [MethodDescriptor] for the [TestService.unary] method.
               *
               * This descriptor is used by generated client and server implementations. It should not be
               * used in general code.
               */
              @Generated("com.github.darvld.krpc")
              public val unary: MethodDescriptor<Int, String> = MethodDescriptor.newBuilder<Int, String>()
                    .setFullMethodName("TestService/officialName")
                    .setType(MethodDescriptor.MethodType.UNARY)
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
        val method = BidiStreamMethod(
            declaredName = "bidiStream",
            methodName = "officialName",
            requestName = "request",
            requestType = FlowClassName.parameterizedBy(Int::class.asClassName()),
            returnType = FlowClassName.parameterizedBy(String::class.asClassName())
        )
        
        val generated = temporaryFolder.newObject("Descriptor") {
            // Placeholder properties so the marshallers are not generated (already covered by another test)
            addProperty(PropertySpec.builder("intMarshaller", Nothing::class).initializer("TODO()").build())
            addProperty(PropertySpec.builder("stringMarshaller", Nothing::class).initializer("TODO()").build())
            
            addServiceMethodDescriptor(service, method)
        }
        
        generated.assertContentEquals(
            """
            package com.test.generated
            
            import io.grpc.MethodDescriptor
            import java.lang.Void
            import javax.`annotation`.processing.Generated
            import kotlin.Int
            import kotlin.String
            
            public object Descriptor {
              public val intMarshaller: Void = TODO()

              public val stringMarshaller: Void = TODO()

              /**
               * A generated [MethodDescriptor] for the [TestService.bidiStream] method.
               *
               * This descriptor is used by generated client and server implementations. It should not be
               * used in general code.
               */
              @Generated("com.github.darvld.krpc")
              public val bidiStream: MethodDescriptor<Int, String> = MethodDescriptor
                .newBuilder<Int, String>()
                .setFullMethodName("TestService/officialName")
                .setType(MethodDescriptor.MethodType.BIDI_STREAMING)
                .setRequestMarshaller(intMarshaller)
                .setResponseMarshaller(stringMarshaller)
                .build()

            }
            
            """.trimIndent()
        )
    }
}