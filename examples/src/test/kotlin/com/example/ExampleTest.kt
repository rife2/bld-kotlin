package com.example

class ExampleTest {
    fun verifyHello() {
        if ("Hello World!" != Example().message) {
            throw AssertionError()
        } else {
            println("Succeeded")
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ExampleTest().verifyHello()
        }
    }
}