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
import com.squareup.kotlinpoet.KModifier.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.darvld.krpc.AbstractServiceDescriptor
import io.github.darvld.krpc.MethodType
import io.github.darvld.krpc.compiler.METHOD_DESCRIPTOR
import io.github.darvld.krpc.compiler.SERIALIZATION_PROVIDER
import io.github.darvld.krpc.compiler.SERIALIZATION_PROVIDER_PARAM
import io.github.darvld.krpc.compiler.SERVICE_NAME_PROPERTY
import io.github.darvld.krpc.compiler.dsl.*
import io.github.darvld.krpc.compiler.model.CompositeRequest
import io.github.darvld.krpc.compiler.model.RequestInfo.Companion.requestTypeFor
import io.github.darvld.krpc.compiler.model.ServiceDefinition
import io.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import kotlinx.serialization.Serializable
import java.io.OutputStream

internal class DescriptorGenerator : ServiceComponentGenerator() {

    override fun getFilename(service: ServiceDefinition): String {
        return service.descriptorName
    }

    override fun generateComponent(output: OutputStream, service: ServiceDefinition) {
        buildFile(service.packageName, service.descriptorName, output) {
            addClass {
                markAsGenerated()
                superclass(AbstractServiceDescriptor::class)

                addKdoc(
                    """
                    Descriptor generated by the kRPC compiler for the [${service.className}] interface.
                         
                    This class provides method descriptors for other generated service components, you can
                    use it to build your own service components instead of using the ones generated by the
                    compiler.
                    """.trimIndent(),
                )

                // Constructor with serialization provider as parameter (used to initialize method descriptors)
                constructor(primary = true) {
                    parameter(SERIALIZATION_PROVIDER_PARAM, SERIALIZATION_PROVIDER)
                }

                // Override serviceName abstract property
                property(SERVICE_NAME_PROPERTY, STRING, OVERRIDE) {
                    initializer("%S", service.serviceName)
                }

                // Generate method descriptors
                for (method in service.methods) {
                    val requestType = service.requestTypeFor(method)

                    if (method.request is CompositeRequest) addType(buildRequestWrapper(method))

                    addTranscoder(requestType)
                    addTranscoder(method.responseType)

                    val descriptorDoc = """
                    Generated gRPC [MethodDescriptor] for the [${service.declaredName}.${method.declaredName}] method.
                    
                    This descriptor is used by generated service components and should not be used in general code.
                    """.trimIndent()
                    addProperty(buildMethodDescriptor(method, requestType, descriptorDoc))
                }
            }
        }
    }

    fun buildRequestWrapper(method: ServiceMethodDefinition): TypeSpec {
        require(method.request is CompositeRequest)

        return TypeSpec.classBuilder(method.request.wrapperName).apply {
            addModifiers(INTERNAL, DATA)
            addAnnotation(Serializable::class)
            markAsGenerated()

            addKdoc(
                """
                Internal wrapper class generated by the kRPC compiler.
                 
                This wrapper is used internally to support multiple arguments in service methods,
                it should not be used in general code.
                """.trimIndent()
            )

            val constructor = FunSpec.constructorBuilder()

            for ((name, type) in method.request.parameters) {
                constructor.addParameter(name, type)
                addProperty(PropertySpec.builder(name, type).initializer(name).build())
            }

            primaryConstructor(constructor.build())
        }.build()
    }

    fun buildMethodDescriptor(
        method: ServiceMethodDefinition,
        requestType: TypeName,
        kdoc: String? = null,
    ): PropertySpec = with(method) {
        return PropertySpec.builder(
            method.declaredName,
            METHOD_DESCRIPTOR.parameterizedBy(requestType, responseType),
            INTERNAL
        ).run {
            markAsGenerated()
            kdoc?.let(::addKdoc)

            initializer(
                """
                methodDescriptor(
                  name=%S,
                  type=%M,
                  requestTranscoder=%L,
                  responseTranscoder=%L
                )
                """.trimIndent(),
                methodName,
                methodType.asMember(),
                requestType.transcoderName,
                responseType.transcoderName
            )
            build()
        }
    }

    private fun MethodType.asMember(): MemberName {
        return MemberName(ClassName("io.github.darvld.krpc", "MethodType"), name)
    }
}
