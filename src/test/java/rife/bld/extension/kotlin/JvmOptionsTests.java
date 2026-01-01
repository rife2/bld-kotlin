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
import rife.bld.extension.CompileKotlinOperation;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class JvmOptionsTests {
    @Test
    void jvmOptions() {
        var op = new CompileKotlinOperation().jvmOptions("--option1", "--option2");
        assertThat(op.jvmOptions()).as("--option1,--option2").containsExactly("--option1", "--option2");
    }

    @Test
    void jvmOptionsWithList() {
        var op = new CompileKotlinOperation().jvmOptions(List.of("--option1", "--option2"));
        assertThat(op.jvmOptions()).as("List.of(--option1,--option2)")
                .containsExactly("--option1", "--option2");
    }

    @Test
    void jvmOptionsWithNativeAccessAllUnnamed() {
        var jvmOptions = new JvmOptions().enableNativeAccess(JvmOptions.ALL_UNNAMED);
        jvmOptions.addAll(List.of("--option1", "--option2"));
        var op = new CompileKotlinOperation().jvmOptions(jvmOptions);

        assertThat(op.jvmOptions()).as("List.of(ALL_UNNAMED,--option1,--option2)")
                .containsExactly("--enable-native-access=ALL-UNNAMED", "--option1", "--option2");
    }

    @Test
    void jvmOptionsWithNativeAccessAllow() {
        var jvmOptions = new JvmOptions().illegalNativeAccess(JvmOptions.NativeAccess.ALLOW);
        jvmOptions.addAll(List.of("--option1", "--option2"));
        var op = new CompileKotlinOperation().jvmOptions(jvmOptions);

        assertThat(op.jvmOptions()).as("allow")
                .containsExactly("--illegal-native-access=allow", "--option1", "--option2");
    }

    @Nested
    @DisplayName("Enable Native Access Tests")
    class EnableNativeAccessTests {
        @Test
        void enableNativeAccessWithAllUnnamed() {
            var options = new JvmOptions().enableNativeAccess(JvmOptions.ALL_UNNAMED);
            assertThat(options).as(JvmOptions.ALL_UNNAMED).containsExactly("--enable-native-access=ALL-UNNAMED");
        }

        @Test
        void enableNativeAccessWithCollection() {
            var options = new JvmOptions().enableNativeAccess(List.of("moduleA", "moduleB"));
            assertThat(options).containsExactly("--enable-native-access=moduleA,moduleB");
        }

        @Test
        void enableNativeAccessWithEmptyCollection() {
            var options = new JvmOptions().enableNativeAccess(Set.of());
            assertThat(options).containsExactly("--enable-native-access=");
        }

        @Test
        void enableNativeAccessWithEmptyVarargs() {
            var options = new JvmOptions().enableNativeAccess();
            assertThat(options).containsExactly("--enable-native-access=");
        }

        @Test
        void enableNativeAccessWithMultipleNames() {
            var options = new JvmOptions().enableNativeAccess("m1", "m2");
            assertThat(options).as("m1,m2").containsExactly("--enable-native-access=m1,m2");
        }

        @Test
        void enableNativeAccessWithName() {
            var options = new JvmOptions().enableNativeAccess("m1");
            assertThat(options).as("m1").containsExactly("--enable-native-access=m1");
        }

        @Test
        void enableNativeAccessWithSingleVararg() {
            var options = new JvmOptions().enableNativeAccess("one.module");
            assertThat(options).containsExactly("--enable-native-access=one.module");
        }

        @Test
        void enableNativeAccessWithVarargs() {
            var options = new JvmOptions().enableNativeAccess("module1", "module2");
            assertThat(options).containsExactly("--enable-native-access=module1,module2");
        }
    }

    @Nested
    @DisplayName("Fluent API Tests")
    class FluentApiTests {
        @Test
        void shouldAllowMethodChaining() {
            var options = new JvmOptions()
                    .enableNativeAccess("my.module", JvmOptions.ALL_UNNAMED)
                    .illegalNativeAccess(JvmOptions.NativeAccess.WARN);

            assertThat(options).containsExactly(
                    "--enable-native-access=my.module,ALL-UNNAMED",
                    "--illegal-native-access=warn"
            );
        }

        @Test
        void shouldReturnTheSameInstance() {
            var options = new JvmOptions();

            var returnedFromEnable = options.enableNativeAccess("mod1");
            assertThat(returnedFromEnable)
                    .withFailMessage("enableNativeAccess should return the same instance for chaining.")
                    .isSameAs(options);

            var returnedFromIllegal = options.illegalNativeAccess(JvmOptions.NativeAccess.DENY);
            assertThat(returnedFromIllegal)
                    .withFailMessage("illegalNativeAccess should return the same instance for chaining.")
                    .isSameAs(options);
        }
    }

    @Nested
    @DisplayName("Illegal Native Access Tests")
    class IllegalNativeAccessTests {
        @Test
        void illegalNativeAccessWithAllow() {
            var options = new JvmOptions().illegalNativeAccess(JvmOptions.NativeAccess.ALLOW);
            assertThat(options).as("ALLOW").containsExactly("--illegal-native-access=allow");
        }

        @Test
        void illegalNativeAccessWithDeny() {
            var options = new JvmOptions().illegalNativeAccess(JvmOptions.NativeAccess.DENY);
            assertThat(options).as("DENY").containsExactly("--illegal-native-access=deny");
        }

        @Test
        void illegalNativeAccessWithWarn() {
            var options = new JvmOptions().illegalNativeAccess(JvmOptions.NativeAccess.WARN);
            assertThat(options).as("WARN").containsExactly("--illegal-native-access=warn");
        }
    }

    @Nested
    @DisplayName("NativeAccess Enum Tests")
    class NativeAccessEnumTests {
        @Test
        void shouldHaveCorrectAllowMode() {
            assertThat(JvmOptions.NativeAccess.ALLOW.mode).isEqualTo("allow");
        }

        @Test
        void shouldHaveCorrectDenyMode() {
            assertThat(JvmOptions.NativeAccess.DENY.mode).isEqualTo("deny");
        }

        @Test
        void shouldHaveCorrectWarnMode() {
            assertThat(JvmOptions.NativeAccess.WARN.mode).isEqualTo("warn");
        }
    }
}
