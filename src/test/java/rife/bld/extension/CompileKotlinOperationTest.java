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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rife.bld.Project;
import rife.bld.blueprints.BaseProjectBlueprint;
import rife.bld.extension.kotlin.CompileOptions;
import rife.bld.extension.kotlin.CompilerPlugin;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class CompileKotlinOperationTest {
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
    void testCollections() {
        var op = new CompileKotlinOperation()
                .fromProject(new Project())
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
                .plugins(CompilerPlugin.KOTLIN_SERIALIZATION, CompilerPlugin.ASSIGNMENT)
                .plugins(new File("lib/compile"), CompilerPlugin.LOMBOK, CompilerPlugin.POWER_ASSERT)
                .plugins(List.of("plugin3", "plugin4"))
                .plugins(Arrays.stream(Objects.requireNonNull(new File("lib/compile").listFiles())).toList(),
                        CompilerPlugin.ALL_OPEN, CompilerPlugin.SAM_WITH_RECEIVER);

        assertThat(op.compileMainClasspath()).as("compileMainClassPath")
                .containsAll(List.of("path1", "path2"));
        assertThat(op.compileOptions().hasRelease()).as("hasRelease").isTrue();
        assertThat(op.compileOptions().isVerbose()).as("isVerbose").isTrue();
        assertThat(op.mainSourceDirectories()).as("mainSourceDirectories").containsExactly(
                Path.of("src", "main", "kotlin").toFile().getAbsoluteFile(), new File("dir1"),
                new File("dir2"), new File("dir3"), new File("dir4"));
        assertThat(op.testSourceDirectories()).as("testSourceDirectories").containsOnly(
                Path.of("src", "test", "kotlin").toFile().getAbsoluteFile(), new File("tdir1"),
                new File("tdir2"), new File("tdir3"), new File("tdir4"));
        assertThat(op.mainSourceFiles()).as("mainSourceFiles").containsOnly(
                new File("file1"), new File("file2"), new File("file3"),
                new File("file4"), new File("file5"), new File("file6"));
        assertThat(op.testSourceFiles()).as("testSourceFiles").containsOnly(
                new File("tfile1"), new File("tfile2"), new File("tfile3"),
                new File("tfile4"), new File("tfile5"), new File("tfile6"));
        assertThat(op.plugins()).hasSize(10);
    }

    @Test
    void testExecute() throws Exception {
        var tmpDir = Files.createTempDirectory("bld-kotlin").toFile();

        try {
            var buildDir = new File(tmpDir, "build");
            var mainDir = new File(buildDir, "main");
            var testDir = new File(buildDir, "test");

            assertThat(mainDir.mkdirs()).isTrue();
            assertThat(testDir.mkdirs()).isTrue();

            var compileJars = new ArrayList<String>();
            for (var f : Objects.requireNonNull(new File("examples/lib/compile").listFiles())) {
                compileJars.add(f.getAbsolutePath());
            }

            var testJars = new ArrayList<String>();
            for (var f : Objects.requireNonNull(new File("examples/lib/test").listFiles())) {
                testJars.add(f.getAbsolutePath());
            }

            var op = new CompileKotlinOperation()
                    .fromProject(new BaseProjectBlueprint(new File("examples"), "com.example",
                            "Example"))
                    .buildMainDirectory(mainDir)
                    .buildTestDirectory(testDir)
                    .compileMainClasspath(compileJars)
                    .compileTestClasspath(testJars)
                    .compileTestClasspath(compileJars)
                    .compileTestClasspath(mainDir.getAbsolutePath());

            op.compileOptions().verbose(true);
            op.compileOptions().jdkRelease("17");

            var args = op.compileOptions().args();
            var matches = List.of("-Xjdk-release=17", "-no-stdlib", "-verbose");
            assertThat(args).isEqualTo(matches);

            op.execute();

            assertThat(tmpDir).isNotEmptyDirectory();
            assertThat(mainDir).isNotEmptyDirectory();
            assertThat(testDir).isNotEmptyDirectory();

            var mainOut = Path.of(mainDir.getAbsolutePath(), "com", "example").toFile();
            assertThat(new File(mainOut, "Example.class")).exists();
            assertThat(new File(mainOut, "Example$Companion.class")).exists();

            var testOut = Path.of(testDir.getAbsolutePath(), "com", "example").toFile();
            assertThat(new File(testOut, "ExampleTest.class")).exists();
        } finally {
            FileUtils.deleteDirectory(tmpDir);
        }
    }
}
