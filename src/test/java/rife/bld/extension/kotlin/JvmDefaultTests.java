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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link JvmDefault} enum.
 */
class JvmDefaultTests {

    @Nested
    @DisplayName("Enum Tests")
    class EnumTests {

        @Test
        void shouldHaveCorrectDisableMode() {
            assertThat(JvmDefault.DISABLE.value).as("DISABLE").isEqualTo("disable");
        }

        @Test
        void shouldHaveCorrectEnableMode() {
            assertThat(JvmDefault.ENABLE.value).as("ENABLE").isEqualTo("enable");
        }

        @Test
        void shouldHaveCorrectNoCompatibilityMode() {
            assertThat(JvmDefault.NO_COMPATIBILITY.value).as("NO_COMPATIBILITY")
                    .isEqualTo("no-compatibility");
        }
    }

    @Nested
    @DisplayName("Enum ValueOf Tests")
    class EnumValueOfTests {

        @Test
        void shouldHaveCorrectDisableValue() {
            assertThat(JvmDefault.valueOf("DISABLE")).as("DISABLE").isEqualTo(JvmDefault.DISABLE);
        }

        @Test
        void shouldHaveCorrectEnableValue() {
            assertThat(JvmDefault.valueOf("ENABLE")).as("ENABLE").isEqualTo(JvmDefault.ENABLE);
        }

        @Test
        void shouldHaveCorrectNoCompatibilityValue() {
            assertThat(JvmDefault.valueOf("NO_COMPATIBILITY")).as("NO_COMPATIBILITY")
                    .isEqualTo(JvmDefault.NO_COMPATIBILITY);
        }

        /**
         * Verifies that the {@code valueOf()} method throws an {@link IllegalArgumentException} when an invalid name is
         * provided.
         */
        @Test
        void valueOfWithInvalidNameThrowsException() {
            var invalidName = "INVALID_NAME";
            var exception = assertThrows(IllegalArgumentException.class,
                    () -> JvmDefault.valueOf(invalidName));

            assertThat(exception).as("Exception message should contain the invalid name.")
                    .hasMessageContaining(invalidName);
        }

        /**
         * Tests the standard {@code values()} method to ensure it returns all enum constants in the declared order.
         */
        @Test
        void valuesMethod() {
            JvmDefault[] expectedValues = {JvmDefault.ENABLE, JvmDefault.NO_COMPATIBILITY, JvmDefault.DISABLE};
            var actualValues = JvmDefault.values();

            assertThat(expectedValues).as("values() should return all constants in declaration order.")
                    .isEqualTo(actualValues);
            assertThat(actualValues).as("There should be exactly 3 JvmDefault constants.").hasSize(3);
        }
    }
}
