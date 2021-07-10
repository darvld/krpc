package com.github.darvld.krpc.compiler.testing

import com.squareup.kotlinpoet.asClassName

object ClassNames {
    val List by lazy { List::class.asClassName() }
    val Int by lazy { Int::class.asClassName() }
    val String by lazy { String::class.asClassName() }
}