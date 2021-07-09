package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.compiler.FlowClassName
import com.github.darvld.krpc.compiler.model.*
import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

internal fun interface ServiceComponentGenerator {
    fun generate(codeGenerator: CodeGenerator, definition: ServiceDefinition)

    companion object {
        val ServiceMethodDefinition.parameterType: TypeName
            get() = if (this is ClientStreamMethod || this is BidiStreamMethod) {
                FlowClassName.parameterizedBy(requestType)
            } else {
                requestType
            }

        val ServiceMethodDefinition.returnType: TypeName
            get() = if (this is ServerStreamMethod || this is BidiStreamMethod) {
                FlowClassName.parameterizedBy(responseType)
            } else {
                responseType
            }
    }
}