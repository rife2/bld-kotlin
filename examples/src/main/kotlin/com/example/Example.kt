package com.example

class Example {
    val message: String
        get() = "Hello World!"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println(Example().message)
        }
    }
}