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

package rife.bld.extension;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import rife.bld.blueprints.BaseProjectBlueprint;
import rife.bld.extension.kotlin.CompileOptions;
import rife.bld.extension.kotlin.CompilerPlugin;
import rife.bld.extension.kotlin.JvmOptions;
import rife.bld.extension.testing.LoggingExtension;
import rife.bld.extension.testing.RandomString;
import rife.bld.extension.testing.RandomStringResolver;
import rife.bld.extension.tools.IOUtils;
import rife.bld.extension.tools.SystemUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LoggingExtension.class)
@ExtendWith(RandomStringResolver.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CompileKotlinOperationTests {

    private static final String BAR = "bar";
    private static final String FILE_1 = "file1";
    private static final String FILE_2 = "file2";
    private static final String FOO = "foo";

    @RegisterExtension
    @SuppressWarnings({"unused"})
    private static final LoggingExtension LOGGING_EXTENSION =
            new LoggingExtension(CompileKotlinOperation.class.getName());

    private static final String PROJECT = "examples";
    private static final String PROJECT_NAME = "Example";
    private static final String PROJECT_PACKAGE = "com.example";
    @TempDir
    private File tmpDir;

    @Test
    void execute() throws Exception {
        var buildDir = new File(tmpDir, "build");
        var mainDir = new File(buildDir, "main");
        var testDir = new File(buildDir, "test");

        try (var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(mainDir.mkdirs()).as("make mainDir").isTrue();
            softly.assertThat(testDir.mkdirs()).as("make testDir").isTrue();
        }

        var compileJars = new ArrayList<String>();
        for (var f : Objects.requireNonNull(new File("examples/lib/compile").listFiles())) {
            compileJars.add(f.getAbsolutePath());
        }

        var testJars = new ArrayList<String>();
        for (var f : Objects.requireNonNull(new File("examples/lib/test").listFiles())) {
            testJars.add(f.getAbsolutePath());
        }

        var op = new CompileKotlinOperation()
                .fromProject(new BaseProjectBlueprint(new File(PROJECT), PROJECT_PACKAGE, PROJECT_NAME, PROJECT_NAME))
                .buildMainDirectory(mainDir)
                .buildTestDirectory(testDir)
                .compileMainClasspath(compileJars)
                .compileTestClasspath(testJars)
                .compileTestClasspath(compileJars)
                .compileTestClasspath(mainDir.getAbsolutePath());

        op.compileOptions().verbose(true);
        op.compileOptions().argFile("src/test/resources/argfile.txt", "src/test/resources/argfile2.txt");

        if (!SystemUtils.isWindows()) {
            op.jvmOptions().enableNativeAccess(JvmOptions.ALL_UNNAMED);
            assertThat(op.jvmOptions()).containsExactly("--enable-native-access=ALL-UNNAMED");
        }

        var args = op.compileOptions().args();
        var matches = List.of("-Xjdk-release=17", "-no-reflect", "-progressive", "-include-runtime", "-no-stdlib",
                "-verbose");
        assertThat(args).as("%s == %s", args, matches).isEqualTo(matches);

        op.execute();

        try (var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(tmpDir).as("tmpDir shouldn't be empty").isNotEmptyDirectory();
            softly.assertThat(mainDir).as("mainDir shouldn't be empty").isNotEmptyDirectory();
            softly.assertThat(testDir).as("testDir shouldn't be empty").isNotEmptyDirectory();
        }

        var mainOut = IOUtils.resolveFile(mainDir, "com", "example");
        try (var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(new File(mainOut, "Example.class")).as("Example.class").exists();
            softly.assertThat(new File(mainOut, "Example$Companion.class"))
                    .as("ExampleCompanion.class").exists();
        }

        var testOut = IOUtils.resolveFile(testDir, "com", "example");
        assertThat(new File(testOut, "ExampleTest.class")).as("ExampleTest.class").exists();

    }

    @Nested
    @DisplayName("Options Test")
    class OptionsTest {

        private static final String KOTLINC = "kotlinc";
        private static final String LIB_COMPILE = "lib/compile";

        @Test
        void collections() {
            var op = new CompileKotlinOperation()
                    .fromProject(new BaseProjectBlueprint(new File(PROJECT), PROJECT_PACKAGE, PROJECT_NAME, PROJECT_NAME))
                    .kotlinHome("/kotlin_home")
                    .kotlinc(KOTLINC)
                    .workDir("work_dir")
                    .compileMainClasspath("path1", "path2")
                    .compileOptions(new CompileOptions()
                            .jdkRelease("17")
                            .jvmTarget("17")
                            .verbose(true))
                    .mainSourceDirectories("dir1", "dir2")
                    .mainSourceDirectories(List.of(new File("dir3"), new File("dir4")))
                    .mainSourceFiles("file1", "file2")
                    .mainSourceFiles(List.of(new File("file3"), new File("file4")))
                    .mainSourceFiles(new File("file5"), new File("file6"))
                    .testSourceDirectories("tdir1", "tdir2")
                    .testSourceDirectories(List.of(new File("tdir3"), new File("tdir4")))
                    .testSourceFiles("tfile1", "tfile2")
                    .testSourceFiles(List.of(new File("tfile3"), new File("tfile4")))
                    .testSourceFiles(new File("tfile5"), new File("tfile6"))
                    .plugins("plugin1", "plugin2")
                    .plugins(CompilerPlugin.KOTLIN_SERIALIZATION, CompilerPlugin.ASSIGNMENT, CompilerPlugin.COMPOSE)
                    .plugins(new File(LIB_COMPILE), CompilerPlugin.LOMBOK, CompilerPlugin.POWER_ASSERT)
                    .plugins(Path.of(LIB_COMPILE), CompilerPlugin.NOARG, CompilerPlugin.ALL_OPEN,
                            CompilerPlugin.KOTLIN_IMPORTS_DUMPER)
                    .plugins(LIB_COMPILE, CompilerPlugin.KOTLINX_SERIALIZATION, CompilerPlugin.SAM_WITH_RECEIVER)
                    .plugins(List.of("plugin3", "plugin4"));

            try (var softly = new AutoCloseableSoftAssertions()) {
                softly.assertThat(op.kotlinHome().getName()).as("kotlin_home").isEqualTo("kotlin_home");
                softly.assertThat(op.kotlinc().getName()).as(KOTLINC).isEqualTo(KOTLINC);
                softly.assertThat(op.workDir().getName()).as("work_dir").isEqualTo("work_dir");
                softly.assertThat(op.compileMainClasspath()).as("compileMainClassPath")
                        .containsAll(List.of("path1", "path2"));
                softly.assertThat(op.compileOptions().hasRelease()).as("hasRelease").isTrue();
                softly.assertThat(op.compileOptions().hasTarget()).as("hasTaget").isTrue();
                softly.assertThat(op.compileOptions().isVerbose()).as("isVerbose").isTrue();
                softly.assertThat(op.mainSourceDirectories()).as("mainSourceDirectories").containsExactly(
                        Path.of(PROJECT, "src", "main", "kotlin").toFile(), new File("dir1"),
                        new File("dir2"), new File("dir3"), new File("dir4"));
                softly.assertThat(op.testSourceDirectories()).as("testSourceDirectories").containsOnly(
                        Path.of(PROJECT, "src", "test", "kotlin").toFile(), new File("tdir1"),
                        new File("tdir2"), new File("tdir3"), new File("tdir4"));
                softly.assertThat(op.mainSourceFiles()).as("mainSourceFiles").containsOnly(
                        new File("file1"), new File("file2"), new File("file3"),
                        new File("file4"), new File("file5"), new File("file6"));
                softly.assertThat(op.testSourceFiles()).as("testSourceFiles").containsOnly(
                        new File("tfile1"), new File("tfile2"), new File("tfile3"),
                        new File("tfile4"), new File("tfile5"), new File("tfile6"));
                softly.assertThat(op.plugins()).as("plugins").contains(
                        "plugin1", "plugin2", "plugin3", "plugin4",
                        "KOTLIN_SERIALIZATION", "ASSIGNMENT", "COMPOSE",
                        new File(LIB_COMPILE, "lombok-compiler-plugin.jar").getAbsolutePath(),
                        new File(LIB_COMPILE, "power-assert-compiler-plugin.jar").getAbsolutePath(),
                        new File(LIB_COMPILE, "noarg-compiler-plugin.jar").getAbsolutePath(),
                        new File(LIB_COMPILE, "allopen-compiler-plugin.jar").getAbsolutePath(),
                        new File(LIB_COMPILE, "kotlin-imports-dumper-compiler-plugin.jar").getAbsolutePath(),
                        new File(LIB_COMPILE, "kotlinx-serialization-compiler-plugin.jar").getAbsolutePath(),
                        new File(LIB_COMPILE, "sam-with-receiver-compiler-plugin.jar").getAbsolutePath()
                );
            }
        }

        @Nested
        @DisplayName("Build Directory Tests")
        class BuildDirectoryTests {

            private final File bar = new File(BAR);
            private final File foo = new File(FOO);
            private CompileKotlinOperation op = new CompileKotlinOperation();

            @Test
            void buildMainDirectoryAsFile() {
                op.buildMainDirectory(foo);
                assertThat(op.buildMainDirectory()).isEqualTo(foo);
            }

            @Test
            void buildMainDirectoryAsPath() {
                op = op.buildMainDirectory(bar.toPath());
                assertThat(op.buildMainDirectory()).isEqualTo(bar);
            }

            @Test
            void buildMainDirectoryAsString() {
                op = new CompileKotlinOperation().buildMainDirectory(FOO);
                assertThat(op.buildMainDirectory()).isEqualTo(foo);
            }

            @Test
            void buildTestDirectoryAsFile() {
                op.buildTestDirectory(foo);
                assertThat(op.buildTestDirectory()).isEqualTo(foo);
            }

            @Test
            void buildTestDirectoryAsPath() {
                op = op.buildTestDirectory(bar.toPath());
                assertThat(op.buildTestDirectory()).isEqualTo(bar);
            }

            @Test
            void buildTestDirectoryAsString() {
                op = new CompileKotlinOperation().buildTestDirectory(FOO);
                assertThat(op.buildTestDirectory()).isEqualTo(foo);
            }
        }

        @Nested
        @DisplayName("From Project Tests")
        class FromProjectTests {

            @Test
            void fromProject() {
                var examples = new File(PROJECT);
                var op = new CompileKotlinOperation().fromProject(
                        new BaseProjectBlueprint(examples, PROJECT_PACKAGE, PROJECT, PROJECT));

                assertThat(op.workDir().getPath()).as("workDir")
                        .isEqualTo(new File("examples").getAbsolutePath());
                assertThat(op.buildMainDirectory()).as("buildMainDirectory").exists();
                assertThat(op.buildTestDirectory()).as("buildTestDirectory").exists();
                assertThat(op.mainSourceDirectories()).as("mainSourceDirectories")
                        .containsExactly(new File(examples, "src/main/kotlin"));
                assertThat(op.testSourceDirectories()).as("testSourceDirectories")
                        .containsExactly(new File(examples, "src/test/kotlin"));
                assertThat(op.compileMainClasspath().size()).as("compileMainClasspath").isGreaterThan(1);
                assertThat(op.compileTestClasspath().size()).as("compileTestClasspath").isGreaterThan(1);
                assertThat(op.compileOptions().isNoStdLib()).as("isNoStdLib").isTrue();

            }

            @Test
            void fromProjectWithoutKotlin() {
                var op = new CompileKotlinOperation().fromProject(
                        new BaseProjectBlueprint(new File(FOO), "org.example", FOO, FOO));

                assertThat(op.workDir()).as("workDir").doesNotExist();
                assertThat(op.mainSourceDirectories()).as("mainSourceDirectories").isEmpty();
                assertThat(op.testSourceDirectories()).as("testSourceDirectories").isEmpty();
                assertThat(op.compileMainClasspath()).as("compileMainClasspath").isEmpty();
                assertThat(op.compileTestClasspath().size()).as("compileTestClasspath").isEqualTo(1);
                assertThat(op.compileOptions().isNoStdLib()).as("isNoStdLib").isTrue();
            }
        }

        @Nested
        @DisplayName("Kotlin Path Tests")
        class KotlinPathTests {

            private final File bar = new File(BAR);
            private final File foo = new File(FOO);
            private CompileKotlinOperation op = new CompileKotlinOperation();

            @Test
            void findKotlincPath() {
                assertThat(CompileKotlinOperation.findKotlincPath()).doesNotStartWith(KOTLINC);
            }

            @Nested
            @DisplayName("Kotlin Home Tests")
            class KotlinHomeTests {

                @Test
                void kotlinHomeAsFile() {
                    op.kotlinHome(foo);
                    assertThat(op.kotlinHome()).isEqualTo(foo);
                }

                @Test
                void kotlinHomeAsPath() {
                    op = op.kotlinHome(bar.toPath());
                    assertThat(op.kotlinHome()).isEqualTo(bar);
                }

                @Test
                void kotlinHomeAsString() {
                    op = new CompileKotlinOperation().kotlinHome(FOO);
                    assertThat(op.kotlinHome()).isEqualTo(foo);
                }
            }

            @Nested
            @DisplayName("Kotlinc Tests")
            class KotlincTests {

                @Test
                void kotlincAsFile() {
                    op.kotlinc(foo);
                    assertThat(op.kotlinc()).isEqualTo(foo);
                }

                @Test
                void kotlincAsPath() {
                    op = op.kotlinc(bar.toPath());
                    assertThat(op.kotlinc()).isEqualTo(bar);
                }

                @Test
                void kotlincAsString() {
                    op = new CompileKotlinOperation().kotlinc(FOO);
                    assertThat(op.kotlinc()).isEqualTo(foo);
                }
            }
        }

        @Nested
        @DisplayName("Main Source Directories Tests")
        class MainSourceDirectoriesTests {

            @Test
            void mainSourceDirectoriesAsFileArray() {
                var op = new CompileKotlinOperation();
                op = op.mainSourceDirectories(new File(FILE_1), new File(FILE_2));
                assertThat(op.mainSourceDirectories()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void mainSourceDirectoriesAsFileList() {
                var op = new CompileKotlinOperation();
                op.mainSourceDirectories(List.of(new File(FILE_1), new File(FILE_2)));
                assertThat(op.mainSourceDirectories()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void mainSourceDirectoriesAsPathArray() {
                var op = new CompileKotlinOperation();
                op = op.mainSourceDirectories(Path.of(FILE_1), Path.of(FILE_2));
                assertThat(op.mainSourceDirectories()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void mainSourceDirectoriesAsPathList() {
                var op = new CompileKotlinOperation();
                op.mainSourceDirectoriesPaths(List.of(new File(FILE_1).toPath(), new File(FILE_2).toPath()));
                assertThat(op.mainSourceDirectories()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void mainSourceDirectoriesAsStringArray() {
                var op = new CompileKotlinOperation();
                op.mainSourceDirectories(FILE_1, FILE_2);
                assertThat(op.mainSourceDirectories()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void mainSourceDirectoriesAsStringList() {
                var op = new CompileKotlinOperation();
                op.mainSourceDirectoriesStrings(List.of(FILE_1, FILE_2));
                assertThat(op.mainSourceDirectories()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Nested
            @DisplayName("Main Source Files Tests")
            class MainSourceFilesTests {

                @Test
                void mainSourceFilesAsFileArray() {
                    var op = new CompileKotlinOperation();
                    op.mainSourceFiles(new File(FILE_1), new File(FILE_2));
                    assertThat(op.mainSourceFiles()).containsExactly(new File(FILE_1), new File(FILE_2));
                }

                @Test
                void mainSourceFilesAsFileList() {
                    var op = new CompileKotlinOperation();
                    op.mainSourceFiles(List.of(new File(FILE_1), new File(FILE_2)));
                    assertThat(op.mainSourceFiles()).containsExactly(new File(FILE_1), new File(FILE_2));
                }

                @Test
                void mainSourceFilesAsPathArray() {
                    var op = new CompileKotlinOperation();
                    op = op.mainSourceFiles(Path.of(FILE_1), Path.of(FILE_2));
                    assertThat(op.mainSourceFiles()).containsExactly(new File(FILE_1), new File(FILE_2));
                }

                @Test
                void mainSourceFilesAsPathList() {
                    var op = new CompileKotlinOperation();
                    op.mainSourceFilesPaths(List.of(new File(FILE_1).toPath(), new File(FILE_2).toPath()));
                    assertThat(op.mainSourceFiles()).containsExactly(new File(FILE_1), new File(FILE_2));
                }

                @Test
                void mainSourceFilesAsStringArray() {
                    var op = new CompileKotlinOperation();
                    op.mainSourceFiles(FILE_1, FILE_2);
                    assertThat(op.mainSourceFiles()).containsExactly(new File(FILE_1), new File(FILE_2));
                }

                @Test
                void mainSourceFilesAsStringList() {
                    var op = new CompileKotlinOperation();
                    op.mainSourceFilesStrings(List.of(FILE_1, FILE_2));
                    assertThat(op.mainSourceFiles()).containsExactly(new File(FILE_1), new File(FILE_2));
                }
            }
        }

        @Nested
        @DisplayName("Plugins Tests")
        class PluginsTests {

            @Test
            void pluginsAsCompilerPluginArray() {
                var op = new CompileKotlinOperation();
                op.plugins(CompilerPlugin.LOMBOK, CompilerPlugin.ALL_OPEN);
                assertThat(op.plugins()).containsExactly("LOMBOK", "ALL_OPEN");
            }

            @Test
            void pluginsAsCompilerPluginArrayWithDirectory() {
                var op = new CompileKotlinOperation();
                op.plugins(new File(LIB_COMPILE), CompilerPlugin.LOMBOK, CompilerPlugin.ALL_OPEN);
                assertThat(op.plugins()).containsExactly(
                        new File(LIB_COMPILE, "lombok-compiler-plugin.jar").getAbsolutePath(),
                        new File(LIB_COMPILE, "allopen-compiler-plugin.jar").getAbsolutePath());
            }

            @Test
            void pluginsAsCompilerPluginArrayWithDirectoryPath() {
                var op = new CompileKotlinOperation();
                op.plugins(Path.of(LIB_COMPILE), CompilerPlugin.LOMBOK, CompilerPlugin.ALL_OPEN);
                assertThat(op.plugins()).containsExactly(
                        new File(LIB_COMPILE, "lombok-compiler-plugin.jar").getAbsolutePath(),
                        new File(LIB_COMPILE, "allopen-compiler-plugin.jar").getAbsolutePath());
            }

            @Test
            void pluginsAsCompilerPluginArrayWithDirectoryString() {
                var op = new CompileKotlinOperation();
                op.plugins(LIB_COMPILE, CompilerPlugin.LOMBOK, CompilerPlugin.ALL_OPEN);
                assertThat(op.plugins()).containsExactly(
                        new File(LIB_COMPILE, "lombok-compiler-plugin.jar").getAbsolutePath(),
                        new File(LIB_COMPILE, "allopen-compiler-plugin.jar").getAbsolutePath());
            }

            @Test
            void pluginsAsStringArray() {
                var op = new CompileKotlinOperation();
                op.plugins("plugin1", "plugin2");
                assertThat(op.plugins()).containsExactly("plugin1", "plugin2");
            }

            @Test
            @RandomString(size = 2)
            void pluginsAsStringList(List<String> plugins) {
                var op = new CompileKotlinOperation();
                op.plugins(plugins);
                assertThat(op.plugins()).containsExactly(plugins.toArray(new String[0]));
            }
        }

        @Nested
        @DisplayName("Source Test")
        class SourceDirectoriesTests {

            @Test
            void testSourceDirectoriesAsFileArray() {
                var op = new CompileKotlinOperation();
                op = op.testSourceDirectories(new File(FILE_1), new File(FILE_2));
                assertThat(op.testSourceDirectories()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void testSourceDirectoriesAsFileList() {
                var op = new CompileKotlinOperation();
                op.testSourceDirectories(List.of(new File(FILE_1), new File(FILE_2)));
                assertThat(op.testSourceDirectories()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void testSourceDirectoriesAsListString() {
                var op = new CompileKotlinOperation();
                op.testSourceDirectoriesStrings(List.of(FILE_1, FILE_2));
                assertThat(op.testSourceDirectories()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void testSourceDirectoriesAsPathArray() {
                var op = new CompileKotlinOperation();
                op = op.testSourceDirectories(Path.of(FILE_1), Path.of(FILE_2));
                assertThat(op.testSourceDirectories()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void testSourceDirectoriesAsPathList() {
                var op = new CompileKotlinOperation();
                op.testSourceDirectoriesPaths(List.of(new File(FILE_1).toPath(), new File(FILE_2).toPath()));
                assertThat(op.testSourceDirectories()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void testSourceDirectoriesAsStringArray() {
                var op = new CompileKotlinOperation();
                op.testSourceDirectories(FILE_1, FILE_2);
                assertThat(op.testSourceDirectories()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Nested
            @DisplayName("Source Files Tests")
            class SourceFilesTests {

                @Test
                void testSourceFilesAsFileArray() {
                    var op = new CompileKotlinOperation();
                    op.testSourceFiles(new File(FILE_1), new File(FILE_2));
                    assertThat(op.testSourceFiles()).containsExactly(new File(FILE_1), new File(FILE_2));
                }

                @Test
                void testSourceFilesAsFileList() {
                    var op = new CompileKotlinOperation();
                    op.testSourceFiles(List.of(new File(FILE_1), new File(FILE_2)));
                    assertThat(op.testSourceFiles()).containsExactly(new File(FILE_1), new File(FILE_2));
                }

                @Test
                void testSourceFilesAsPathArray() {
                    var op = new CompileKotlinOperation();
                    op = op.testSourceFiles(Path.of(FILE_1), Path.of(FILE_2));
                    assertThat(op.testSourceFiles()).containsExactly(new File(FILE_1), new File(FILE_2));
                }

                @Test
                void testSourceFilesAsPathList() {
                    var op = new CompileKotlinOperation();
                    op.testSourceFilesPaths(List.of(new File(FILE_1).toPath(), new File(FILE_2).toPath()));
                    assertThat(op.testSourceFiles()).containsExactly(new File(FILE_1), new File(FILE_2));
                }

                @Test
                void testSourceFilesAsStringArray() {
                    var op = new CompileKotlinOperation();
                    op.testSourceFiles(FILE_1, FILE_2);
                    assertThat(op.testSourceFiles()).containsExactly(new File(FILE_1), new File(FILE_2));
                }

                @Test
                void testSourceFilesAsStringList() {
                    var op = new CompileKotlinOperation();
                    op.testSourceFilesStrings(List.of(FILE_1, FILE_2));
                    assertThat(op.testSourceFiles()).containsExactly(new File(FILE_1), new File(FILE_2));
                }
            }
        }

        @Nested
        @DisplayName("Work Dir Tests")
        class WorkDirTests {

            private final File bar = new File(BAR);
            private final File foo = new File(FOO);
            private CompileKotlinOperation op = new CompileKotlinOperation();

            @Test
            void workDirAsFile() {
                op.workDir(foo);
                assertThat(op.workDir()).isEqualTo(foo);
            }

            @Test
            void workDirAsPath() {
                op = op.workDir(bar.toPath());
                assertThat(op.workDir()).isEqualTo(bar);
            }

            @Test
            void workDirAsString() {
                op = new CompileKotlinOperation().workDir(FOO);
                assertThat(op.workDir()).isEqualTo(foo);
            }
        }
    }
}