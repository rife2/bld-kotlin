package com.example

import kotlin.test.Test
import kotlin.test.assertEquals

class ExampleTest {
    @Test
    fun verifyHello() {
        assertEquals("Hello World!", Example().message)
    }
}