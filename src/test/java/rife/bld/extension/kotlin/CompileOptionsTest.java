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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CompileOptionsTest {
    /**
     * Returns the local path of the given file names.
     *
     * @param fileNames The file names
     * @return the local path
     */
    private String localPath(String... fileNames) {
        return Arrays.stream(fileNames).map(it -> new File(it).getAbsolutePath())
                .collect(Collectors.joining(File.pathSeparator));
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void testArgs() {
        var options = new CompileOptions()
                .apiVersion("11")
                .argFile(new File("file.txt"), new File("file2.txt"))
                .classpath(new File("path1"), new File("path2"))
                .javaParameters(true)
                .jvmTarget("11")
                .includeRuntime(true)
                .jdkHome(new File("path"))
                .jdkRelease("11")
                .kotlinHome(new File("path"))
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
                "@" + localPath("file.txt"), "@" + localPath("file2.txt"),
                "-classpath", localPath("path1", "path2"),
                "-java-parameters",
                "-jvm-target", "11",
                "-include-runtime",
                "-jdk-home", localPath("path"),
                "-Xjdk-release=11",
                "-kotlin-home", localPath("path"),
                "-language-version", "1.0",
                "-module-name", "module",
                "-no-jdk",
                "-no-reflect",
                "-nowarn",
                "-opt-in", "opt1",
                "-opt-in", "opt2",
                "-foo",
                "-bar",
                "-d", localPath("path"),
                "-P", "plugin:id:name:value",
                "-progressive",
                "-script-templates", "name,name2",
                "-verbose",
                "-Werror");

        var args = new ArrayList<List<String>>();
        args.add(options.args());
        args.add(options.apiVersion(11).jvmTarget(11).args());

        for (var a : args) {
            IntStream.range(0, a.size()).forEach(i -> assertThat(a.get(i)).isEqualTo(matches.get(i)));
        }
    }

    @Test
    void testArgsCollections() {
        var advanceOptions = List.of("Xoption1", "Xoption2");
        var argFile = List.of(new File("arg1.txt"), new File("arg2.txt"));
        var classpath = List.of(new File("path1"), new File("path2"));
        var jvmOptions = List.of("option1", "option2");
        var optIn = List.of("opt1", "opt2");
        var options = List.of("-foo", "-bar");
        var plugin = List.of("id:name:value", "id2:name2:value2");
        var scriptTemplates = List.of("temp1", "temp2");

        var op = new CompileOptions()
                .advancedOptions(advanceOptions)
                .argFile(argFile)
                .classpath(classpath)
                .jvmOptions(jvmOptions)
                .noStdLib(false)
                .optIn(optIn)
                .options(options)
                .scriptTemplates(scriptTemplates);

        plugin.forEach(it -> {
            var p = it.split(":");
            op.plugin(p[0], p[1], p[2]);
        });

        assertThat(op.advancedOptions()).as("advancedOptions")
                .hasSize(advanceOptions.size()).containsAll(advanceOptions);
        assertThat(op.argFile()).as("argFile")
                .hasSize(argFile.size()).containsAll(argFile);
        assertThat(op.classpath()).as("classpath")
                .hasSize(classpath.size()).containsAll(classpath);
        assertThat(op.jvmOptions()).as("jvmOptions")
                .hasSize(jvmOptions.size()).containsAll(jvmOptions);
        assertThat(op.optIn()).as("optIn")
                .hasSize(optIn.size()).containsAll(optIn);
        assertThat(op.options()).as("options")
                .hasSize(options.size()).containsAll(options);
        assertThat(op.plugin()).as("plugin")
                .hasSize(plugin.size()).containsAll(plugin);
        assertThat(op.scriptTemplates()).as("scriptTemplates")
                .hasSize(scriptTemplates.size()).containsAll(scriptTemplates);

        var matches = List.of(
                '@' + localPath("arg1.txt"), '@' + localPath("arg2.txt"),
                "-classpath", localPath("path1", "path2"),
                "-Joption1", "-Joption2",
                "-opt-in", "opt1",
                "-opt-in", "opt2",
                "-foo", "-bar",
                "-script-templates",
                "temp1,temp2",
                "-XXoption1", "-XXoption2",
                "-P", "plugin:id:name:value",
                "-P", "plugin:id2:name2:value2");

        var args = op.args();
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
    void testCheckAllParams() throws IOException {
        var args = Files.readAllLines(Paths.get("src", "test", "resources", "kotlinc-args.txt"));

        assertThat(args).isNotEmpty();

        var params = new CompileOptions()
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
                .path(new File("path"))
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
