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

package rife.bld.extension;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CompileKotlinOptionsTest {
    @Test
    void argsCollectionTest() {
        var args = new CompileKotlinOptions()
                .advancedOptions(List.of("Xoption1", "Xoption2"))
                .argFile(List.of("arg1.txt", "arg2.txt"))
                .classpath(List.of("path1", "path2"))
                .jvmOptions(List.of("option1", "option2"))
                .noStdLib(false)
                .optIn(List.of("opt1", "opt2"))
                .options(List.of("-foo", "-bar"))
                .scriptTemplates(List.of("temp1", "temp2"))
                .args();
        var matches = List.of(
                "@arg1.txt", "@arg2.txt",
                "-classpath", "path1:path2",
                "-Joption1", "-Joption2",
                "-opt-in", "opt1",
                "-opt-in", "opt2",
                "-foo", "-bar",
                "-script-templates",
                "temp1,temp2",
                "-XXoption1", "-XXoption2");

        for (var arg : args) {
            var found = false;
            for (var match : matches) {
                if (match.equals(arg)) {
                    found = true;
                    break;
                }
            }
            assertThat(found).as(arg).isTrue();
        }
    }


    @Test
    void argsTest() {
        var options = new CompileKotlinOptions()
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
                .wError(true);

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
                "-nowarn",
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

        var args = new ArrayList<List<String>>();
        args.add(options.args());
        args.add(options.apiVersion(11).jvmTarget(11).args());

        for (var a : args) {
            assertThat(a).hasSize(matches.size());
            IntStream.range(0, a.size()).forEach(i -> assertThat(a.get(i)).isEqualTo(matches.get(i)));
        }
    }

    @Test
    void checkAllParamsTest() throws IOException {
        var args = Files.readAllLines(Paths.get("src", "test", "resources", "kotlinc-args.txt"));

        assertThat(args).isNotEmpty();

        var params = new CompileKotlinOptions()
                .advancedOptions("Xoption")
                .argFile("file")
                .classpath("classpath")
                .expression("expression")
                .jvmOptions("option")
                .includeRuntime(true)
                .javaParameters(true)
                .jdkHome("jdkhome")
                .jvmTarget(12)
                .kotlinHome("kotlin")
                .moduleName("moduleName")
                .noJdk(true)
                .noReflect(true)
                .noStdLib(true)
                .noWarn(true)
                .optIn("annotation")
                .options("option")
                .path("path")
                .plugin("id", "option", "value")
                .progressive(true)
                .scriptTemplates("template")
                .verbose(true)
                .wError(true);

        for (var p : args) {
            var found = false;
            for (var a : params.args()) {
                if (a.startsWith(p)) {
                    found = true;
                    break;
                }
            }
            assertThat(found).as(p + " not found.").isTrue();
        }
    }
}
