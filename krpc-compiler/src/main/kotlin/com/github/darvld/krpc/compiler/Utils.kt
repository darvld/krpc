package com.github.darvld.krpc.compiler

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream
import javax.annotation.processing.Generated

val UnitClassName = Unit::class.asClassName()
val FlowClassName = Flow::class.asClassName()

private val generatedAnnotationSpec = AnnotationSpec.builder(Generated::class)
    .addMember("\"com.github.darvld.krpc\"")
    .build()

/**Marks this type as generated by the KRPC compiler.*/
fun TypeSpec.Builder.markAsGenerated(): TypeSpec.Builder {
    return apply {
        addAnnotation(generatedAnnotationSpec)
    }
}

/**Marks this function as generated by the KRPC compiler.*/
fun FunSpec.Builder.markAsGenerated(): FunSpec.Builder {
    return apply {
        addAnnotation(generatedAnnotationSpec)
    }
}

/**Marks this property as generated by the KRPC compiler.*/
fun PropertySpec.Builder.markAsGenerated(): PropertySpec.Builder {
    return apply {
        addAnnotation(generatedAnnotationSpec)
    }
}

/**Constructs a [ClassName] from this type's package name and simple name. Note that for generics, type
 * arguments must be added manually.*/
fun KSType.asClassName(): ClassName {
    return ClassName(declaration.packageName.asString(), declaration.simpleName.asString())
}

/**Build a file using [FileSpec.Builder] and write it to [output].*/
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