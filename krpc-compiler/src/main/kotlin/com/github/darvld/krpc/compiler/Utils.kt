package com.github.darvld.krpc.compiler

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream
import javax.annotation.processing.Generated

internal val UnitClassName = Unit::class.asClassName()
internal val FlowClassName = Flow::class.asClassName()

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

/**Returns a [ClassName] constructed from this symbol's package name and simple name.*/
fun KSType.asClassName(): ClassName {
    return ClassName(declaration.packageName.asString(), declaration.simpleName.asString())
}

/**Resolves this type reference and constructs a [ClassName] from the type's package name and simple name.
 *
 *  This extension does not handle generics, use [resolveParameterizedName] instead.*/
fun KSTypeReference.resolveAsClassName(): ClassName {
    return resolve().declaration.run {
        ClassName(packageName.asString(), simpleName.asString())
    }
}

/**Maps this ksp [KSType] into a kotlinPoet [TypeName].
 *
 * For non-generic types, this method returns a simple [ClassName]. For generics, it recursively resolves
 * type arguments.*/
fun KSType.asTypeName(): TypeName {
    val baseName = ClassName(declaration.packageName.asString(), declaration.simpleName.asString())

    if (arguments.isEmpty()) return baseName

    return baseName.parameterizedBy(arguments.map { it.type?.resolve()?.asTypeName() ?: STAR })
}

/**Resolves this type references and recursively constructs a properly parameterized type name including the type arguments.*/
inline fun KSTypeReference.resolveParameterizedName(validate: (KSType) -> Boolean = { true }): ParameterizedTypeName? {
    return resolve().also { if (!validate(it)) return null }.run {
        // It *looks* unsafe, but it's actually perfectly safe: if the `asTypeName` call returns a ClassName,
        // then this will return null, otherwise we get the appropriate parametrized type.
        asTypeName() as? ParameterizedTypeName
    }
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