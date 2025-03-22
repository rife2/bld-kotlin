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

package rife.bld.extension;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rife.bld.BaseProject;
import rife.bld.blueprints.BaseProjectBlueprint;
import rife.bld.extension.kotlin.CompileOptions;
import rife.bld.extension.kotlin.CompilerPlugin;
import rife.bld.extension.kotlin.JvmOptions;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CompileKotlinOperationTest {
    private static final String FILE_1 = "file1";
    private static final String FILE_2 = "file2";

    @BeforeAll
    static void beforeAll() {
        var level = Level.ALL;
        var logger = Logger.getLogger("rife.bld.extension");
        var consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);
        logger.addHandler(consoleHandler);
        logger.setLevel(level);
        logger.setUseParentHandlers(false);
    }

    @Test
    void testBuildMainDirectory() {
        var foo = new File("foo");
        var bar = new File("bar");

        var op = new CompileKotlinOperation().buildMainDirectory(foo);
        assertThat(op.buildMainDirectory()).as("as file").isEqualTo(foo);

        op = op.buildMainDirectory(bar.toPath());
        assertThat(op.buildMainDirectory()).as("as path").isEqualTo(bar);

        op = new CompileKotlinOperation().buildMainDirectory("foo");
        assertThat(op.buildMainDirectory()).as("as string").isEqualTo(foo);
    }

    @Test
    void testBuildTestDirectory() {
        var foo = new File("foo");
        var bar = new File("bar");

        var op = new CompileKotlinOperation().buildTestDirectory(foo);
        assertThat(op.buildTestDirectory()).as("as file").isEqualTo(foo);

        op = op.buildTestDirectory(bar.toPath());
        assertThat(op.buildTestDirectory()).as("as path").isEqualTo(bar);

        op = new CompileKotlinOperation().buildTestDirectory("foo");
        assertThat(op.buildTestDirectory()).as("as string").isEqualTo(foo);
    }

    @Test
    void testCollections() {
        var op = new CompileKotlinOperation()
                .fromProject(new BaseProjectBlueprint(new File("examples"), "com.example", "Example", "Example"))
                .kotlinHome("/kotlin_home")
                .kotlinc("kotlinc")
                .workDir("work_dir")
                .compileMainClasspath("path1", "path2")
                .compileOptions(new CompileOptions().jdkRelease("17").verbose(true))
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
                .plugins(new File("lib/compile"), CompilerPlugin.LOMBOK, CompilerPlugin.POWER_ASSERT)
                .plugins(Path.of("lib/compile"), CompilerPlugin.NOARG, CompilerPlugin.ALL_OPEN,
                        CompilerPlugin.KOTLIN_IMPORTS_DUMPER)
                .plugins("lib/compile", CompilerPlugin.KOTLINX_SERIALIZATION, CompilerPlugin.SAM_WITH_RECEIVER)

                .plugins(List.of("plugin3", "plugin4"));

        try (var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(op.kotlinHome().getName()).as("kotlin_home").isEqualTo("kotlin_home");
            softly.assertThat(op.kotlinc().getName()).as("kotlinc").isEqualTo("kotlinc");
            softly.assertThat(op.workDir().getName()).as("work_dir").isEqualTo("work_dir");
            softly.assertThat(op.compileMainClasspath()).as("compileMainClassPath")
                    .containsAll(List.of("path1", "path2"));
            softly.assertThat(op.compileOptions().hasRelease()).as("hasRelease").isTrue();
            softly.assertThat(op.compileOptions().isVerbose()).as("isVerbose").isTrue();
            softly.assertThat(op.mainSourceDirectories()).as("mainSourceDirectories").containsExactly(
                    Path.of("examples", "src", "main", "kotlin").toFile(), new File("dir1"),
                    new File("dir2"), new File("dir3"), new File("dir4"));
            softly.assertThat(op.testSourceDirectories()).as("testSourceDirectories").containsOnly(
                    Path.of("examples", "src", "test", "kotlin").toFile(), new File("tdir1"),
                    new File("tdir2"), new File("tdir3"), new File("tdir4"));
            softly.assertThat(op.mainSourceFiles()).as("mainSourceFiles").containsOnly(
                    new File("file1"), new File("file2"), new File("file3"),
                    new File("file4"), new File("file5"), new File("file6"));
            softly.assertThat(op.testSourceFiles()).as("testSourceFiles").containsOnly(
                    new File("tfile1"), new File("tfile2"), new File("tfile3"),
                    new File("tfile4"), new File("tfile5"), new File("tfile6"));
            softly.assertThat(op.plugins()).as("plugins").contains("plugin1", "plugin2", "plugin3", "plugin4",
                    "/kotlin_home/lib/kotlin-serialization-compiler-plugin.jar",
                    "/kotlin_home/lib/assignment-compiler-plugin.jar",
                    "/kotlin_home/lib/compose-compiler-plugin.jar",
                    new File("lib/compile", "lombok-compiler-plugin.jar").getAbsolutePath(),
                    new File("lib/compile", "power-assert-compiler-plugin.jar").getAbsolutePath(),
                    new File("lib/compile", "noarg-compiler-plugin.jar").getAbsolutePath(),
                    new File("lib/compile", "allopen-compiler-plugin.jar").getAbsolutePath(),
                    new File("lib/compile", "kotlin-imports-dumper-compiler-plugin.jar").getAbsolutePath(),
                    new File("lib/compile", "kotlinx-serialization-compiler-plugin.jar").getAbsolutePath(),
                    new File("lib/compile", "sam-with-receiver-compiler-plugin.jar").getAbsolutePath());
        }
    }

    @Test
    void testExecute() throws Exception {
        var tmpDir = Files.createTempDirectory("bld-kotlin").toFile();

        try {
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
                    .fromProject(new BaseProjectBlueprint(new File("examples"), "com.example", "Example", "Example"))
                    .buildMainDirectory(mainDir)
                    .buildTestDirectory(testDir)
                    .compileMainClasspath(compileJars)
                    .compileTestClasspath(testJars)
                    .compileTestClasspath(compileJars)
                    .compileTestClasspath(mainDir.getAbsolutePath());

            op.compileOptions().verbose(true);
            op.compileOptions().jdkRelease("17");
            op.compileOptions().jvmOptions().enableNativeAccess(JvmOptions.ALL_UNNAMED);

            var args = op.compileOptions().args();
            var matches = List.of("-Xjdk-release=17", "-J--enable-native-access=ALL-UNNAMED", "-no-stdlib", "-verbose");
            assertThat(args).as(args + " == " + matches).isEqualTo(matches);

            op.execute();

            try (var softly = new AutoCloseableSoftAssertions()) {
                softly.assertThat(tmpDir).as("tmpDir").isNotEmptyDirectory();
                softly.assertThat(mainDir).as("mainDir").isNotEmptyDirectory();
                softly.assertThat(testDir).as("testDir").isNotEmptyDirectory();
            }

            var mainOut = Path.of(mainDir.getAbsolutePath(), "com", "example").toFile();
            try (var softly = new AutoCloseableSoftAssertions()) {
                softly.assertThat(new File(mainOut, "Example.class")).as("Example.class").exists();
                softly.assertThat(new File(mainOut, "Example$Companion.class"))
                        .as("ExampleCompanion.class").exists();
            }

            var testOut = Path.of(testDir.getAbsolutePath(), "com", "example").toFile();
            assertThat(new File(testOut, "ExampleTest.class")).as("ExampleTest.class").exists();
        } finally {
            FileUtils.deleteDirectory(tmpDir);
        }
    }

    @Test
    void testFindKotlincPath() {
        assertThat(CompileKotlinOperation.findKotlincPath()).doesNotStartWith("kotlinc");
    }

    @Test
    void testFromProject() {
        var examples = new File("examples");
        var op = new CompileKotlinOperation().fromProject(
                new BaseProjectBlueprint(examples, "com.example", "examples", "examples"));
        assertThat(op.mainSourceDirectories()).contains(new File(examples, "src/main/kotlin"));
        assertThat(op.testSourceDirectories()).contains(new File(examples, "src/test/kotlin"));
    }

    @Test
    void testFromProjectNoKotlin() {
        var op = new CompileKotlinOperation().fromProject(
                new BaseProjectBlueprint(new File("foo"), "org.example", "foo", "foo"));
        assertThat(op.mainSourceDirectories()).isEmpty();
        assertThat(op.testSourceDirectories()).isEmpty();
    }

    @Test
    void testIsOS() {
        var osName = System.getProperty("os.name");
        if (osName != null) {
            var os = osName.toLowerCase(Locale.US);
            if (os.contains("win")) {
                assertThat(CompileKotlinOperation.isWindows()).isTrue();
            } else if (os.contains("linux") || os.contains("unix")) {
                assertThat(CompileKotlinOperation.isLinux()).isTrue();
            } else if (os.contains("mac") || os.contains("darwin")) {
                assertThat(CompileKotlinOperation.isMacOS()).isTrue();
            }
        }
    }

    @Test
    void testKotlinHome() {
        var foo = new File("foo");
        var bar = new File("bar");

        var op = new CompileKotlinOperation().kotlinHome(foo);
        assertThat(op.kotlinHome()).as("as file").isEqualTo(foo);

        op = op.kotlinHome(bar.toPath());
        assertThat(op.kotlinHome()).as("as path").isEqualTo(bar);

        op = new CompileKotlinOperation().kotlinHome("foo");
        assertThat(op.kotlinHome()).as("as string").isEqualTo(foo);
    }

    @Test
    void testKotlinc() {
        var foo = new File("foo");
        var bar = new File("bar");

        var op = new CompileKotlinOperation().kotlinc(foo);
        assertThat(op.kotlinc()).as("as file").isEqualTo(foo);

        op = op.kotlinc(bar.toPath());
        assertThat(op.kotlinc()).as("as path").isEqualTo(bar);

        op = new CompileKotlinOperation().kotlinc("foo");
        assertThat(op.kotlinc()).as("as string").isEqualTo(foo);
    }

    @Test
    void testMainSourceDirectories() {
        var op = new CompileKotlinOperation();

        op.mainSourceDirectories(List.of(new File(FILE_1), new File(FILE_2)));
        assertThat(op.mainSourceDirectories()).as("List(File...)").containsExactly(new File(FILE_1), new File(FILE_2));
        op.mainSourceDirectories().clear();

        op.mainSourceDirectories(new File(FILE_1), new File(FILE_2));
        assertThat(op.mainSourceDirectories()).as("File...").containsExactly(new File(FILE_1), new File(FILE_2));
        op.mainSourceDirectories().clear();

        op.mainSourceDirectories(FILE_1, FILE_2);
        assertThat(op.mainSourceDirectories()).as("String...")
                .containsExactly(new File(FILE_1), new File(FILE_2));
        op.mainSourceDirectories().clear();

        op = op.mainSourceDirectories(Path.of(FILE_1), Path.of(FILE_2));
        assertThat(op.mainSourceDirectories()).as("Path...")
                .containsExactly(new File(FILE_1), new File(FILE_2));
        op.mainSourceDirectories().clear();

        op.mainSourceDirectoriesPaths(List.of(new File(FILE_1).toPath(), new File(FILE_2).toPath()));
        assertThat(op.mainSourceDirectories()).as("List(Path...)")
                .containsExactly(new File(FILE_1), new File(FILE_2));
        op.mainSourceDirectories().clear();

        op.mainSourceDirectoriesStrings(List.of(FILE_1, FILE_2));
        assertThat(op.mainSourceDirectories()).as("List(String...)")
                .containsExactly(new File(FILE_1), new File(FILE_2));
        op.mainSourceDirectories().clear();
    }

    @Test
    void testMainSourceFiles() {
        var op = new CompileKotlinOperation();

        op.mainSourceFiles(List.of(new File(FILE_1), new File(FILE_2)));
        assertThat(op.mainSourceFiles()).as("List(File...)").containsExactly(new File(FILE_1), new File(FILE_2));
        op.mainSourceFiles().clear();

        op.mainSourceFiles(new File(FILE_1), new File(FILE_2));
        assertThat(op.mainSourceFiles()).as("File...").containsExactly(new File(FILE_1), new File(FILE_2));
        op.mainSourceFiles().clear();

        op.mainSourceFiles(FILE_1, FILE_2);
        assertThat(op.mainSourceFiles()).as("String...")
                .containsExactly(new File(FILE_1), new File(FILE_2));
        op.mainSourceFiles().clear();

        op = op.mainSourceFiles(Path.of(FILE_1), Path.of(FILE_2));
        assertThat(op.mainSourceFiles()).as("Path...")
                .containsExactly(new File(FILE_1), new File(FILE_2));
        op.mainSourceFiles().clear();

        op.mainSourceFilesPaths(List.of(new File(FILE_1).toPath(), new File(FILE_2).toPath()));
        assertThat(op.mainSourceFiles()).as("List(Path...)").containsExactly(new File(FILE_1), new File(FILE_2));
        op.mainSourceFiles().clear();

        op.mainSourceFilesStrings(List.of(FILE_1, FILE_2));
        assertThat(op.mainSourceFiles()).as("List(String...)").containsExactly(new File(FILE_1), new File(FILE_2));
        op.mainSourceFiles().clear();
    }

    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    void testPlugins() {
        var op = new CompileKotlinOperation()
                .fromProject(new BaseProject())
                .plugins(CompilerPlugin.ALL_OPEN,
                        CompilerPlugin.ASSIGNMENT,
                        CompilerPlugin.COMPOSE,
                        CompilerPlugin.KOTLIN_IMPORTS_DUMPER,
                        CompilerPlugin.KOTLINX_SERIALIZATION,
                        CompilerPlugin.KOTLIN_SERIALIZATION,
                        CompilerPlugin.LOMBOK,
                        CompilerPlugin.NOARG,
                        CompilerPlugin.POWER_ASSERT,
                        CompilerPlugin.SAM_WITH_RECEIVER);

        try (var softly = new AutoCloseableSoftAssertions()) {
            for (var p : op.plugins()) {
                softly.assertThat(new File(p)).as(p).exists();
            }
        }
    }

    @Test
    void testTestSourceDirectories() {
        var op = new CompileKotlinOperation();

        op.testSourceDirectories(List.of(new File(FILE_1), new File(FILE_2)));
        assertThat(op.testSourceDirectories()).as("List(File...)").containsExactly(new File(FILE_1), new File(FILE_2));
        op.testSourceDirectories().clear();

        op.testSourceDirectories(new File(FILE_1), new File(FILE_2));
        assertThat(op.testSourceDirectories()).as("File...").containsExactly(new File(FILE_1), new File(FILE_2));
        op.testSourceDirectories().clear();

        op.testSourceDirectories(FILE_1, FILE_2);
        assertThat(op.testSourceDirectories()).as("String...")
                .containsExactly(new File(FILE_1), new File(FILE_2));
        op.testSourceDirectories().clear();

        op = op.testSourceDirectories(Path.of(FILE_1), Path.of(FILE_2));
        assertThat(op.testSourceDirectories()).as("Path...")
                .containsExactly(new File(FILE_1), new File(FILE_2));
        op.testSourceDirectories().clear();

        op.testSourceDirectoriesPaths(List.of(new File(FILE_1).toPath(), new File(FILE_2).toPath()));
        assertThat(op.testSourceDirectories()).as("List(Path...)").containsExactly(new File(FILE_1), new File(FILE_2));
        op.testSourceDirectories().clear();

        op.testSourceDirectoriesStrings(List.of(FILE_1, FILE_2));
        assertThat(op.testSourceDirectories()).as("List(String...)").containsExactly(new File(FILE_1), new File(FILE_2));
        op.testSourceDirectories().clear();
    }

    @Test
    void testTestSourceFiles() {
        var op = new CompileKotlinOperation();

        op.testSourceFiles(List.of(new File(FILE_1), new File(FILE_2)));
        assertThat(op.testSourceFiles()).as("List(File...)").containsExactly(new File(FILE_1), new File(FILE_2));
        op.testSourceFiles().clear();

        op.testSourceFiles(new File(FILE_1), new File(FILE_2));
        assertThat(op.testSourceFiles()).as("File...").containsExactly(new File(FILE_1), new File(FILE_2));
        op.testSourceFiles().clear();

        op.testSourceFiles(FILE_1, FILE_2);
        assertThat(op.testSourceFiles()).as("String...")
                .containsExactly(new File(FILE_1), new File(FILE_2));
        op.testSourceFiles().clear();

        op = op.testSourceFiles(Path.of(FILE_1), Path.of(FILE_2));
        assertThat(op.testSourceFiles()).as("Path...")
                .containsExactly(new File(FILE_1), new File(FILE_2));
        op.testSourceFiles().clear();

        op.testSourceFilesPaths(List.of(new File(FILE_1).toPath(), new File(FILE_2).toPath()));
        assertThat(op.testSourceFiles()).as("List(Path...)").containsExactly(new File(FILE_1), new File(FILE_2));
        op.testSourceFiles().clear();

        op.testSourceFilesStrings(List.of(FILE_1, FILE_2));
        assertThat(op.testSourceFiles()).as("List(String...)").containsExactly(new File(FILE_1), new File(FILE_2));
        op.testSourceFiles().clear();
    }

    @Test
    void testWorkDir() {
        var foo = new File("foo");
        var bar = new File("bar");

        var op = new CompileKotlinOperation().workDir(foo);
        assertThat(op.workDir()).as("as file").isEqualTo(foo);

        op = op.workDir(bar.toPath());
        assertThat(op.workDir()).as("as path").isEqualTo(bar);

        op = new CompileKotlinOperation().workDir("foo");
        assertThat(op.workDir()).as("as string").isEqualTo(foo);
    }
}
