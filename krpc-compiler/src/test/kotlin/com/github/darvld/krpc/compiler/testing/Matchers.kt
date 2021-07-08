@file:Suppress("nothing_to_inline")

package com.github.darvld.krpc.compiler.testing

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast
import kotlin.test.assertContains
import kotlin.test.assertEquals

inline infix fun <T> T.shouldBe(expected: T) {
    assertEquals(expected, this)
}

inline infix fun String.shouldContain(expected: String) {
    assertContains(this, expected)
}

inline fun <reified R : Any> Any?.assertIs(message: String? = null): R {
    return assertedCast { message ?: "Asserted cast to ${R::class.qualifiedName} failed" }
}

inline fun <T> T?.assertNotNull(message: String? = null): T = kotlin.test.assertNotNull(this, message)