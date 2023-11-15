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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rife.bld.blueprints.BaseProjectBlueprint;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    void testExecute() throws IOException {
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