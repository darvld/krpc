package com.github.darvld.krpc.compiler

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import java.io.OutputStream
import javax.annotation.processing.Generated


private val generatedAnnotationSpec = AnnotationSpec.builder(Generated::class)
    .addMember("\"com.github.darvld.krpc\"")
    .build()

fun TypeSpec.Builder.markAsGenerated() = apply {
    addAnnotation(generatedAnnotationSpec)
}

fun FunSpec.Builder.markAsGenerated() = apply {
    addAnnotation(generatedAnnotationSpec)
}

fun PropertySpec.Builder.markAsGenerated() = apply {
    addAnnotation(generatedAnnotationSpec)
}

fun KSType.asClassName(): ClassName {
    return ClassName(declaration.packageName.asString(), declaration.simpleName.asString())
}

inline fun buildFile(
    withPackage: String,
    fileName: String,
    output: OutputStream,
    block: FileSpec.Builder.() -> Unit
) {
    FileSpec.builder(withPackage, fileName).apply(block).build().let { spec ->
        output.writer().use(spec::writeTo)
    }
}

/**Build a class using [TypeSpec.classBuilder] and automatically add it to the current file.*/
inline fun FileSpec.Builder.addClass(
    packageName: String = this.packageName,
    className: String = this.name,
    block: TypeSpec.Builder.() -> Unit
): TypeSpec {
    return TypeSpec.classBuilder(ClassName(packageName, className)).apply(block).build().also(::addType)
}


/**Build a function using [TypeSpec.classBuilder] and automatically add it to the current class.*/
inline fun TypeSpec.Builder.addFunction(
    name: String,
    vararg modifiers: KModifier,
    block: FunSpec.Builder.() -> Unit
): FunSpec {
    return FunSpec.builder(name).apply { addModifiers(*modifiers); block() }.build().also(::addFunction)
}