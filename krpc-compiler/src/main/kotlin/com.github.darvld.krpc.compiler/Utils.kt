package com.github.darvld.krpc.compiler

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import javax.annotation.processing.Generated

fun TypeSpec.Builder.markAsGenerated() = apply {
    addAnnotation(Generated::class)
}

fun FunSpec.Builder.markAsGenerated() = apply {
    addAnnotation(Generated::class)
}

fun PropertySpec.Builder.markAsGenerated() = apply {
    addAnnotation(Generated::class)
}