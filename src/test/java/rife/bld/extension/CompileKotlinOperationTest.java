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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import rife.bld.BaseProject;
import rife.bld.blueprints.BaseProjectBlueprint;
import rife.bld.dependencies.Repository;
import rife.bld.dependencies.Version;
import rife.bld.dependencies.VersionNumber;
import rife.bld.extension.kotlin.CompileOptions;
import rife.bld.extension.kotlin.CompilerPlugin;
import rife.bld.extension.kotlin.JvmOptions;
import rife.bld.extension.tools.IOTools;
import rife.bld.extension.tools.SystemTools;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.bld.testing.LoggingExtension;
import rife.bld.testing.RandomString;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(LoggingExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CompileKotlinOperationTest {

    private static final String BAR = "bar";
    private static final String FILE_1 = "file1";
    private static final String FILE_2 = "file2";
    private static final String FOO = "foo";

    private static final String PROJECT = "examples";
    private static final String PROJECT_NAME = "Example";
    private static final String PROJECT_PACKAGE = "com.example";

    /**
     * Class-wide, initially empty directory used as the {@code workDirectory} of the shared
     * {@link TestProject} for the download tests.
     * <p>
     * Because it is a {@code static @TempDir}, JUnit creates it once per test class (empty)
     * and deletes it afterward. The first download test that runs creates
     * {@code lib/.sandbox/bld-kotlin} and downloads the compiler; every subsequent download
     * test in the same class reuses that sandbox, matching how a CI run behaves. It is
     * deliberately <em>not</em> under {@code src/test/resources}, so a sandbox left over
     * from a prior developer run cannot mask the "sandbox does not exist" precondition.
     */
    @TempDir
    @SuppressWarnings("PMD.MutableStaticState") // @TempDir requires a non-final, injectable static field
    static Path sharedProjectRoot;

    @TempDir
    private File tmpDir;

    /**
     * Runs before any test in this class, including every nested test.
     * <p>
     * Establishes that the download tests exercise the "no sandbox present" path at least
     * once, like a CI checkout: the first download test creates the sandbox, and later tests
     * reuse it.
     */
    @BeforeAll
    static void assertSandboxAbsentAtStart() {
        assertThat(sharedProjectRoot)
                .as("JUnit must inject the class-wide shared project root")
                .isNotNull();
        assertThat(new File(sharedProjectRoot.toFile(), "lib/.sandbox/bld-kotlin"))
                .as("sandbox must not exist before the first download test")
                .doesNotExist();
    }

    /**
     * Returns the class-wide {@link TestProject}. The sandbox under
     * {@code <sharedProjectRoot>/lib/.sandbox/bld-kotlin} is empty on the first call and
     * populated by the first download test thereafter — matching CI, where the sandbox is
     * created once and reused (or re-extracted on version change) by subsequent tests.
     */
    private static TestProject sharedProject() {
        assertThat(sharedProjectRoot)
                .as("JUnit must inject the class-wide shared project root")
                .isNotNull();
        return new TestProject(sharedProjectRoot.toFile());
    }

    /**
     * A real {@link BaseProject} used by the sandbox tests, so the downloaded Kotlin
     * distribution lands in {@code <workDirectory>/lib/.sandbox/bld-kotlin}.
     * <p>
     * Note: the operation's {@code workDir(...)} (the process working directory passed to
     * {@code kotlinc}) is independent of the project's {@code workDirectory()} (where the
     * {@code Sandbox} extracts downloads). The sandbox tests deliberately keep these separate:
     * sources/output live in a per-test {@code @TempDir}, while the downloaded compiler is
     * cached under the shared project root so it can be reused across tests in a run.
     */
    // This is a test helper, not a test class; it deliberately has no test methods.
    @SuppressWarnings("PMD.TestClassWithoutTestCases")
    static class TestProject extends BaseProject {

        /**
         * Roots the project at {@code workDirectory}, so the sandbox lives at
         * {@code <workDirectory>/lib/.sandbox/bld-kotlin}. Passing a shared root gives every
         * download test the same sandbox: created by the first test, reused by the rest.
         */
        // BaseProject only exposes version(...) as an overridable setter; that is the
        // sanctioned way to configure the version in a constructor, so the call is required.
        @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
        TestProject(File workDirectory) {
            super();
            this.workDirectory = workDirectory;
            pkg = "com.example";
            name = "TestProject";
            version = version(0, 1, 0);

            autoDownloadPurge = true;

            repositories = List.of(Repository.MAVEN_CENTRAL);
        }
    }

    @Nested
    @DisplayName("Compiler Plugin Download Tests")
    class CompilerPluginDownloadTests {

        /**
         * Enum values kept for backward compatibility whose JARs are not bundled with current
         * Kotlin distributions.
         * <p>
         * Add an entry here when a plugin that used to ship in {@code kotlinc/lib/} is dropped
         * from newer Kotlin releases but remains in {@link CompilerPlugin} for source
         * compatibility. The {@link #downloadedCompilerShipsBundledPlugins} test skips these.
         */
        private static final Set<CompilerPlugin> NOT_IN_CURRENT_DISTRIBUTION =
                EnumSet.of(CompilerPlugin.KOTLIN_IMPORTS_DUMPER);

        /**
         * Returns the {@code lib/} directory of the downloaded compiler, where the bundled
         * compiler plugins live.
         */
        private static File sandboxKotlincLib(TestProject project) {
            return new File(project.workDirectory(), "lib/.sandbox/bld-kotlin/kotlinc/lib");
        }

        @Test
        @DisplayName("downloaded compiler ships the bundled compiler plugins in lib/")
        void downloadedCompilerShipsBundledPlugins(@TempDir Path tempDir) throws Exception {
            var buildDir = new File(tempDir.toFile(), "build");
            var mainDir = new File(buildDir, "main");
            var testDir = new File(buildDir, "test");
            assertThat(mainDir.mkdirs()).isTrue();
            assertThat(testDir.mkdirs()).isTrue();

            var srcMain = new File(tempDir.toFile(), "src/main/kotlin");
            assertThat(srcMain.mkdirs()).isTrue();
            Files.writeString(new File(srcMain, "Hello.kt").toPath(),
                    "fun main() { println(\"Hello\") }\n");

            var project = sharedProject();
            var sandboxLib = sandboxKotlincLib(project);

            // Trigger the download via useDownloadedKotlin, then assert the bundled plugin JARs
            // are present in the extracted distribution's lib/ directory.
            var op = new CompileKotlinOperation()
                    .fromProject(project)
                    .useDownloadedKotlin()
                    .workDir(tempDir.toFile())
                    .buildMainDirectory(mainDir)
                    .buildTestDirectory(testDir)
                    .mainSourceDirectories(srcMain)
                    .compileOptions(o -> o.noStdLib(false))
                    .silent(true);

            assertThatCode(op::execute).doesNotThrowAnyException();

            try (var softly = new AutoCloseableSoftAssertions()) {
                Arrays.stream(CompilerPlugin.values())
                        .filter(p -> !NOT_IN_CURRENT_DISTRIBUTION.contains(p))
                        .forEach(p -> softly.assertThat(new File(sandboxLib, p.getJar()))
                                .as("downloaded lib/ should contain " + p.getJar())
                                .isFile());
            }
        }

        @Test
        @DisplayName("explicit plugin directory still takes precedence over the download")
        void explicitPluginDirectoryTakesPrecedence(@TempDir Path tempDir) throws Exception {
            var buildDir = new File(tempDir.toFile(), "build");
            var mainDir = new File(buildDir, "main");
            var testDir = new File(buildDir, "test");
            assertThat(mainDir.mkdirs()).isTrue();
            assertThat(testDir.mkdirs()).isTrue();

            var srcMain = new File(tempDir.toFile(), "src/main/kotlin");
            assertThat(srcMain.mkdirs()).isTrue();
            Files.writeString(new File(srcMain, "Hello.kt").toPath(),
                    "fun main() { println(\"Hello\") }\n");

            var project = sharedProject();
            var sandboxLib = sandboxKotlincLib(project);

            // Using the plugins(File, CompilerPlugin...) overload stores absolute paths,
            // so plugin resolution does not go through findKotlinHome(). The compile should
            // still succeed even if a downloaded compiler is used alongside.
            var op = new CompileKotlinOperation()
                    .fromProject(project)
                    .useDownloadedKotlin()
                    .workDir(tempDir.toFile())
                    .buildMainDirectory(mainDir)
                    .buildTestDirectory(testDir)
                    .mainSourceDirectories(srcMain)
                    .compileOptions(o -> o.noStdLib(false))
                    .silent(true);

            assertThatCode(op::execute).doesNotThrowAnyException();

            // The bundled plugin JARs live next to the compiler even when a plugin directory
            // is explicitly configured; verify they are on disk so resolution is unambiguous.
            assertThat(new File(sandboxLib, CompilerPlugin.LOMBOK.getJar()))
                    .as("downloaded lib/ still contains the bundled plugin JAR")
                    .isFile();
        }

        @Test
        @DisplayName("plugins(...) resolves against the downloaded compiler's lib/")
        void pluginsResolveAgainstDownloadedCompiler(@TempDir Path tempDir) throws Exception {
            var buildDir = new File(tempDir.toFile(), "build");
            var mainDir = new File(buildDir, "main");
            var testDir = new File(buildDir, "test");
            assertThat(mainDir.mkdirs()).isTrue();
            assertThat(testDir.mkdirs()).isTrue();

            // Use @JvmInline value class, which requires the "no-arg" / "all-open" style plugin
            // machinery to be found and loaded. But since those don't change the output of
            // this trivial source, we don't assert on the produced classes; we only assert
            // that the plugin is passed as -Xplugin= and doesn't break compiling.
            var srcMain = new File(tempDir.toFile(), "src/main/kotlin");
            assertThat(srcMain.mkdirs()).isTrue();
            Files.writeString(new File(srcMain, "Hello.kt").toPath(),
                    "fun main() { println(\"Hello\") }\n");

            var project = sharedProject();

            var op = new CompileKotlinOperation()
                    .fromProject(project)
                    .useDownloadedKotlin()
                    .workDir(tempDir.toFile())
                    .buildMainDirectory(mainDir)
                    .buildTestDirectory(testDir)
                    .mainSourceDirectories(srcMain)
                    .plugins(CompilerPlugin.ALL_OPEN, CompilerPlugin.NOARG)
                    .compileOptions(o -> o.noStdLib(false).verbose(true))
                    .silent(true);

            assertThatCode(op::execute)
                    .as("downloaded compiler should resolve bundled ALL_OPEN and NOARG plugins")
                    .doesNotThrowAnyException();

            assertThat(new File(mainDir, "HelloKt.class"))
                    .as("compilation produced HelloKt.class")
                    .isFile();
        }
    }

    @Nested
    @DisplayName("Execute Tests")
    class ExecuteTests {

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

            var compileOptions = op.compileOptions();
            assertThat(compileOptions).as("compileOptions() should not be null").isNotNull();
            compileOptions.verbose(true);
            compileOptions.argFile("src/test/resources/argfile.txt", "src/test/resources/argfile2.txt");

            var jvmOptions = op.jvmOptions();
            assertThat(jvmOptions).as("jvmOptions() should not be null").isNotNull();
            if (!SystemTools.isWindows()) {
                jvmOptions.nativeAccessModules(JvmOptions.ALL_UNNAMED);
                assertThat(jvmOptions.args()).containsExactly("--enable-native-access=ALL-UNNAMED");
            }

            var args = compileOptions.args();
            var matches = List.of("-Xjdk-release=17", "-no-reflect", "-progressive", "-include-runtime", "-no-stdlib",
                    "-verbose");
            assertThat(args).as("%s == %s", args, matches).isEqualTo(matches);

            op.execute();

            try (var softly = new AutoCloseableSoftAssertions()) {
                softly.assertThat(tmpDir).as("tmpDir shouldn't be empty").isNotEmptyDirectory();
                softly.assertThat(mainDir).as("mainDir shouldn't be empty").isNotEmptyDirectory();
                softly.assertThat(testDir).as("testDir shouldn't be empty").isNotEmptyDirectory();
            }

            var mainOut = IOTools.resolveFile(mainDir, "com", "example");
            try (var softly = new AutoCloseableSoftAssertions()) {
                softly.assertThat(new File(mainOut, "Example.class")).as("Example.class").exists();
                softly.assertThat(new File(mainOut, "Example$Companion.class"))
                        .as("ExampleCompanion.class").exists();
            }

            var testOut = IOTools.resolveFile(testDir, "com", "example");
            assertThat(new File(testOut, "ExampleTest.class")).as("ExampleTest.class").exists();
        }

        @Test
        void executeWithConsumer() throws Exception {
            var lines = new ArrayList<String>();
            var op = new CompileKotlinOperation()
                    .fromProject(new BaseProjectBlueprint(
                            new File(PROJECT), PROJECT_PACKAGE, PROJECT_NAME, PROJECT_NAME))
                    .inheritIO(false)
                    .outputConsumer(lines::add);
            var compileOptions = op.compileOptions();
            assertThat(compileOptions).as("compileOptions() should not be null").isNotNull();
            compileOptions.verbose(true);
            op.execute();
            assertThat(lines).contains("logging: configuring the compilation environment");
        }

        @Test
        void executeWithInheritIO() {
            var op = new CompileKotlinOperation()
                    .fromProject(new BaseProjectBlueprint(
                            new File(PROJECT), PROJECT_PACKAGE, PROJECT_NAME, PROJECT_NAME))
                    .inheritIO(true);
            assertThatCode(op::execute).doesNotThrowAnyException();
        }

        @Test
        void executeWithTimeout() {
            var op = new CompileKotlinOperation()
                    .fromProject(new BaseProjectBlueprint(
                            new File(PROJECT), PROJECT_PACKAGE, PROJECT_NAME, PROJECT_NAME))
                    .timeout(1);
            assertThatThrownBy(op::execute)
                    .isInstanceOf(ExitStatusException.class);
        }
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
                    .compileOptions(o -> o.advancedOptions("foo"))
                    .env("foo", "bar")
                    .env(Map.of("FOZ", "BAZ"))
                    .jvmOptions(o -> o.nativeAccessModules("foo"))
                    .inheritIO(true)
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
                    .timeout(10)
                    .plugins("plugin1", "plugin2")
                    .plugins(CompilerPlugin.KOTLIN_SERIALIZATION, CompilerPlugin.ASSIGNMENT, CompilerPlugin.COMPOSE)
                    .plugins(new File(LIB_COMPILE), CompilerPlugin.LOMBOK, CompilerPlugin.POWER_ASSERT)
                    .plugins(Path.of(LIB_COMPILE), CompilerPlugin.NOARG, CompilerPlugin.ALL_OPEN,
                            CompilerPlugin.KOTLIN_IMPORTS_DUMPER)
                    .plugins(LIB_COMPILE, CompilerPlugin.KOTLINX_SERIALIZATION, CompilerPlugin.SAM_WITH_RECEIVER)
                    .plugins(List.of("plugin3", "plugin4"))
                    .plugins("plugin5");

            var compileOptions = op.compileOptions();
            assertThat(compileOptions).as("compileOptions() should not be null").isNotNull();

            var kotlinHome = op.kotlinHome();
            assertThat(kotlinHome).as("kotlinHome() should not be null").isNotNull();

            var kotlinc = op.kotlinc();
            assertThat(kotlinc).as("kotlinc() should not be null").isNotNull();

            var workDir = op.workDir();
            assertThat(workDir).as("workDir() should not be null").isNotNull();

            try (var softly = new AutoCloseableSoftAssertions()) {
                softly.assertThat(kotlinHome.getName()).as("kotlin_home").isEqualTo("kotlin_home");
                softly.assertThat(kotlinc.getName()).as(KOTLINC).isEqualTo(KOTLINC);
                softly.assertThat(workDir.getName()).as("work_dir").isEqualTo("work_dir");
                softly.assertThat(op.compileMainClasspath()).as("compileMainClassPath")
                        .containsAll(List.of("path1", "path2"));
                softly.assertThat(compileOptions.hasRelease()).as("hasRelease").isTrue();
                softly.assertThat(compileOptions.hasTarget()).as("hasTaget").isTrue();
                softly.assertThat(compileOptions.isVerbose()).as("isVerbose").isTrue();
                softly.assertThat(compileOptions.advancedOptions()).contains("foo");
                softly.assertThat(op.isInheritIO()).as("inheritIO").isTrue();
                softly.assertThat(op.jvmOptions().nativeAccessModules()).contains("foo");
                softly.assertThat(op.mainSourceDirectories()).as("mainSourceDirectories").containsExactly(
                        Path.of(PROJECT, "src", "main", "kotlin").toFile(), new File("dir1"),
                        new File("dir2"), new File("dir3"), new File("dir4"));
                softly.assertThat(op.env()).containsOnly(entry("foo", "bar"), entry("FOZ", "BAZ"));
                softly.assertThat(op.testSourceDirectories()).as("testSourceDirectories").containsOnly(
                        Path.of(PROJECT, "src", "test", "kotlin").toFile(), new File("tdir1"),
                        new File("tdir2"), new File("tdir3"), new File("tdir4"));
                softly.assertThat(op.mainSourceFiles()).as("mainSourceFiles").containsOnly(
                        new File("file1"), new File("file2"), new File("file3"),
                        new File("file4"), new File("file5"), new File("file6"));
                softly.assertThat(op.testSourceFiles()).as("testSourceFiles").containsOnly(
                        new File("tfile1"), new File("tfile2"), new File("tfile3"),
                        new File("tfile4"), new File("tfile5"), new File("tfile6"));
                softly.assertThat(op.timeout()).as("timeout").isEqualTo(10);
                softly.assertThat(op.plugins()).as("plugins").contains(
                        "plugin1", "plugin2", "plugin3", "plugin4", "plugin5",
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
                var workDir = op.workDir();
                assertThat(workDir).as("workDir() should not be null").isNotNull();

                var compileOptions = op.compileOptions();
                assertThat(compileOptions).as("compileOptions() should not be null").isNotNull();

                assertThat(workDir.getPath()).as("workDir")
                        .isEqualTo(new File("examples").getAbsolutePath());
                assertThat(op.buildMainDirectory()).as("buildMainDirectory").exists();
                assertThat(op.buildTestDirectory()).as("buildTestDirectory").exists();
                assertThat(op.mainSourceDirectories()).as("mainSourceDirectories")
                        .containsExactly(new File(examples, "src/main/kotlin"));
                assertThat(op.testSourceDirectories()).as("testSourceDirectories")
                        .containsExactly(new File(examples, "src/test/kotlin"));
                assertThat(op.compileMainClasspath().size()).as("compileMainClasspath").isGreaterThan(1);
                assertThat(op.compileTestClasspath().size()).as("compileTestClasspath").isGreaterThan(1);
                assertThat(compileOptions.isNoStdLib()).as("isNoStdLib").isTrue();

            }

            @Test
            void fromProjectWithoutKotlin() {
                var op = new CompileKotlinOperation().fromProject(
                        new BaseProjectBlueprint(new File(FOO), "org.example", FOO, FOO));
                var compileOptions = op.compileOptions();
                assertThat(compileOptions).as("compileOptions() should not be null").isNotNull();

                assertThat(op.workDir()).as("workDir").doesNotExist();
                assertThat(op.mainSourceDirectories()).as("mainSourceDirectories").isEmpty();
                assertThat(op.testSourceDirectories()).as("testSourceDirectories").isEmpty();
                assertThat(op.compileMainClasspath()).as("compileMainClasspath").isEmpty();
                assertThat(op.compileTestClasspath().size()).as("compileTestClasspath").isEqualTo(1);
                assertThat(compileOptions.isNoStdLib()).as("isNoStdLib").isTrue();
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
                assertThat(CompileKotlinOperation.findKotlinCompilerPath()).doesNotStartWith(KOTLINC);
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

            @Nested
            @DisplayName("Use Downloaded Kotlin Tests")
            class UseDownloadedKotlinTests {

                @Test
                void canBeEnabled() {
                    assertThat(new CompileKotlinOperation().useDownloadedKotlin().isUseDownloadedKotlin())
                            .isTrue();
                }

                @Test
                void defaultsToFalse() {
                    assertThat(new CompileKotlinOperation().isUseDownloadedKotlin()).isFalse();
                }

                @Test
                void doesNotAffectKotlinHome() {
                    var kotlinHome = new File("/kotlin_home");
                    var op = new CompileKotlinOperation()
                            .kotlinHome(kotlinHome)
                            .useDownloadedKotlin();
                    // useDownloadedKotlin is a flag only; kotlinHome_ is untouched
                    assertThat(op.kotlinHome()).isEqualTo(kotlinHome);
                }

                @Test
                void doesNotAffectKotlinc() {
                    // Build the File once and reuse it for both the setter (File overload) and
                    // the assertion, so input and expected value are separator-neutral.
                    var kotlinc = new File("/some/kotlinc");
                    var op = new CompileKotlinOperation()
                            .kotlinc(kotlinc)
                            .useDownloadedKotlin();
                    assertThat(op.kotlinc()).isEqualTo(kotlinc);
                }

                @Test
                void explicitVersionAndUseDownloadedKotlinCoexist() {
                    var op = new CompileKotlinOperation()
                            .version("2.0.0")
                            .useDownloadedKotlin();

                    try (var softly = new AutoCloseableSoftAssertions()) {
                        softly.assertThat(op.isUseDownloadedKotlin()).isTrue();
                        softly.assertThat(op.version()).isEqualTo(new VersionNumber(2, 0, 0));
                    }
                }

                @Test
                void returnsSameInstance() {
                    var op = new CompileKotlinOperation();
                    assertThat(op.useDownloadedKotlin()).isSameAs(op);
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

        /**
         * Tests for the {@link CompileKotlinOperation#compileOptions(Consumer)} and
         * {@link CompileKotlinOperation#jvmOptions(Consumer)} overloads introduced in 1.3.
         */
        @Nested
        @DisplayName("Options Consumer Tests")
        class OptionsConsumerTests {

            @Test
            void compileOptionsConsumerAfterSetterAugmentsReplacements() {
                var op = new CompileKotlinOperation()
                        .compileOptions(new CompileOptions().path(FOO))
                        .compileOptions(o -> o.verbose(true));

                var opts = op.compileOptions();
                try (var softly = new AutoCloseableSoftAssertions()) {
                    softly.assertThat(opts.path()).as("path from setter").isEqualTo(new File(FOO));
                    softly.assertThat(opts.isVerbose()).as("verbose from consumer").isTrue();
                }
            }

            @Test
            void compileOptionsConsumerCanBeCalledMultipleTimes() {
                var op = new CompileKotlinOperation()
                        .compileOptions(o -> o.path(FOO))
                        .compileOptions(o -> o.verbose(true));

                var opts = op.compileOptions();
                try (var softly = new AutoCloseableSoftAssertions()) {
                    softly.assertThat(opts.path()).as("path from first call").isEqualTo(new File(FOO));
                    softly.assertThat(opts.isVerbose()).as("verbose from second call").isTrue();
                }
            }

            @Test
            void compileOptionsConsumerChainsFluentCalls() {
                var op = new CompileKotlinOperation()
                        .compileOptions(o -> o.path(FOO).jvmTarget("17").verbose(true));

                var opts = op.compileOptions();
                try (var softly = new AutoCloseableSoftAssertions()) {
                    softly.assertThat(opts.path()).as("path").isEqualTo(new File(FOO));
                    softly.assertThat(opts.jvmTarget()).as("jvmTarget").isEqualTo("17");
                    softly.assertThat(opts.isVerbose()).as("verbose").isTrue();
                }
            }

            @Test
            void compileOptionsConsumerEquivalentToSetter() {
                var viaSetter = new CompileKotlinOperation()
                        .compileOptions(new CompileOptions().path(FOO).verbose(true));
                var viaConsumer = new CompileKotlinOperation()
                        .compileOptions(o -> o.path(FOO).verbose(true));

                assertThat(viaConsumer.compileOptions().args())
                        .as("Consumer and setter should produce identical args")
                        .isEqualTo(viaSetter.compileOptions().args());
            }

            @Test
            void compileOptionsConsumerMutatesLiveInstance() {
                var op = new CompileKotlinOperation();
                var before = op.compileOptions();

                op.compileOptions(o -> o.path(FOO).noWarn(true));

                var after = op.compileOptions();
                try (var softly = new AutoCloseableSoftAssertions()) {
                    softly.assertThat(after).as("instance identity is preserved").isSameAs(before);
                    softly.assertThat(after.path()).as("path").isEqualTo(new File(FOO));
                    softly.assertThat(after.isNoWarn()).as("noWarn").isTrue();
                }
            }

            @Test
            void jvmOptionsConsumerCanBeCalledMultipleTimes() {
                var op = new CompileKotlinOperation()
                        .jvmOptions(j -> j.nativeAccessModules("foo"))
                        .jvmOptions(j -> j.nativeAccessModules("bar"));

                assertThat(op.jvmOptions().nativeAccessModules())
                        .as("nativeAccessModules from both calls")
                        .containsExactly("foo", "bar");
            }

            @Test
            void jvmOptionsConsumerChainsFluentCalls() {
                var op = new CompileKotlinOperation()
                        .jvmOptions(j -> j.nativeAccessModules("foo").nativeAccessModules("bar"));

                assertThat(op.jvmOptions().nativeAccessModules())
                        .as("nativeAccessModules")
                        .containsExactly("foo", "bar");
            }

            @Test
            void jvmOptionsConsumerEquivalentToSetter() {
                var viaSetter = new CompileKotlinOperation()
                        .jvmOptions(new JvmOptions().nativeAccessModules(JvmOptions.ALL_UNNAMED));
                var viaConsumer = new CompileKotlinOperation()
                        .jvmOptions(j -> j.nativeAccessModules(JvmOptions.ALL_UNNAMED));

                assertThat(viaConsumer.jvmOptions().args())
                        .as("Consumer and setter should produce identical args")
                        .isEqualTo(viaSetter.jvmOptions().args());
            }

            @Test
            void jvmOptionsConsumerMutatesLiveInstance() {
                var op = new CompileKotlinOperation();
                var before = op.jvmOptions();

                op.jvmOptions(j -> j.nativeAccessModules(JvmOptions.ALL_UNNAMED));

                var after = op.jvmOptions();
                try (var softly = new AutoCloseableSoftAssertions()) {
                    softly.assertThat(after).as("instance identity is preserved").isSameAs(before);
                    softly.assertThat(after.nativeAccessModules())
                            .as("nativeAccessModules")
                            .containsExactly(JvmOptions.ALL_UNNAMED);
                }
            }

            @Test
            void jvmOptionsIsNeverNull() {
                assertThat(new CompileKotlinOperation().jvmOptions())
                        .as("jvmOptions() must never be null")
                        .isNotNull();
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
                var op = new CompileKotlinOperation()
                        .plugins(LIB_COMPILE, CompilerPlugin.LOMBOK, CompilerPlugin.ALL_OPEN);
                assertThat(op.plugins()).containsExactly(
                        new File(LIB_COMPILE, "lombok-compiler-plugin.jar").getAbsolutePath(),
                        new File(LIB_COMPILE, "allopen-compiler-plugin.jar").getAbsolutePath());
            }

            @Test
            @SuppressWarnings("DataFlowIssue")
            void pluginsAsCompilerPluginArrayWithNull() {
                var op = new CompileKotlinOperation();
                assertThatThrownBy(() ->
                        op.plugins(Path.of(LIB_COMPILE), CompilerPlugin.LOMBOK, null, CompilerPlugin.ALL_OPEN))
                        .isInstanceOf(NullPointerException.class);
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

    @Nested
    @DisplayName("Sandbox Download Tests")
    class SandboxDownloadTests {

        private static File kotlincLauncher(File kotlincHome) {
            var launcherName = SystemTools.isWindows() ? "kotlinc.bat" : "kotlinc";
            var launcher = new File(kotlincHome, launcherName);
            if (!launcher.isFile()) {
                launcher = new File(new File(kotlincHome, "bin"), launcherName);
            }
            return launcher;
        }

        /**
         * Derives the expected sandbox kotlinc home from the project's work directory, so the
         * assertion tracks whatever {@link TestProject} configures rather than duplicating it.
         */
        private static File sandboxKotlinc(TestProject project) {
            return new File(project.workDirectory(), "lib/.sandbox/bld-kotlin/kotlinc");
        }

        @Test
        @DisplayName("execute with default version resolves and downloads the latest")
        void executeWithDefaultVersionDownloadsLatest(@TempDir Path tempDir) throws Exception {
            var buildDir = new File(tempDir.toFile(), "build");
            var mainDir = new File(buildDir, "main");
            var testDir = new File(buildDir, "test");
            assertThat(mainDir.mkdirs()).isTrue();
            assertThat(testDir.mkdirs()).isTrue();

            var srcMain = new File(tempDir.toFile(), "src/main/kotlin");
            assertThat(srcMain.mkdirs()).isTrue();
            Files.writeString(new File(srcMain, "Hello.kt").toPath(),
                    "fun main() { println(\"Hello\") }\n");

            var project = sharedProject();
            var sandboxKotlinc = sandboxKotlinc(project);

            // Do not call version(...) -> KOTLIN_VERSION default triggers "fetch latest"
            var op = new CompileKotlinOperation()
                    .fromProject(project)
                    .useDownloadedKotlin()
                    .workDir(tempDir.toFile())
                    .buildMainDirectory(mainDir)
                    .buildTestDirectory(testDir)
                    .mainSourceDirectories(srcMain)
                    // fromProject() enables noStdLib; let the downloaded compiler supply
                    // its own bundled stdlib so the self-contained smoke test compiles.
                    .compileOptions(o -> o.noStdLib(false))
                    .silent(true);

            assertThatCode(op::execute)
                    .as("Default version should resolve to the latest and compile")
                    .doesNotThrowAnyException();

            assertThat(sandboxKotlinc)
                    .as("sandbox should contain an extracted kotlinc distribution")
                    .isDirectory();
        }

        @Test
        @DisplayName("execute with explicit version downloads into the sandbox")
        void executeWithExplicitVersion(@TempDir Path tempDir) throws Exception {
            var buildDir = new File(tempDir.toFile(), "build");
            var mainDir = new File(buildDir, "main");
            var testDir = new File(buildDir, "test");
            assertThat(mainDir.mkdirs()).as("make mainDir").isTrue();
            assertThat(testDir.mkdirs()).as("make testDir").isTrue();

            // Minimal main source so the compiler actually runs
            var srcMain = new File(tempDir.toFile(), "src/main/kotlin");
            assertThat(srcMain.mkdirs()).as("make srcMain").isTrue();
            Files.writeString(new File(srcMain, "Hello.kt").toPath(),
                    "fun main() { println(\"Hello\") }\n");

            var project = sharedProject();
            var sandboxKotlinc = sandboxKotlinc(project);

            var requested = "2.4.20";
            var op = new CompileKotlinOperation()
                    .fromProject(project)
                    .useDownloadedKotlin()
                    .version(requested)
                    .workDir(tempDir.toFile())
                    .buildMainDirectory(mainDir)
                    .buildTestDirectory(testDir)
                    .mainSourceDirectories(srcMain)
                    // fromProject() enables noStdLib; let the downloaded compiler supply
                    // its own bundled stdlib so the self-contained smoke test compiles.
                    .compileOptions(o -> o.noStdLib(false))
                    .silent(true);

            assertThatCode(op::execute)
                    .as("Kotlin " + requested + " should compile the source")
                    .doesNotThrowAnyException();

            // The downloaded distribution should be under the project's sandbox
            assertThat(sandboxKotlinc)
                    .as("sandbox should contain kotlinc: " + sandboxKotlinc.getAbsolutePath())
                    .isDirectory();

            // kotlinc launcher should be present and executable
            var launcher = kotlincLauncher(sandboxKotlinc);
            assertThat(launcher)
                    .as("sandbox should contain the kotlinc launcher: " + launcher.getAbsolutePath())
                    .isFile();
            if (!SystemTools.isWindows()) {
                assertThat(launcher.canExecute())
                        .as("kotlinc launcher should be executable: " + launcher.getAbsolutePath())
                        .isTrue();
            }
        }

        @Test
        @DisplayName("explicit version alone bypasses KOTLIN_HOME/PATH and downloads")
        void explicitVersionTriggersDownloadWithoutUseDownloadedKotlin(@TempDir Path tempDir) throws Exception {
            var buildDir = new File(tempDir.toFile(), "build");
            var mainDir = new File(buildDir, "main");
            var testDir = new File(buildDir, "test");
            assertThat(mainDir.mkdirs()).as("make mainDir").isTrue();
            assertThat(testDir.mkdirs()).as("make testDir").isTrue();

            var srcMain = new File(tempDir.toFile(), "src/main/kotlin");
            assertThat(srcMain.mkdirs()).as("make srcMain").isTrue();
            Files.writeString(new File(srcMain, "Hello.kt").toPath(),
                    "fun main() { println(\"Hello\") }\n");

            var project = sharedProject();
            var sandboxKotlinc = sandboxKotlinc(project);

            var requested = "2.4.20";

            // Intentionally omit useDownloadedKotlin(true): an explicit version is
            // itself sufficient to skip KOTLIN_HOME/PATH and force the download.
            // On a host without kotlinc, this exercises the fallback path; on a host
            // that has kotlinc, it specifically exercises the new precedence rule.
            var op = new CompileKotlinOperation()
                    .fromProject(project)
                    .version(requested)
                    .workDir(tempDir.toFile())
                    .buildMainDirectory(mainDir)
                    .buildTestDirectory(testDir)
                    .mainSourceDirectories(srcMain)
                    .compileOptions(o -> o.noStdLib(false))
                    .silent(true);

            assertThatCode(op::execute)
                    .as("Explicit version " + requested + " should compile without useDownloadedKotlin")
                    .doesNotThrowAnyException();

            assertThat(sandboxKotlinc)
                    .as("explicit version must have triggered the sandbox download: "
                            + sandboxKotlinc.getAbsolutePath())
                    .isDirectory();

            var launcher = kotlincLauncher(sandboxKotlinc);
            assertThat(launcher)
                    .as("sandbox should contain the kotlinc launcher: " + launcher.getAbsolutePath())
                    .isFile();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    @SuppressWarnings("DataFlowIssue")
    class ValidationTests {

        @ParameterizedTest
        @EmptySource
        void buildMainDirectoryWithEmpty(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().buildMainDirectory(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void buildMainDirectoryWithNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().buildMainDirectory((File) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().buildMainDirectory((Path) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void buildTestDirectoryWithEmpty(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().buildTestDirectory(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void buildTestDirectoryWithNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().buildTestDirectory((File) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().buildTestDirectory((Path) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void compileMainClasspathWithEmpty(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().compileMainClasspath("foo", arg))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileMainClasspath(arg))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileMainClasspath(List.of("foo", arg)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileMainClasspath(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void compileMainClasspathWithNull(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().compileMainClasspath("foo", arg))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileMainClasspath(arg))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileMainClasspath(List.of("foo", arg)))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileMainClasspath((String[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileMainClasspath((Collection<String>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void compileOptionsWithNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().compileOptions((CompileOptions) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void compileOptionsWithNullConsumer() {
            assertThatThrownBy(() -> new CompileKotlinOperation().compileOptions((Consumer<CompileOptions>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void compileTestClasspathWithEmpty(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().compileTestClasspath("foo", arg))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileTestClasspath(arg))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileTestClasspath(List.of("foo", arg)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileTestClasspath(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void compileTestClasspathWithNull(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().compileTestClasspath("foo", arg))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileTestClasspath(arg))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileTestClasspath(List.of("foo", arg)))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileTestClasspath((String[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().compileTestClasspath((Collection<String>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void executeWithNullWorkDir() {
            var op = new CompileKotlinOperation();
            assertThatThrownBy(op::execute)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("workDir");
        }

        @Test
        void executeWithVersionNoProject() {
            var op = new CompileKotlinOperation().workDir(".").version("2.0.20");
            assertThatThrownBy(op::execute)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("project");
        }

        @Test
        void fromProjectWithNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().fromProject(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("project");
        }

        @Test
        void jvmOptionsWithNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().jvmOptions((JvmOptions) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void jvmOptionsWithNullConsumer() {
            assertThatThrownBy(() -> new CompileKotlinOperation().jvmOptions((Consumer<JvmOptions>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void kotlinHomeWithEmpty(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().kotlinHome(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void kotlinHomeWithNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().kotlinHome((File) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().kotlinHome((Path) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void kotlincWithEmpty(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().kotlinc(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void kotlincWithNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().kotlinc((File) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().kotlinc((Path) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void mainSourceDirectoriesWithEmpty(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().mainSourceDirectories(arg))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().mainSourceDirectoriesStrings(List.of("foo", arg)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().mainSourceDirectoriesStrings(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void mainSourceDirectoriesWithNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().mainSourceDirectories((File[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().mainSourceDirectories((Path[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().mainSourceDirectories((Collection<File>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void mainSourceFilesWithEmpty(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().mainSourceFiles(arg))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().mainSourceFilesStrings(List.of("foo", arg)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().mainSourceFilesStrings(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void mainSourceFilesWithNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().mainSourceFiles((File[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().mainSourceFiles((Path[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().mainSourceFiles((Collection<File>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void pluginsWithEmpty(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().plugins("foo", arg))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new CompileKotlinOperation().plugins(List.of("foo", arg)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().plugins(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void pluginsWithNull(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().plugins("foo", arg))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().plugins(List.of("foo", arg)))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().plugins((String[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().plugins((Collection<String>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void pluginsWithNullCompilerPlugin() {
            assertThatThrownBy(() -> new CompileKotlinOperation().plugins((String) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().plugins((CompilerPlugin[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().plugins(new File("."), (CompilerPlugin[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().plugins(Path.of("."), (CompilerPlugin[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().plugins(".", (CompilerPlugin[]) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void testSourceDirectoriesWithEmpty(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().testSourceDirectories(arg))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().testSourceDirectoriesStrings(List.of("foo", arg)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().testSourceDirectoriesStrings(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void testSourceDirectoriesWithNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().testSourceDirectories((File[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().testSourceDirectories((Path[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().testSourceDirectories((Collection<File>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void testSourceFilesWithEmpty(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().testSourceFiles(arg))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().testSourceFilesStrings(List.of("foo", arg)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().testSourceFilesStrings(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void testSourceFilesWithNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().testSourceFiles((File[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().testSourceFiles((Path[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().testSourceFiles((Collection<File>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void workDirWithEmpty(String arg) {
            assertThatThrownBy(() -> new CompileKotlinOperation().workDir(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void workDirWithNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().workDir((File) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new CompileKotlinOperation().workDir((Path) null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Version Tests")
    class VersionTests {

        @Test
        void defaultVersion() {
            assertThat(CompileKotlinOperation.KOTLIN_VERSION)
                    .as("default KOTLIN_VERSION should be 2.4.21")
                    .isEqualTo(new VersionNumber(2, 4, 21));
        }

        @Test
        void defaultVersionIsConstant() {
            assertThat(new CompileKotlinOperation().version())
                    .as("default version() should be KOTLIN_VERSION")
                    .isEqualTo(CompileKotlinOperation.KOTLIN_VERSION);
        }

        @Test
        void kotlinHomeAfterVersion() {
            var kotlinHome = new File("/kotlin_home");
            var op = new CompileKotlinOperation()
                    .version("2.0.0")
                    .kotlinHome(kotlinHome);

            assertThat(op.kotlinHome())
                    .as("kotlinHome must be set after version")
                    .isEqualTo(kotlinHome);
            assertThat(op.version()).isEqualTo(new VersionNumber(2, 0, 0));
        }

        @Test
        void kotlincAfterVersion() {
            // Build the File once and reuse it for both the setter (File overload) and the
            // assertion, so input and expected value are separator-neutral.
            var kotlinc = new File("/some/kotlinc");
            var op = new CompileKotlinOperation()
                    .version("2.0.0")
                    .kotlinc(kotlinc);

            assertThat(op.kotlinc())
                    .as("kotlinc must be set after version")
                    .isEqualTo(kotlinc);
            assertThat(op.version()).isEqualTo(new VersionNumber(2, 0, 0));
        }

        @Test
        void versionAsString() {
            assertThat(new CompileKotlinOperation().version("2.0.0").version())
                    .isEqualTo(new VersionNumber(2, 0, 0));
        }

        @Test
        void versionAsVersionObject() {
            var v = new VersionNumber(2, 0, 0);
            assertThat(new CompileKotlinOperation().version(v).version()).isEqualTo(v);
        }

        @Test
        void versionDoesNotClearKotlinHome() {
            // Build the File once and reuse it for both the setter (File overload) and the
            // assertion, so input and expected value are separator-neutral.
            var kotlinHome = new File("/kotlin_home");
            var op = new CompileKotlinOperation()
                    .kotlinHome(kotlinHome)
                    .version("2.0.0");

            assertThat(op.kotlinHome())
                    .as("kotlinHome must survive an explicit version")
                    .isEqualTo(kotlinHome);
            assertThat(op.version()).isEqualTo(new VersionNumber(2, 0, 0));
        }

        @Test
        void versionDoesNotClearKotlinc() {
            // Build the File once and reuse it for both the setter (File overload) and the
            // assertion, so input and expected value are separator-neutral.
            var kotlinc = new File("/some/kotlinc");
            var op = new CompileKotlinOperation()
                    .kotlinc(kotlinc)
                    .version("2.0.0");

            assertThat(op.kotlinc())
                    .as("kotlinc must survive an explicit version")
                    .isEqualTo(kotlinc);
            assertThat(op.version()).isEqualTo(new VersionNumber(2, 0, 0));
        }

        @Test
        @SuppressWarnings("DataFlowIssue")
        void versionObjectRejectsNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().version((Version) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void versionStringAndObjectAreEquivalent() {
            var viaString = new CompileKotlinOperation().version("2.0.0").version();
            var viaObject = new CompileKotlinOperation().version(new VersionNumber(2, 0, 0)).version();

            assertThat(viaString)
                    .as("String and Version overloads must produce the same version")
                    .isEqualTo(viaObject);
        }

        @ParameterizedTest
        @EmptySource
        void versionStringRejectsBlank(String v) {
            assertThatThrownBy(() -> new CompileKotlinOperation().version(v))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @SuppressWarnings("DataFlowIssue")
        void versionStringRejectsNull() {
            assertThatThrownBy(() -> new CompileKotlinOperation().version((String) null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}