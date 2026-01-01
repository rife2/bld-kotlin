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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class CompilerPluginTests {
    @Test
    @DisplayName("Ensure the number of compiler plugins is correct")
    void testEnumSize() {
        // This test provides a quick check that no enums were added or removed without updating the tests.
        assertThat(CompilerPlugin.values())
                .withFailMessage("There should be 10 compiler plugins.")
                .hasSize(10);
    }

    @Test
    @DisplayName("Test individual enum valueOf() and jar property")
    void testIndividualPlugin() {
        var allOpen = CompilerPlugin.valueOf("ALL_OPEN");
        assertThat(allOpen).isEqualTo(CompilerPlugin.ALL_OPEN);
        assertThat(allOpen.jar).isEqualTo("allopen-compiler-plugin.jar");
    }

    @ParameterizedTest
    @EnumSource(CompilerPlugin.class)
    @DisplayName("Verify the JAR file name for each compiler plugin")
    void testPluginJarFileName(CompilerPlugin plugin) {
        switch (plugin) {
            case ALL_OPEN -> assertThat(plugin.jar).isEqualTo("allopen-compiler-plugin.jar");
            case ASSIGNMENT -> assertThat(plugin.jar).isEqualTo("assignment-compiler-plugin.jar");
            case COMPOSE -> assertThat(plugin.jar).isEqualTo("compose-compiler-plugin.jar");
            case KOTLIN_IMPORTS_DUMPER -> assertThat(plugin.jar).isEqualTo("kotlin-imports-dumper-compiler-plugin.jar");
            case KOTLINX_SERIALIZATION -> assertThat(plugin.jar).isEqualTo("kotlinx-serialization-compiler-plugin.jar");
            case KOTLIN_SERIALIZATION -> assertThat(plugin.jar).isEqualTo("kotlin-serialization-compiler-plugin.jar");
            case LOMBOK -> assertThat(plugin.jar).isEqualTo("lombok-compiler-plugin.jar");
            case NOARG -> assertThat(plugin.jar).isEqualTo("noarg-compiler-plugin.jar");
            case POWER_ASSERT -> assertThat(plugin.jar).isEqualTo("power-assert-compiler-plugin.jar");
            case SAM_WITH_RECEIVER -> assertThat(plugin.jar).isEqualTo("sam-with-receiver-compiler-plugin.jar");
            default -> throw new IllegalStateException("Unhandled plugin in test: " + plugin);
        }
    }
}