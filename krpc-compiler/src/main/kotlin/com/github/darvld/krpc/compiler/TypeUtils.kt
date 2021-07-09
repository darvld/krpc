package com.github.darvld.krpc.compiler

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.coroutines.flow.Flow

internal val UnitClassName by lazy { Unit::class.asClassName() }
internal val FlowClassName by lazy { Flow::class.asClassName() }

/**Resolves this type reference and constructs a [ClassName] from the type's package name and simple name.
 *
 *  This extension does not handle generics, use [resolveAsParameterizedName] instead.*/
internal fun KSTypeReference.resolveAsClassName(): ClassName {
    return resolve().declaration.run {
        ClassName(packageName.asString(), simpleName.asString())
    }
}

/**Resolves this type reference and attempts construct a [ParameterizedTypeName].*/
internal fun KSTypeReference.resolveAsParameterizedName(): ParameterizedTypeName? {
    return resolve().asTypeName() as? ParameterizedTypeName
}

/**Maps this ksp [KSType] into a kotlinPoet [TypeName].
 *
 * For non-generic types, this method returns a simple [ClassName]. For generics, it recursively resolves
 * type arguments.*/
internal fun KSType.asTypeName(): TypeName {
    val baseName = ClassName(declaration.packageName.asString(), declaration.simpleName.asString())

    if (arguments.isEmpty()) return baseName

    return baseName.parameterizedBy(arguments.map { it.type?.resolve()?.asTypeName() ?: STAR })
}