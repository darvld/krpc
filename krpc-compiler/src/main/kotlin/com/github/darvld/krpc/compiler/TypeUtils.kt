package com.github.darvld.krpc.compiler

import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.coroutines.flow.Flow

internal val UnitClassName by lazy { Unit::class.asClassName() }
internal val FlowClassName by lazy { Flow::class.asClassName() }

/**Resolves this type reference and attempts construct a [ParameterizedTypeName].*/
internal fun KSTypeReference.resolveAsParameterizedName(): ParameterizedTypeName? {
    return resolveAsTypeName() as? ParameterizedTypeName
}

/**Resolves this ksp [KSTypeReference] as a kotlinPoet [TypeName].
 *
 * For non-generic types, this method returns a simple [ClassName]. For generics, it recursively resolves
 * type arguments.*/
internal fun KSTypeReference.resolveAsTypeName(): TypeName = with(resolve()) {
    val baseName = ClassName(declaration.packageName.asString(), declaration.simpleName.asString())

    if (arguments.isEmpty()) return baseName

    return baseName.parameterizedBy(arguments.map { it.type?.resolveAsTypeName() ?: STAR })
}