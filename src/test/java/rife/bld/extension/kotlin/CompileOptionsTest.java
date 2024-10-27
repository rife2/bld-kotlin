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

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
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

        try (var softly = new AutoCloseableSoftAssertions()) {
            for (var a : args) {
                IntStream.range(0, a.size()).forEach(i -> softly.assertThat(a.get(i))
                        .as(a.get(i) + " == " + matches.get(i)).isEqualTo(matches.get(i)));
            }
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

        try (var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(op.advancedOptions()).as("advancedOptions")
                    .hasSize(advanceOptions.size()).containsAll(advanceOptions);
            softly.assertThat(op.argFile()).as("argFile")
                    .hasSize(argFile.size()).containsAll(argFile);
            softly.assertThat(op.classpath()).as("classpath")
                    .hasSize(classpath.size()).containsAll(classpath);
            softly.assertThat(op.jvmOptions()).as("jvmOptions")
                    .hasSize(jvmOptions.size()).containsAll(jvmOptions);
            softly.assertThat(op.optIn()).as("optIn")
                    .hasSize(optIn.size()).containsAll(optIn);
            softly.assertThat(op.options()).as("options")
                    .hasSize(options.size()).containsAll(options);
            softly.assertThat(op.plugin()).as("plugin")
                    .hasSize(plugin.size()).containsAll(plugin);
            softly.assertThat(op.scriptTemplates()).as("scriptTemplates")
                    .hasSize(scriptTemplates.size()).containsAll(scriptTemplates);
        }

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

        try (var softly = new AutoCloseableSoftAssertions()) {
            var args = op.args();
            for (var arg : args) {
                var found = false;
                for (var match : matches) {
                    if (match.equals(arg)) {
                        found = true;
                        break;
                    }
                }
                softly.assertThat(found).as(arg + " not found.").isTrue();
            }
        }
    }

    @Test
    void testArgsFile() {
        var foo = new File("foo.txt");
        var bar = new File("bar.txt");
        var options = new CompileOptions();

        options.argFile(foo, bar);
        assertThat(options.argFile()).contains(foo, bar);
        options.argFile().clear();

        options = options.argFile(foo.toPath(), bar.toPath());
        assertThat(options.argFile()).contains(foo, bar);
        options.argFile().clear();

        options.argFile(foo.getAbsolutePath(), bar.getAbsolutePath());
        assertThat(options.argFile()).contains(new File(foo.getAbsolutePath()), new File(bar.getAbsolutePath()));
        options.argFile().clear();
    }

    @Test
    void testCheckAllParams() throws IOException {
        var args = Files.readAllLines(Paths.get("src", "test", "resources", "kotlinc-args.txt"));

        assertThat(args).isNotEmpty();

        var params = new CompileOptions()
                .advancedOptions("Xoption")
                .apiVersion("11")
                .argFile("file")
                .classpath("classpath")
                .expression("expression")
                .jvmOptions("option")
                .includeRuntime(true)
                .javaParameters(true)
                .jdkHome("jdkhome")
                .jvmTarget(12)
                .kotlinHome("kotlin")
                .languageVersion("1.0")
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

        try (var softly = new AutoCloseableSoftAssertions()) {
            for (var p : args) {
                var found = false;
                for (var a : params.args()) {
                    if (a.startsWith(p)) {
                        found = true;
                        break;
                    }
                }
                softly.assertThat(found).as(p + " not found.").isTrue();
            }
        }
    }

    @Test
    void testClasspath() {
        var foo = new File("foo.txt");
        var bar = new File("bar.txt");
        var options = new CompileOptions();

        options.classpath(foo, bar);
        assertThat(options.classpath()).as("File...").containsExactly(foo, bar);
        options.classpath().clear();

        options.classpath(List.of(foo, bar));
        assertThat(options.classpath()).as("List(File...)").containsExactly(foo, bar);
        options.classpath().clear();

        options = options.classpath(foo.toPath(), bar.toPath());
        assertThat(options.classpath()).as("Path...").containsExactly(foo, bar);
        options.classpath().clear();

        options = options.classpathPaths(List.of(foo.toPath(), bar.toPath()));
        assertThat(options.classpath()).as("List(Path...)").containsExactly(foo, bar);
        options.classpath().clear();

        options.classpath(foo.getAbsolutePath(), bar.getAbsolutePath());
        assertThat(options.classpath()).as("String...")
                .containsExactly(new File(foo.getAbsolutePath()), new File(bar.getAbsolutePath()));
        options.classpath().clear();

        options.classpathStrings(List.of(foo.getAbsolutePath(), bar.getAbsolutePath()));
        assertThat(options.classpath()).as("List(String...)")
                .containsExactly(new File(foo.getAbsolutePath()), new File(bar.getAbsolutePath()));
        options.classpath().clear();
    }

    @Test
    void testJdkHome() {
        var foo = new File("foo.txt");
        var options = new CompileOptions();

        options.jdkHome(foo);
        assertThat(options.jdkHome()).isEqualTo(foo);

        options = options.jdkHome(foo.toPath());
        assertThat(options.jdkHome()).isEqualTo(foo);

        options.jdkHome(foo.getAbsolutePath());
        assertThat(options.jdkHome().getAbsolutePath()).isEqualTo(foo.getAbsolutePath());
    }

    @Test
    void testKotlinHome() {
        var foo = new File("foo.txt");
        var options = new CompileOptions();

        options.kotlinHome(foo);
        assertThat(options.kotlinHome()).isEqualTo(foo);

        options = options.kotlinHome(foo.toPath());
        assertThat(options.kotlinHome()).isEqualTo(foo);

        options.kotlinHome(foo.getAbsolutePath());
        assertThat(options.kotlinHome().getAbsolutePath()).isEqualTo(foo.getAbsolutePath());
    }

    @Test
    void testOptions() {
        var options = new CompileOptions()
                .advancedOptions("xopt1", "xopt2")
                .apiVersion("11")
                .argFile(Path.of("args.txt"))
                .classpath("classpath")
                .expression("expression")
                .includeRuntime(true)
                .javaParameters(true)
                .jdkHome("jdk-home")
                .jdkRelease(22)
                .jvmTarget("9")
                .kotlinHome("kotlin-home")
                .languageVersion("1.0")
                .moduleName("module")
                .noJdk(true)
                .noReflect(true)
                .noStdLib(true)
                .noWarn(true)
                .optIn("opt1", "opt2")
                .options("-foo", "-bar")
                .path(Path.of("path"))
                .plugin("id", "name", "value")
                .progressive(true)
                .scriptTemplates("name", "name2")
                .verbose(true)
                .wError(true);

        try (var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(options.advancedOptions()).containsExactly("xopt1", "xopt2");
            softly.assertThat(options.apiVersion()).isEqualTo("11");
            softly.assertThat(options.argFile()).containsExactly(new File("args.txt"));
            softly.assertThat(options.classpath()).containsExactly(new File("classpath"));
            softly.assertThat(options.expression()).isEqualTo("expression");
            softly.assertThat(options.isIncludeRuntime()).isTrue();
            softly.assertThat(options.isJavaParameters()).isTrue();
            softly.assertThat(options.isNoJdk()).isTrue();
            softly.assertThat(options.isNoReflect()).isTrue();
            softly.assertThat(options.isNoStdLib()).isTrue();
            softly.assertThat(options.isNoWarn()).isTrue();
            softly.assertThat(options.isProgressive()).isTrue();
            softly.assertThat(options.isVerbose()).isTrue();
            softly.assertThat(options.jdkHome()).isEqualTo(new File("jdk-home"));
            softly.assertThat(options.jdkRelease()).isEqualTo("22");
            softly.assertThat(options.jvmTarget()).isEqualTo("9");
            softly.assertThat(options.kotlinHome()).isEqualTo(new File("kotlin-home"));
            softly.assertThat(options.languageVersion()).isEqualTo("1.0");
            softly.assertThat(options.moduleName()).isEqualTo("module");
            softly.assertThat(options.optIn()).containsExactly("opt1", "opt2");
            softly.assertThat(options.options()).containsExactly("-foo", "-bar");
            softly.assertThat(options.path()).isEqualTo(new File("path"));
            softly.assertThat(options.plugin()).containsExactly("id:name:value");
            softly.assertThat(options.scriptTemplates()).containsExactly("name", "name2");
            softly.assertThat(options.isWError()).isTrue();
        }
    }
}
