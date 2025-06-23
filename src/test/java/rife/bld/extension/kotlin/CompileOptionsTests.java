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

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

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
class CompileOptionsTests {
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

    @Nested
    @DisplayName("Args File Tests")
    class ArgsFileTests {
        private final File bar = new File("bar.txt");
        private final File foo = new File("foo.txt");
        private final CompileOptions options = new CompileOptions();

        @Test
        void argsFileAsFileArray() {
            var options = new CompileOptions();
            options = options.argFile(foo, bar);
            assertThat(options.argFile()).contains(foo, bar);
        }

        @Test
        void argsFileAsFileList() {
            options.argFile().clear();
            options.argFile(List.of(foo, bar));
            assertThat(options.argFile()).contains(foo, bar);
        }

        @Test
        void argsFileAsPathArray() {
            options.argFile().clear();
            options.argFile(foo.toPath(), bar.toPath());
            assertThat(options.argFile()).contains(foo, bar);
        }

        @Test
        void argsFileAsPathList() {
            options.argFile().clear();
            options.argFilePaths(List.of(foo.toPath(), bar.toPath()));
            assertThat(options.argFile()).contains(foo, bar);
        }

        @Test
        void argsFileAsStringArray() {
            var options = new CompileOptions();
            options = options.argFile(foo.getAbsolutePath(), bar.getAbsolutePath());
            assertThat(options.argFile()).contains(foo.getAbsoluteFile(), bar.getAbsoluteFile());
        }

        @Test
        void argsFileAsStringList() {
            options.argFileStrings(List.of(foo.getAbsolutePath(), bar.getAbsolutePath()));
            assertThat(options.argFile()).contains(foo.getAbsoluteFile(), bar.getAbsoluteFile());
        }
    }

    @Nested
    @DisplayName("Args Tests")
    class ArgsTests {
        @Test
        @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
        void args() {
            var options = new CompileOptions()
                    .apiVersion("11")
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
                    .wError(true)
                    .wExtra(true);

            var matches = List.of(
                    "-api-version", "11",
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
                    "-Werror",
                    "-Wextra");

            var args = new ArrayList<List<String>>();
            args.add(options.args());
            args.add(options.apiVersion(11).jvmTarget(11).args());

            try (var softly = new AutoCloseableSoftAssertions()) {
                for (var a : args) {
                    IntStream.range(0, a.size()).forEach(i -> softly.assertThat(a.get(i))
                            .as("%s == %s", a.get(i), matches.get(i)).isEqualTo(matches.get(i)));
                }
            }
        }

        @Test
        void argsCollections() {
            var advanceOptions = List.of("-Xoption1", "option=2");
            var argFile = List.of(new File("arg1.txt"), new File("arg2.txt"));
            var classpath = List.of(new File("path1"), new File("path2"));
            var optIn = List.of("opt1", "opt2");
            var options = List.of("-foo", "-bar");
            var plugin = List.of("id:name:value", "id2:name2:value2");
            var scriptTemplates = List.of("temp1", "temp2");

            var op = new CompileOptions()
                    .advancedOptions(advanceOptions)
                    .argFile(argFile)
                    .classpath(classpath)
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
                    "-Xoption1", "-Xoption=2",
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
                    softly.assertThat(found).as("%s not found.", arg).isTrue();
                }
            }
        }
    }

    @Nested
    @DisplayName("Classpath Tests")
    class ClasspathTests {
        private final File bar = new File("bar.txt");
        private final File foo = new File("foo.txt");
        private final CompileOptions options = new CompileOptions();

        @Test
        void classpathAsFile() {
            var options = new CompileOptions();
            options = options.classpath(foo);
            assertThat(options.classpath()).containsExactly(foo);
        }

        @Test
        void classpathAsFileArray() {
            options.classpath().clear();
            options.classpath(foo, bar);
            assertThat(options.classpath()).containsExactly(foo, bar);
        }

        @Test
        void classpathAsFileList() {
            options.classpath().clear();
            options.classpath(List.of(foo, bar));
            assertThat(options.classpath()).containsExactly(foo, bar);
        }

        @Test
        void classpathAsPathArray() {
            var options = new CompileOptions();
            options = options.classpath(foo.toPath(), bar.toPath());
            assertThat(options.classpath()).containsExactly(foo, bar);
        }

        @Test
        void classpathAsPathList() {
            options.classpath().clear();
            options.classpathPaths(List.of(foo.toPath(), bar.toPath()));
            assertThat(options.classpath()).containsExactly(foo, bar);
        }

        @Test
        void classpathAsStringArray() {
            options.classpath().clear();
            options.classpath(foo.getAbsolutePath(), bar.getAbsolutePath());
            assertThat(options.classpath())
                    .containsExactly(new File(foo.getAbsolutePath()), new File(bar.getAbsolutePath()));
        }

        @Test
        void classpathAsStringList() {
            options.classpath().clear();
            options.classpathStrings(List.of(foo.getAbsolutePath(), bar.getAbsolutePath()));
            assertThat(options.classpath())
                    .containsExactly(new File(foo.getAbsolutePath()), new File(bar.getAbsolutePath()));
        }
    }

    @Nested
    @DisplayName("JDK Home Tests")
    class JdkHomeTests {
        private final File foo = new File("foo.txt");
        private final CompileOptions options = new CompileOptions();

        @Test
        void jdkHomeAsFile() {
            options.jdkHome(foo);
            assertThat(options.jdkHome()).isEqualTo(foo);
        }

        @Test
        void jdkHomeAsPath() {
            var options = new CompileOptions();
            options = options.jdkHome(foo.toPath());
            assertThat(options.jdkHome()).isEqualTo(foo);
        }

        @Test
        void jdkHomeAsString() {
            options.jdkHome(foo.getAbsolutePath());
            assertThat(options.jdkHome().getAbsolutePath()).isEqualTo(foo.getAbsolutePath());
        }
    }

    @Nested
    @DisplayName("Kotlin Home Tests")
    class KotlinHomeTests {
        private final File foo = new File("foo.txt");

        @Test
        void kotlinHomeAsFile() {
            var options = new CompileOptions().kotlinHome(foo);
            assertThat(options.kotlinHome()).isEqualTo(foo);
        }

        @Test
        void kotlinHomeAsPath() {
            var options = new CompileOptions().kotlinHome(foo.toPath());
            assertThat(options.kotlinHome()).isEqualTo(foo);
        }

        @Test
        void kotlinHomeAsString() {
            var options = new CompileOptions().kotlinHome(foo.getAbsolutePath());
            assertThat(options.kotlinHome().getAbsolutePath()).isEqualTo(foo.getAbsolutePath());
        }
    }

    @Nested
    @DisplayName("Options Tests")
    class OptionsTests {
        @Test
        @EnabledOnOs(OS.LINUX)
        void checkAllParams() throws IOException {
            var args = Files.readAllLines(Paths.get("src", "test", "resources", "kotlinc-args.txt"));

            assertThat(args).isNotEmpty();

            var params = new CompileOptions()
                    .advancedOptions("Xoption")
                    .apiVersion("11")
                    .expression("expression")
                    .includeRuntime(true)
                    .javaParameters(true)
                    .jdkHome("jdkhome")
                    .jvmDefault(JvmDefault.ENABLE)
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
                    .wError(true)
                    .wExtra(true);

            var skipArgs = List.of("-J", "-classpath", "@");
            assertThat(args).as("%s not found.", skipArgs).containsAll(skipArgs);
            args.removeAll(skipArgs);

            try (var softly = new AutoCloseableSoftAssertions()) {
                for (var p : args) {
                    var found = false;
                    for (var a : params.args()) {
                        if (a.startsWith(p)) {
                            found = true;
                            break;
                        }
                    }
                    softly.assertThat(found).as("%s not found.", p).isTrue();
                }
            }
        }

        @Test
        void options() {
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
                    .jvmDefault(JvmDefault.NO_COMPATIBILITY)
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
                    .wError(true)
                    .wExtra(true);

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
                softly.assertThat(options.jvmDefault()).isEqualTo(JvmDefault.NO_COMPATIBILITY);
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
                softly.assertThat(options.isWExtra()).isTrue();
            }
        }
    }
}
