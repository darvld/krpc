@file:Suppress("nothing_to_inline")

package com.github.darvld.krpc.compiler.testing

import kotlin.test.assertContains
import kotlin.test.assertEquals

inline infix fun <T> T.shouldBe(expected: T) {
    assertEquals(expected, this)
}

inline infix fun String.shouldContain(expected: String) {
    assertContains(this, expected)
}