/*
 * Copyright 2023-2024 the original author or authors.
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
 * Defines the known Kotlin compiler plugins match (regex) strings.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public enum CompilerPlugin {
    ALL_OPEN("^kotlin-allopen-compiler-plugin-.*$"),
    ASSIGNMENT("^kotlin-assignment-compiler-plugin-.*$"),
    KOTLIN_SERIALIZATION("^kotlin-serialization-compiler-plugin-.*$"),
    LOMBOK("^kotlin-lombok-compiler-plugin-.*$"),
    NOARG("^kotlin-noarg-compiler-plugin-.*$"),
    POWER_ASSERT("^kotlin-power-assert-compiler-plugin-.*$"),
    SAM_WITH_RECEIVER("^kotlin-sam-with-receiver-compiler-plugin-.*$");

    public final String regex;

    CompilerPlugin(String regex) {
        this.regex = regex;
    }
}
