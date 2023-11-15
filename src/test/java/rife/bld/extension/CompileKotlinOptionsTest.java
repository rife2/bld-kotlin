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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CompileKotlinOptionsTest {
    @Test
    void argsCollectionTest() {
        var args = new CompileKotlinOptions()
                .argFile(List.of("arg1.txt", "arg2.txt"))
                .classpath(List.of("path1", "path2"))
                .noStdLib(false)
                .optIn(List.of("opt1", "opt2"))
                .options(List.of("-foo", "-bar"))
                .scriptTemplates(List.of("temp1", "temp2"))
                .args();
        var matches = List.of(
                "@arg1.txt", "@arg2.txt",
                "-classpath", "path1:path2",
                "-opt-in", "opt1",
                "-opt-in", "opt2",
                "-foo",
                "-bar",
                "-script-templates", "temp1,temp2");

        assertThat(args).hasSize(matches.size());

        IntStream.range(0, args.size()).forEach(i -> assertThat(args.get(i)).isEqualTo(matches.get(i)));


    }

    @Test
    void argsTest() {
        var args = new CompileKotlinOptions()
                .apiVersion("11")
                .argFile("file.txt", "file2.txt")
                .classpath("path1", "path2")
                .javaParameters(true)
                .jvmTarget("11")
                .includeRuntime(true)
                .jdkHome("path")
                .jdkRelease("11")
                .kotlinHome("path")
                .languageVersion("1.0")
                .moduleName("module")
                .noJdk(true)
                .noReflect(true)
                .noWarn(true)
                .optIn("opt1", "opt2")
                .options("-foo", "-bar")
                .path("path")
                .plugin("id", "name", "value")
                .progressive(true)
                .scriptTemplates("name", "name2")
                .verbose(true)
                .wError(true)
                .args();

        var matches = List.of(
                "-api-version", "11",
                "@file.txt", "@file2.txt",
                "-classpath", "path1" + File.pathSeparator + "path2",
                "-java-parameters",
                "-jvm-target", "11",
                "-include-runtime",
                "-jdk-home", "path",
                "-Xjdk-release=11",
                "-kotlin-home", "path",
                "-language-version", "1.0",
                "-module-name", "module",
                "-no-jdk",
                "-no-reflect",
                "-no-stdlib",
                "-no-warn",
                "-opt-in", "opt1",
                "-opt-in", "opt2",
                "-foo",
                "-bar",
                "-d", "path",
                "-P", "plugin:id:name:value",
                "-progressive",
                "-script-templates", "name,name2",
                "-verbose",
                "-Werror");

        assertThat(args).hasSize(matches.size());

        IntStream.range(0, args.size()).forEach(i -> assertThat(args.get(i)).isEqualTo(matches.get(i)));
    }
}
