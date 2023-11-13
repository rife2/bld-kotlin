/*
 * Copyright 2023 the original author or authors.
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

package rife.bld.extension;

public enum CompileKotlinPlugin {
    ALL_OPEN("^allopen-compiler-plugin-.*$"),
    ASSIGNMENT("^assignment-compiler-plugin-.*$"),
    KOTLIN_IMPORTS_DUMPER("^kotlin-imports-dumper-compiler-plugin-.*$"),
    KOTLIN_SERIALIZATION("^kotlin-serialization-compiler-plugin-.*$"),
    KOTLINX_SERIALIZATION("^kotlinx-serialization-compiler-plugin-.*$"),
    LOMBOK("^lombok-compiler-plugin-.*$"),
    NOARG("^noarg-compiler-plugin-.*$"),
    SAM_WITH_RECEIVER("^sam-with-receiver-compiler-plugin-.*$");

    public final String label;

    CompileKotlinPlugin(String label) {
        this.label = label;
    }
}
