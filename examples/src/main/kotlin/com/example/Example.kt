package com.example

/**
 * Example class.
 */
class Example {
    /**
     * Message property.
     */
    val message: String
        /**
         * Returns the message property.
         */
        get() = "Hello World!"

    /**
     * Companion object.
     */
    companion object {
        /**
         * Main function.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            println(Example().message)
        }
    }
}