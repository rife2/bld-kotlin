/*
 * Copyright 2023-2025 the original author or authors.
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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class JvmOptionsTest {
    @Test
    void testCompileOptions() {
        var compileOptions = new CompileOptions().jvmOptions(new JvmOptions().enableNativeAccess(JvmOptions.ALL_UNNAMED));
        assertThat(compileOptions.jvmOptions()).as(JvmOptions.ALL_UNNAMED).containsExactly("--enable-native-access=ALL-UNNAMED");
        assertThat(compileOptions.args()).as("args()").containsExactly("-J--enable-native-access=ALL-UNNAMED");

        compileOptions = new CompileOptions().jvmOptions(new JvmOptions().enableNativeAccess("m1", "m2"));
        assertThat(compileOptions.jvmOptions()).as("m1,m2").containsExactly("--enable-native-access=m1,m2");
        assertThat(compileOptions.args()).as("args(m1,m2)").containsExactly("-J--enable-native-access=m1,m2");
    }

    @Test
    void testEnableNativeAccess() {
        var options = new JvmOptions().enableNativeAccess(JvmOptions.ALL_UNNAMED);
        assertThat(options).as(JvmOptions.ALL_UNNAMED).containsExactly("--enable-native-access=ALL-UNNAMED");

        options = new JvmOptions().enableNativeAccess("m1", "m2");
        assertThat(options).as("m1,m2").containsExactly("--enable-native-access=m1,m2");
    }

    @Test
    void testJvmOptions() {
        var compileOptions = new CompileOptions().jvmOptions("option1", "option2");
        assertThat(compileOptions.jvmOptions()).as("option1,option2").containsExactly("option1", "option2");
        assertThat(compileOptions.args()).as("args()").containsExactly("-Joption1", "-Joption2");

        compileOptions = new CompileOptions().jvmOptions(List.of("option1", "option2"));
        assertThat(compileOptions.jvmOptions()).as("List.of(option1,option2)").containsExactly("option1", "option2");
        assertThat(compileOptions.args()).as("args(list)").containsExactly("-Joption1", "-Joption2");

        compileOptions = compileOptions.jvmOptions(new JvmOptions().enableNativeAccess(JvmOptions.ALL_UNNAMED));
        assertThat(compileOptions.jvmOptions()).as("List.of(option1,option2,ALL_UNNAMED)")
                .containsExactly("option1", "option2", "--enable-native-access=ALL-UNNAMED");
        assertThat(compileOptions.args()).as("args(option1,option2,ALL_UNNAMED)")
                .containsExactly("-Joption1", "-Joption2", "-J--enable-native-access=ALL-UNNAMED");
    }
}
