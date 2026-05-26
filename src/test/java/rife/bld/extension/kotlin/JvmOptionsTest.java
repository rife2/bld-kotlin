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
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class JvmOptionsTest {

    @Test
    @SuppressWarnings("deprecation")
    void jvmOptions() {
        var jvmOptions = new JvmOptions();
        jvmOptions.nativeAccessModules("foo", "bar");
        jvmOptions.illegalNativeAccess(JvmOptions.NativeAccess.ALLOW);

        var kotlinOperation = new CompileKotlinOperation().jvmOptions(jvmOptions);
        var options = kotlinOperation.jvmOptions();

        assertThat(options.nativeAccessModules()).containsExactly("foo", "bar");
        assertThat(options.illegalNativeAccess()).isEqualTo(JvmOptions.NativeAccess.ALLOW);
    }

    @Nested
    @DisplayName("Fluent API Tests")
    class FluentApiTests {

        @Test
        @SuppressWarnings("deprecation")
        void shouldAllowMethodChaining() {
            var options = new JvmOptions()
                    .nativeAccessModules("my.module", JvmOptions.ALL_UNNAMED)
                    .illegalNativeAccess(JvmOptions.NativeAccess.WARN);

            assertThat(options.args()).containsExactly(
                    "--enable-native-access=my.module,ALL-UNNAMED",
                    "--illegal-native-access=warn"
            );
        }

        @Test
        @SuppressWarnings("deprecation")
        void shouldReturnTheSameInstance() {
            var options = new JvmOptions();

            var returnedFromEnable = options.nativeAccessModules("mod1");
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
    @SuppressWarnings("deprecation")
    class IllegalNativeAccessTests {

        @Test
        void illegalNativeAccessWithAllow() {
            var options = new JvmOptions().illegalNativeAccess(JvmOptions.NativeAccess.ALLOW);
            assertThat(options.illegalNativeAccess()).as("ALLOW").isEqualTo(JvmOptions.NativeAccess.ALLOW);
            assertThat(options.args()).as("allow").containsExactly("--illegal-native-access=allow");
        }

        @Test
        void illegalNativeAccessWithDeny() {
            var options = new JvmOptions().illegalNativeAccess(JvmOptions.NativeAccess.DENY);
            assertThat(options.illegalNativeAccess()).as("DENY").isEqualTo(JvmOptions.NativeAccess.DENY);
            assertThat(options.args()).as("deny").containsExactly("--illegal-native-access=deny");
        }

        @Test
        void illegalNativeAccessWithWarn() {
            var options = new JvmOptions().illegalNativeAccess(JvmOptions.NativeAccess.WARN);
            assertThat(options.illegalNativeAccess()).as("WARN").isEqualTo(JvmOptions.NativeAccess.WARN);
            assertThat(options.args()).as("warn").containsExactly("--illegal-native-access=warn");
        }
    }

    @Nested
    @DisplayName("NativeAccess Enum Tests")
    class NativeAccessEnumTests {

        @Test
        void shouldHaveCorrectAllowMode() {
            assertThat(JvmOptions.NativeAccess.ALLOW.getMode()).isEqualTo("allow");
        }

        @Test
        void shouldHaveCorrectDenyMode() {
            assertThat(JvmOptions.NativeAccess.DENY.getMode()).isEqualTo("deny");
        }

        @Test
        void shouldHaveCorrectWarnMode() {
            assertThat(JvmOptions.NativeAccess.WARN.getMode()).isEqualTo("warn");
        }
    }

    @Nested
    @DisplayName("Native Access Modules Tests")
    class NativeAccessModulesTests {

        @Test
        void enableNativeAccessWithAllUnnamed() {
            var options = new JvmOptions().nativeAccessModules(JvmOptions.ALL_UNNAMED);
            assertThat(options.nativeAccessModules()).containsExactly("ALL-UNNAMED");
            assertThat(options.args()).as(JvmOptions.ALL_UNNAMED)
                    .containsExactly("--enable-native-access=ALL-UNNAMED");
        }

        @Test
        void enableNativeAccessWithCollection() {
            var options = new JvmOptions().nativeAccessModules(List.of("moduleA", "moduleB"));
            assertThat(options.nativeAccessModules()).containsExactly("moduleA", "moduleB");
            assertThat(options.args()).containsExactly("--enable-native-access=moduleA,moduleB");
        }

        @Test
        void enableNativeAccessWithEmptyCollection() {
            assertThatThrownBy(() -> new JvmOptions().nativeAccessModules(Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void enableNativeAccessWithEmptyVarargs() {
            assertThatThrownBy(() -> new JvmOptions().nativeAccessModules("foo", "", "bar"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void enableNativeAccessWithList() {
            var options = new JvmOptions().nativeAccessModules(List.of("module1", "module2"));
            assertThat(options.args()).containsExactly("--enable-native-access=module1,module2");
        }

        @Test
        void enableNativeAccessWithMultipleNames() {
            var options = new JvmOptions().nativeAccessModules("m1", "m2");
            assertThat(options.args()).as("m1,m2").containsExactly("--enable-native-access=m1,m2");
        }

        @Test
        void enableNativeAccessWithName() {
            var options = new JvmOptions().nativeAccessModules("m1");
            assertThat(options.args()).as("m1").containsExactly("--enable-native-access=m1");
        }

        @Test
        void enableNativeAccessWithSingleVararg() {
            var options = new JvmOptions().nativeAccessModules("one.module");
            assertThat(options.args()).containsExactly("--enable-native-access=one.module");
        }

        @Test
        void enableNativeAccessWithVarargs() {
            var options = new JvmOptions().nativeAccessModules("module1", "module2");
            assertThat(options.args()).containsExactly("--enable-native-access=module1,module2");
        }
    }
}
