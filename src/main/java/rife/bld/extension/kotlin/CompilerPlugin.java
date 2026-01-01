/*
 * Copyright 2023-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rife.bld.extension.kotlin;

/**
 * Defines the known Kotlin compiler plugin JARs.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public enum CompilerPlugin {
    /**
     * Represents the Kotlin {@code All-Open} compiler plugin JAR.
     * <p>
     * This plugin allows certain classes or methods to be open for inheritance without requiring explicit
     * modification of their declarations.
     */
    ALL_OPEN("allopen-compiler-plugin.jar"),
    /**
     * Represents the Kotlin {@code Assignment} compiler plugin JAR.
     * <p>
     * This plugin enables features related to assignments in Kotlin, enhancing the compilation process for
     * related constructs.
     */
    ASSIGNMENT("assignment-compiler-plugin.jar"),
    /**
     * Represents the Kotlin {@code Compose} compiler plugin JAR.
     * <p>
     * This plugin is used to enable and optimize Jetpack Compose usage within Kotlin projects,
     * integrating UI development into the compilation process.
     */
    COMPOSE("compose-compiler-plugin.jar"),
    /**
     * Represents the Kotlin Imports Dumper compiler plugin JAR.
     * <p>
     * This plugin is used to analyze and dump Kotlin imports during the compilation
     * process. It allows the export or inspection of import directives within Kotlin
     * sources, aiding in diagnostics or code analysis tasks within a build or compiler
     * plugin integration.
     */
    KOTLIN_IMPORTS_DUMPER("kotlin-imports-dumper-compiler-plugin.jar"),
    /**
     * Represents the Kotlin {@code Serialization} compiler plugin JAR.
     * <p>
     * This plugin facilitates serialization and deserialization of Kotlin classes,
     * allowing them to transform into various formats such as JSON, XML, or ProtoBuf.
     * It integrates seamlessly with Kotlin's syntax and provides efficient runtime
     * serialization mechanisms.
     */
    KOTLINX_SERIALIZATION("kotlinx-serialization-compiler-plugin.jar"),
    /**
     * Represents the Kotlinx {@code Serialization} compiler plugin JAR.
     * <p>
     * This plugin integrates serialization and deserialization capabilities into
     * Kotlin projects. It facilitates the transformation of Kotlin classes
     * into various formats, such as JSON or XML, by leveraging Kotlin's
     * serialization framework.
     */
    KOTLIN_SERIALIZATION("kotlin-serialization-compiler-plugin.jar"),
    /**
     * Represents the Kotlin {@code Lombok} compiler plugin JAR.
     * <p>
     * This plugin enables support for the Lombok Java library in Kotlin projects,
     * facilitating the generation of boilerplate code such as getters, setters,
     * equals, hashCode, and more during the compilation process.
     */
    LOMBOK("lombok-compiler-plugin.jar"),
    /**
     * Represents the {@code No-Arg} compiler plugin JAR.
     * <p>
     * This plugin enables the automatic generation of no-argument constructors for classes
     * and data classes in Kotlin.
     * <p>
     * This plugin is useful in scenarios where frameworks or libraries require objects
     * with no-argument constructors, such as when using reflection-based instantiation.
     */
    NOARG("noarg-compiler-plugin.jar"),
    /**
     * Represents the {@code Power-Assert} compiler plugin JAR.
     * <p>
     * This plugin often used to enable enhanced assertion capabilities in Kotlin compilation.
     * <p>
     * This plugin provides powerful runtime assertion messages, useful for debugging and
     * testing purposes.
     */
    POWER_ASSERT("power-assert-compiler-plugin.jar"),
    /**
     * Represents the Kotlin {@code SAM-with-receiver} compiler plugin JAR.
     * <p>
     * This plugin enables support for single abstract method (SAM) conversions with an
     * implicit receiver in Kotlin.
     */
    SAM_WITH_RECEIVER("sam-with-receiver-compiler-plugin.jar");

    public final String jar;

    CompilerPlugin(String jar) {
        this.jar = jar;
    }
}
