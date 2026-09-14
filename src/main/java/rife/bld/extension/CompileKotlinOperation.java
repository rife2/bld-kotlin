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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import rife.bld.BaseProject;
import rife.bld.extension.kotlin.CompileOptions;
import rife.bld.extension.kotlin.CompilerPlugin;
import rife.bld.extension.kotlin.JvmOptions;
import rife.bld.extension.tools.*;
import rife.bld.operations.AbstractOperation;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Compiles main and test Kotlin sources in the relevant build directories.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@NullMarked
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Builder pattern intentionally exposes mutable collections; callers may add to them directly"
)
public class CompileKotlinOperation extends AbstractOperation<CompileKotlinOperation> {

    private static final String KOTLIN_COMPILER = "kotlinc";
    private static final String KOTLIN_COMPILER_EXE = KOTLIN_COMPILER + (SystemTools.isWindows() ? ".bat" : "");
    private static final String MAIN_SOURCE_DIRECTORIES = "mainSourceDirectories";
    private static final String MAIN_SOURCE_FILES = "mainSourceFiles";
    private static final String PLUGINS = "plugins";
    private static final String TEST_SOURCE_DIRECTORIES = "testSourceDirectories";
    private static final String TEST_SOURCE_FILES = "testSourceFiles";
    private static final String WORK_DIR = "workDir";
    private static final Logger logger = Logger.getLogger(CompileKotlinOperation.class.getName());
    private static final Consumer<String> defaultOutputConsumer = logger::info;
    private final Set<String> compileMainClasspath_ = new LinkedHashSet<>();
    private final Set<String> compileTestClasspath_ = new LinkedHashSet<>();
    private final Map<String, String> env_ = new HashMap<>();
    private final List<File> mainSourceDirectories_ = new ArrayList<>();
    private final List<File> mainSourceFiles_ = new ArrayList<>();
    private final Set<String> plugins_ = new LinkedHashSet<>();
    private final List<File> testSourceDirectories_ = new ArrayList<>();
    private final List<File> testSourceFiles_ = new ArrayList<>();
    private @Nullable File buildMainDirectory_;
    private @Nullable File buildTestDirectory_;
    private @Nullable CompileOptions compileOptions_ = new CompileOptions();
    private boolean inheritIO_ = true;
    private @Nullable JvmOptions jvmOptions_ = new JvmOptions();
    private @Nullable File kotlinCompiler_;
    private boolean kotlinHomeResolved_;
    private @Nullable File kotlinHome_;
    private Consumer<String> outputConsumer_ = defaultOutputConsumer;
    private @Nullable BaseProject project_;
    private @Nullable String resolvedKotlinCompilerPath_;
    private @Nullable File resolvedKotlinHome_;
    private long timeout_ = 600L;
    private @Nullable File workDir_;

    /**
     * Performs the compile operation.
     *
     * @throws NullPointerException if {@code project} or {@code workDir} is {@code null}
     * @throws Exception            when an exception occurs during the execution
     */
    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public void execute() throws Exception {
        ObjectTools.requireNonNull(project_, "project");
        ObjectTools.requireNonNull(workDir_, WORK_DIR);

        executeCreateBuildDirectories();
        executeBuildMainSources();
        executeBuildTestSources();

        if (!silent()) {
            System.out.println("Kotlin compilation finished successfully.");
        }
    }

    @Nullable
    private static String findKotlinCompilerInDir(String directory) {
        var kotlinc = new File(directory, KOTLIN_COMPILER_EXE);

        if (IOTools.canExecute(kotlinc)) {
            return kotlinc.getAbsolutePath();
        }

        // Check the bin subdirectory if it exists
        var binDir = new File(directory, "bin");
        if (binDir.isDirectory()) {
            kotlinc = new File(binDir, KOTLIN_COMPILER_EXE);
            if (IOTools.canExecute(kotlinc)) {
                return kotlinc.getAbsolutePath();
            }
        }

        return null;
    }

    /**
     * Locates the Kotlin compiler (kotlinc) executable.
     *
     * @return The path to the {@code ekotlinc} executable, or {@code kotlinc}/{@code kotlinc.bat} if not found.
     * @since 1.1.0
     */
    public static String findKotlinCompilerPath() {
        return findKotlinCompilerPath(false);
    }

    /**
     * Locates the Kotlin compiler (kotlinc) executable.
     *
     * @param isSilent do not log the path to the {@code kotlinc} executable, if {@code true}
     * @return The path to the {@code kotlinc} executable, or {@code kotlinc}/{@code kotlinc.bat} if not found.
     * @since 1.1.0
     */
    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    protected static String findKotlinCompilerPath(boolean isSilent) {
        String kotlincPath;

        // Check the KOTLIN_HOME environment variable first
        var kotlinHome = System.getenv("KOTLIN_HOME");
        if (TextTools.isNotEmpty(kotlinHome)) {
            kotlincPath = findKotlinCompilerInDir(kotlinHome);
            if (kotlincPath != null) {
                logKotlinCompilerPath(kotlincPath, isSilent, "KOTLIN_HOME");
                return kotlincPath;
            }
        }

        // Check PATH environment variable
        var pathEnv = System.getenv("PATH");
        if (TextTools.isNotEmpty(pathEnv)) {
            var pathDirs = pathEnv.split(File.pathSeparator);
            for (var dir : pathDirs) {
                kotlincPath = findKotlinCompilerInDir(dir);
                if (kotlincPath != null) {
                    logKotlinCompilerPath(kotlincPath, isSilent, "PATH");
                    return kotlincPath;
                }
            }
        }

        // Common installation paths (e.g., SDKMAN!, IntelliJ IDEA, etc.)
        var commonPaths = new LinkedHashMap<String, @Nullable String>();

        if (SystemTools.isLinux()) {
            var userHome = System.getProperty("user.home");
            if (userHome != null) {
                commonPaths.put(userHome + "/.sdkman/candidates/kotlin/current/bin", "SDKMAN!");
            }
            commonPaths.put("/snap/bin", "Kotlin (Snap)");
            commonPaths.put("/usr/bin", null);
            commonPaths.put("/usr/share", null);
            commonPaths.put("/usr/local/bin", null);
            commonPaths.put("/usr/local/kotlin/bin", null);
            commonPaths.put("/usr/share/kotlin/bin/", null);
            commonPaths.put("/opt/kotlin/bin", null);
            if (userHome != null) {
                commonPaths.put(userHome + "/.local/share/JetBrains/Toolbox/apps/intellij-idea-ultimate/plugins/Kotlin/kotlinc/bin",
                        "IntelliJ IDEA Ultimate");
                commonPaths.put(userHome + "/.local/share/JetBrains/Toolbox/apps/intellij-idea-community-edition/plugins/Kotlin/kotlinc/bin",
                        "IntelliJ IDEA Community Edition");
                commonPaths.put(userHome + "/.local/share/JetBrains/Toolbox/apps/android-studio/plugins/Kotlin/kotlinc/bin",
                        "Android Studio");
            }
            commonPaths.put("/snap/intellij-idea-ultimate/current/commons/plugins/Kotlin/kotlinc/bin",
                    "IntelliJ IDEA Ultimate (Snap)");
            commonPaths.put("/snap/intellij-idea-community/current/commons/plugins/Kotlin/kotlinc/bin",
                    "IntelliJ IDEA Community Edition (Snap)");
            commonPaths.put("/snap/android-studio/current/android-studio/commons/plugins/Kotlin/kotlinc/bin",
                    "Android Studio (Snap)");
        } else if (SystemTools.isWindows()) {
            commonPaths.put("C:\\tools\\kotlinc\\bin", null);
            var localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                commonPaths.put(localAppData + "\\Programs\\IntelliJ IDEA Ultimate\\plugins\\Kotlin\\kotlinc\\bin",
                        "IntelliJ IDEA Ultimate");
                commonPaths.put(localAppData + "\\Programs\\IntelliJ IDEA Community Edition\\plugins\\Kotlin\\kotlinc\\bin",
                        "IntelliJ IDEA Community Edition");
                commonPaths.put(localAppData + "\\Programs\\Android Studio\\plugins\\Kotlin\\kotlinc\\bin",
                        "Android Studio");
            }
            var programFiles = System.getenv("ProgramFiles");
            if (programFiles != null) {
                commonPaths.put(programFiles + "\\Kotlin\\bin", null);
            }
        } else if (SystemTools.isMacOS()) {
            var userHome = System.getProperty("user.home");
            if (userHome != null) {
                commonPaths.put(userHome + "/.sdkman/candidates/kotlin/current/bin", "SDKMAN!");
            }
            commonPaths.put("/opt/homebrew/bin", "Homebrew");
            commonPaths.put("/usr/local/bin", null);
            commonPaths.put("/Applications/IntelliJ IDEA.app/Contents/plugins/Kotlin/kotlinc/bin/",
                    "IntelliJ IDEA");
            commonPaths.put("/Applications/IntelliJ IDEA Community Edition.app/Contents/plugins/Kotlin/kotlinc/bin/",
                    "IntelliJ IDEA Community Edition");
            commonPaths.put("/Applications/Android Studio.app/Contents/plugins/Kotlin/kotlinc/bin",
                    "Android Studio");
        }

        for (var path : commonPaths.entrySet()) {
            kotlincPath = findKotlinCompilerInDir(path.getKey());
            if (kotlincPath != null) {
                logKotlinCompilerPath(kotlincPath, isSilent, commonPaths.get(path.getKey()));
                return kotlincPath;
            }
        }

        // Try 'which' or 'where' commands (less reliable but sometimes works)
        Process process = null;
        try {
            if (SystemTools.isWindows()) {
                process = Runtime.getRuntime().exec("where " + KOTLIN_COMPILER);
            } else {
                process = Runtime.getRuntime().exec("which " + KOTLIN_COMPILER);
            }

            try (var scanner = new Scanner(process.getInputStream())) {
                if (scanner.hasNextLine()) {
                    kotlincPath = scanner.nextLine().trim();
                    if (IOTools.canExecute(new File(kotlincPath))) {
                        logKotlinCompilerPath(kotlincPath, isSilent);
                        return kotlincPath;
                    }
                }
            } finally {
                process.waitFor();
            }
        } catch (IOException | SecurityException | IllegalArgumentException ignored) {
            // Ignore exceptions from which/where, as they might not be available
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return KOTLIN_COMPILER_EXE;
    }

    private static void logKotlinCompilerPath(String kotlinCompilerPath, boolean isSilent) {
        logKotlinCompilerPath(kotlinCompilerPath, isSilent, null);
    }

    private static void logKotlinCompilerPath(String kotlinCompilerPath, boolean isSilent, @Nullable String from) {
        if (logger.isLoggable(Level.INFO) && !isSilent) {
            if (from != null) {
                logger.info("Using Kotlin compiler inferred from " + from + ": " + kotlinCompilerPath);
            } else {
                logger.info("Using Kotlin compiler found at: " + kotlinCompilerPath);
            }
        }
    }

    /**
     * Provides the main build destination directory.
     *
     * @param directory the directory to use for the main build destination
     * @return this operation instance
     * @throws NullPointerException if {@code directory} is {@code null}
     */
    public CompileKotlinOperation buildMainDirectory(Path directory) {
        ObjectTools.requireNonNull(directory, "buildMainDirectory");
        return buildMainDirectory(directory.toFile());
    }

    /**
     * Provides the main build destination directory.
     *
     * @param directory the directory to use for the main build destination
     * @return this operation instance
     * @throws NullPointerException if {@code directory} is {@code null}
     */
    public CompileKotlinOperation buildMainDirectory(File directory) {
        buildMainDirectory_ = ObjectTools.requireNonNull(directory, "buildMainDirectory");
        return this;
    }

    /**
     * Provides the main build destination directory.
     *
     * @param directory the directory to use for the main build destination
     * @return this operation instance
     * @throws NullPointerException     if {@code directory} is {@code null}
     * @throws IllegalArgumentException if {@code directory} is blank
     */
    public CompileKotlinOperation buildMainDirectory(String directory) {
        TextTools.requireNotBlank(directory, "buildMainDirectory");
        return buildMainDirectory(new File(directory));
    }

    /**
     * Retrieves the main build destination directory.
     *
     * @return the main build directory
     */
    @Nullable
    public File buildMainDirectory() {
        return buildMainDirectory_;
    }

    /**
     * Provides the test build destination directory.
     *
     * @param directory the directory to use for the test build destination
     * @return this operation instance
     * @throws NullPointerException if {@code directory} is {@code null}
     */
    public CompileKotlinOperation buildTestDirectory(File directory) {
        buildTestDirectory_ = ObjectTools.requireNonNull(directory, "buildTestDirectory");
        return this;
    }

    /**
     * Provides the test build destination directory.
     *
     * @param directory the directory to use for the test build destination
     * @return this operation instance
     * @throws NullPointerException if {@code directory} is {@code null}
     */
    public CompileKotlinOperation buildTestDirectory(Path directory) {
        ObjectTools.requireNonNull(directory, "buildTestDirectory");
        return buildTestDirectory(directory.toFile());
    }

    /**
     * Provides the test build destination directory.
     *
     * @param directory the directory to use for the test build destination
     * @return this operation instance
     * @throws NullPointerException     if {@code directory} is {@code null}
     * @throws IllegalArgumentException if {@code directory} is blank
     */
    public CompileKotlinOperation buildTestDirectory(String directory) {
        TextTools.requireNotBlank(directory, "buildTestDirectory");
        return buildTestDirectory(new File(directory));
    }

    /**
     * Retrieves the test build destination directory.
     *
     * @return the test build directory
     */
    @Nullable
    public File buildTestDirectory() {
        return buildTestDirectory_;
    }

    /**
     * Provides entries for the main compilation classpath.
     *
     * @param classpath one or more classpath entries
     * @return this operation instance
     * @throws NullPointerException     if {@code classpath} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code classpath} is empty, or contains blank elements
     * @see #compileMainClasspath(Collection)
     */
    public CompileKotlinOperation compileMainClasspath(String... classpath) {
        TextTools.requireNotBlank("compileMainClasspath", classpath);
        return compileMainClasspath(List.of(classpath));
    }

    /**
     * Provides the entries for the main compilation classpath.
     *
     * @param classpath the classpath entries
     * @return this operation instance
     * @throws NullPointerException     if {@code classpath} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code classpath} is empty, or contains blank elements
     */
    public final CompileKotlinOperation compileMainClasspath(Collection<String> classpath) {
        TextTools.requireNotBlank(classpath, "compileMainClasspath");
        compileMainClasspath_.addAll(classpath);
        return this;
    }

    /**
     * Retrieves the entries for the main compilation classpath.
     *
     * @return the classpath entries
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Set<String> compileMainClasspath() {
        return compileMainClasspath_;
    }

    /**
     * Retrieves the compilation options for the compiler.
     *
     * @return the compilation options
     */
    @Nullable
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public CompileOptions compileOptions() {
        return compileOptions_;
    }

    /**
     * Provides the compilation options to pass to the Kotlin compiler.
     *
     * @param options the compiler options
     * @return this operation instance
     * @throws NullPointerException if {@code options} is {@code null}
     */
    public CompileKotlinOperation compileOptions(CompileOptions options) {
        compileOptions_ = ObjectTools.requireNonNull(options, "compileOptions");
        return this;
    }

    /**
     * Provides entries for the test compilation classpath.
     *
     * @param classpath one or more classpath entries
     * @return this operation instance
     * @throws NullPointerException     if {@code classpath} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code classpath} is empty, or contains blank elements
     */
    public CompileKotlinOperation compileTestClasspath(String... classpath) {
        TextTools.requireNotBlank("compileTestClasspath", classpath);
        return compileTestClasspath(List.of(classpath));
    }

    /**
     * Provides the entries for the test compilation classpath.
     *
     * @param classpath the classpath entries
     * @return this operation instance
     * @throws NullPointerException     if {@code classpath} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code classpath} is empty, or contains blank elements
     */
    public final CompileKotlinOperation compileTestClasspath(Collection<String> classpath) {
        TextTools.requireNotBlank(classpath, "compileTestClasspath");
        compileTestClasspath_.addAll(classpath);
        return this;
    }

    /**
     * Retrieves the entries for the test compilation classpath.
     *
     * @return the classpath entries
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Set<String> compileTestClasspath() {
        return compileTestClasspath_;
    }

    /**
     * Adds an environment variable.
     * <p>
     * These variables are merged with the current process environment. Existing variables
     * with the same name are overridden.
     *
     * @param name  the variable name, must not be null
     * @param value the variable value, must not be null
     * @return this operation instance
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws NullPointerException     if {@code name} or {@code value} is {@code null}
     * @see #env(Map)
     */
    public CompileKotlinOperation env(String name, String value) {
        env_.put(TextTools.requireNotBlank(name, "env name"),
                ObjectTools.requireNonNull(value, "env value"));
        return this;
    }

    /**
     * Adds environment variables.
     * <p>
     * These variables are merged with the current process environment. Existing variables
     * with the same name are overridden.
     *
     * @param vars the map of environment variables, must not be null and must not contain null keys or values
     * @return this operation instance
     * @throws NullPointerException if {@code vars} is {@code null}, or if {@code vars} contains a null key or value
     * @see #env(String, String)
     */
    public CompileKotlinOperation env(Map<String, String> vars) {
        env_.putAll(ObjectTools.requireNonNull(vars, "env"));
        return this;
    }

    /**
     * Returns the environment variables.
     * <p>
     * The returned map is mutable and can be modified directly before calling {@link #execute()}.
     *
     * @return the mutable environment variables map, never null
     */
    public Map<String, String> env() {
        return env_;
    }

    /**
     * Configures a compile operation from a {@link BaseProject}.
     * <p>
     * Sets the following from the project:
     * <ul>
     *     <li>
     *         {@link #workDir() workDir} to the project's directory, if not already set.
     *     </li>
     *     <li>
     *         {@link #buildMainDirectory() buildMainDirectory}, if not already set.
     *     </li>
     *     <li>
     *         {@link #buildTestDirectory() buildTestDirectory}, if not already set.
     *     </li>
     *     <li>
     *         {@link #compileMainClasspath() compileMainClasspath}, if not already set.
     *     </li>
     *     <li>
     *         {@link #compileTestClasspath() compilesTestClasspath}, if not already set.
     *     </li>
     *     <li>
     *         {@link #mainSourceDirectories() mainSourceDirectories} to the {@code kotlin} directory in
     *         {@link BaseProject#srcMainDirectory() srcMainDirectory}, if present and not already set.
     *     </li>
     *     <li>
     *         {@link #testSourceDirectories() testSourceDirectories} to the {@code kotlin} directory in
     *         {@link BaseProject#srcTestDirectory() srcTestDirectory}, if present and not already set.</li>
     *     <li>
     *         {@link CompileOptions#jdkRelease jdkRelease} to {@link BaseProject#javaRelease() javaRelease}, if not
     *         already set.
     *     </li>
     *     <li>
     *         {@link CompileOptions#jvmTarget jvmTarget} to {@link BaseProject#javaRelease() javaRelease}, if not
     *         already set.
     *     </li>
     *     <li>{@link CompileOptions#noStdLib(boolean) noStdLib} to {@code true}</li>
     * </ul>
     *
     * @param project the project to configure the compile operation from
     * @return this operation instance
     * @throws NullPointerException if {@code project} is {@code null}
     */
    public CompileKotlinOperation fromProject(BaseProject project) {
        project_ = Objects.requireNonNull(project, "The project must not be null");

        if (workDir_ == null) {
            workDir_ = new File(project.workDirectory().getAbsolutePath());
        }

        if (buildMainDirectory_ == null) {
            buildMainDirectory_ = project.buildMainDirectory();
        }
        if (buildTestDirectory_ == null) {
            buildTestDirectory_ = project.buildTestDirectory();
        }
        if (compileMainClasspath_.isEmpty()) {
            compileMainClasspath_.addAll(project.compileMainClasspath());
        }
        if (compileTestClasspath_.isEmpty()) {
            compileTestClasspath_.addAll(project.compileTestClasspath());
        }

        if (mainSourceDirectories_.isEmpty()) {
            var mainDir = new File(project.srcMainDirectory(), "kotlin");
            if (mainDir.exists()) {
                mainSourceDirectories_.add(mainDir);
            }
        }
        if (testSourceDirectories_.isEmpty()) {
            var testDir = new File(project.srcTestDirectory(), "kotlin");
            if (testDir.exists()) {
                testSourceDirectories_.add(testDir);
            }
        }

        if (compileOptions_ != null) {
            if (project.javaRelease() != null) {
                if (!compileOptions_.hasRelease()) {
                    compileOptions_.jdkRelease(project.javaRelease());
                }
                if (!compileOptions_.hasTarget()) {
                    compileOptions_.jvmTarget(project.javaRelease());
                }
            }

            compileOptions_.noStdLib(true);
        }

        return this;
    }

    /**
     * Configures whether the child process should inherit the I/O streams of the current JVM.
     * <p>
     * When {@code true}, the child process uses the same stdin, stdout, and stderr as the current
     * Java process. This enables interactive commands, preserves ANSI colors, and allows progress
     * bars to display correctly. Output is <em>not</em> captured by the logger and cannot be asserted in tests.
     * <p>
     * When {@code false}, stdout and stderr are merged and captured through the logger. This makes
     * output testable and keeps it in the build log, but breaks interactive prompts and ANSI formatting.
     * <p>
     * Default is {@code TRUE}
     *
     * @param inheritIO {@code true} to inherit I/O, {@code false} to capture output
     * @return this operation instance
     */
    public CompileKotlinOperation inheritIO(boolean inheritIO) {
        inheritIO_ = inheritIO;
        return this;
    }

    /**
     * Returns whether the child process inherits the I/O streams of the current JVM.
     *
     * @return {@code true} if I/O is inherited (default), {@code false} if {@code stderr} is redirected
     * to {@code stdout}
     * @see #inheritIO(boolean)
     */
    public boolean isInheritIO() {
        return inheritIO_;
    }

    /**
     * Retrieves the Java Virtual Machine options.
     *
     * @return the JVM options
     */
    @Nullable
    public JvmOptions jvmOptions() {
        return jvmOptions_;
    }

    /**
     * Provides the Java Virtual Machine options.
     *
     * @param options the JVM options
     * @return this operation instance
     * @throws NullPointerException if {@code options} is {@code null}
     * @since 1.2
     */
    public CompileKotlinOperation jvmOptions(JvmOptions options) {
        jvmOptions_ = ObjectTools.requireNonNull(options, "jvmOptions");
        return this;
    }

    /**
     * Provides the Kotlin home directory, if it differs from the default {@code KOTLIN_HOME}.
     *
     * @param dir the directory path
     * @return this operation instance
     * @throws NullPointerException     if {@code dir} is {@code null}
     * @throws IllegalArgumentException if {@code dir} is blank
     */
    public CompileKotlinOperation kotlinHome(String dir) {
        TextTools.requireNotBlank(dir, "kotlinHome");
        return kotlinHome(new File(dir));
    }

    /**
     * Provides the Kotlin home directory, if it differs from the default {@code KOTLIN_HOME}.
     *
     * @param dir the directory
     * @return this operation instance
     * @throws NullPointerException if {@code dir} is {@code null}
     */
    public CompileKotlinOperation kotlinHome(File dir) {
        kotlinHome_ = ObjectTools.requireNonNull(dir, "kotlinHome");
        kotlinHomeResolved_ = false;
        resolvedKotlinHome_ = null;
        return this;
    }

    /**
     * Provides the Kotlin home directory, if it differs from the default {@code KOTLIN_HOME}.
     *
     * @param dir the directory path
     * @return this operation instance
     * @throws NullPointerException if {@code dir} is {@code null}
     */
    public CompileKotlinOperation kotlinHome(Path dir) {
        ObjectTools.requireNonNull(dir, "kotlinHome");
        return kotlinHome(dir.toFile());
    }

    /**
     * Retrieves the Kotlin home directory.
     *
     * @return the directory
     */
    @Nullable
    public File kotlinHome() {
        return kotlinHome_;
    }

    /**
     * Provides the path to the Kotlin compiler ({@code kotlinc}) executable, if not in {@link #kotlinHome()}.
     *
     * @param executable the executable path
     * @return this operation instance
     * @throws NullPointerException     if {@code executable} is {@code null}
     * @throws IllegalArgumentException if {@code executable} is blank
     */
    public CompileKotlinOperation kotlinc(String executable) {
        TextTools.requireNotBlank(executable, KOTLIN_COMPILER);
        return kotlinc(new File(executable));
    }

    /**
     * Retrieves the path to the Kotlin compiler ({@code kotlinc}) executable, if not in {@link #kotlinHome()}.
     *
     * @return the executable path
     */
    @Nullable
    public File kotlinc() {
        return kotlinCompiler_;
    }

    /**
     * Provides the path to the Kotlin compiler ({@code kotlinc}) executable, if not in {@link #kotlinHome()}.
     *
     * @param executable the executable path
     * @return this operation instance
     * @throws NullPointerException if {@code executable} is {@code null}
     */
    public CompileKotlinOperation kotlinc(File executable) {
        kotlinCompiler_ = ObjectTools.requireNonNull(executable, KOTLIN_COMPILER);
        kotlinHomeResolved_ = false;
        resolvedKotlinHome_ = null;
        return this;
    }

    /**
     * Provides the path to the Kotlin compiler ({@code kotlinc}) executable, if not in {@link #kotlinHome()}.
     *
     * @param executable the executable path
     * @return this operation instance
     * @throws NullPointerException if {@code executable} is {@code null}
     */
    public CompileKotlinOperation kotlinc(Path executable) {
        ObjectTools.requireNonNull(executable, KOTLIN_COMPILER);
        return kotlinc(executable.toFile());
    }

    /**
     * Retrieves the main source directories that should be compiled.
     *
     * @return the main source directories
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> mainSourceDirectories() {
        return mainSourceDirectories_;
    }

    /**
     * Provides main source directories that should be compiled.
     *
     * @param directories one or more main source directories
     * @return this operation instance
     * @throws NullPointerException if {@code directories} is {@code null}
     * @see #mainSourceDirectories(Collection)
     */
    public CompileKotlinOperation mainSourceDirectories(File... directories) {
        ObjectTools.requireNonNull(directories, MAIN_SOURCE_DIRECTORIES);
        return mainSourceDirectories(List.of(directories));
    }

    /**
     * Provides main source directories that should be compiled.
     *
     * @param directories one or more main source directories
     * @return this operation instance
     * @throws NullPointerException if {@code directories} is {@code null}
     * @see #mainSourceDirectoriesPaths(Collection)
     */
    public CompileKotlinOperation mainSourceDirectories(Path... directories) {
        ObjectTools.requireNonNull(directories, MAIN_SOURCE_DIRECTORIES);
        return mainSourceDirectoriesPaths(List.of(directories));
    }

    /**
     * Provides main source directories that should be compiled.
     *
     * @param directories one or more main source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null}
     * @throws IllegalArgumentException if {@code directories} is empty, or contains blank elements
     * @see #mainSourceDirectoriesStrings(Collection)
     */
    public CompileKotlinOperation mainSourceDirectories(String... directories) {
        TextTools.requireNotBlank("mainSourceDirectories", directories);
        return mainSourceDirectoriesStrings(List.of(directories));
    }

    /**
     * Provides the main source directories that should be compiled.
     *
     * @param directories the main source directories
     * @return this operation instance
     * @throws NullPointerException if {@code directories} is {@code null}
     * @see #mainSourceDirectories(File...)
     */
    public final CompileKotlinOperation mainSourceDirectories(Collection<File> directories) {
        ObjectTools.requireNonNull(directories, MAIN_SOURCE_DIRECTORIES);
        mainSourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Provides the main source directories that should be compiled.
     *
     * @param directories the main source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null}
     * @throws IllegalArgumentException if {@code directories} is empty, or contains {@code null} elements
     * @see #mainSourceDirectories(Path...)
     */
    public final CompileKotlinOperation mainSourceDirectoriesPaths(Collection<Path> directories) {
        ObjectTools.requireNotEmpty(directories, "mainSourceDirectoriesPaths");
        mainSourceDirectories_.addAll(CollectionTools.combinePathsToFiles(directories));
        return this;
    }

    /**
     * Provides the main source directories that should be compiled.
     *
     * @param directories the main source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null}
     * @throws IllegalArgumentException if {@code directories} is empty, or contains blank elements
     * @see #mainSourceDirectories(String...)
     */
    public final CompileKotlinOperation mainSourceDirectoriesStrings(Collection<String> directories) {
        TextTools.requireNotBlank(directories, "mainSourceDirectoriesStrings");
        mainSourceDirectories_.addAll(CollectionTools.combineStringsToFiles(directories));
        return this;
    }

    /**
     * Retrieves the main files that should be compiled.
     *
     * @return the files
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> mainSourceFiles() {
        return mainSourceFiles_;
    }

    /**
     * Provides main source files that should be compiled.
     *
     * @param files one or more main source files
     * @return this operation instance
     * @throws NullPointerException if {@code files} is {@code null}
     * @see #mainSourceFiles(Collection)
     */
    public CompileKotlinOperation mainSourceFiles(File... files) {
        ObjectTools.requireNonNull(files, MAIN_SOURCE_FILES);
        return mainSourceFiles(List.of(files));
    }

    /**
     * Provides the main source files that should be compiled.
     *
     * @param files one or more main source files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException if {@code files} is empty, or contains blank elements
     * @see #mainSourceFilesStrings(Collection)
     */
    public CompileKotlinOperation mainSourceFiles(String... files) {
        TextTools.requireNotBlank("mainSourceFiles", files);
        return mainSourceFilesStrings(List.of(files));
    }

    /**
     * Provides the main source files that should be compiled.
     *
     * @param files one or more main source files
     * @return this operation instance
     * @throws NullPointerException if {@code files} is {@code null}
     * @see #mainSourceFilesPaths(Collection)
     */
    public CompileKotlinOperation mainSourceFiles(Path... files) {
        ObjectTools.requireNonNull(files, MAIN_SOURCE_FILES);
        return mainSourceFilesPaths(List.of(files));
    }

    /**
     * Provides the main source files that should be compiled.
     *
     * @param files the main source files
     * @return this operation instance
     * @throws NullPointerException if {@code files} is {@code null}
     * @see #mainSourceFiles(File...)
     */
    public CompileKotlinOperation mainSourceFiles(Collection<File> files) {
        ObjectTools.requireNonNull(files, MAIN_SOURCE_FILES);
        mainSourceFiles_.addAll(files);
        return this;
    }

    /**
     * Provides the main source files that should be compiled.
     *
     * @param files the main source files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException if {@code files} is empty, or contains {@code null} elements
     * @see #mainSourceFiles(Path...)
     */
    public final CompileKotlinOperation mainSourceFilesPaths(Collection<Path> files) {
        ObjectTools.requireNotEmpty(files, "mainSourceFilesPaths");
        mainSourceFiles_.addAll(CollectionTools.combinePathsToFiles(files));
        return this;
    }

    /**
     * Provides the main source files that should be compiled.
     *
     * @param files the main source files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException if {@code files} is empty, or contains blank elements
     * @see #mainSourceFiles(String...)
     */
    public final CompileKotlinOperation mainSourceFilesStrings(Collection<String> files) {
        TextTools.requireNotBlank(files, "mainSourceFilesStrings");
        mainSourceFiles_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * Sets a consumer to receive output lines when not inheriting I/O.
     * <p>
     * Only called when {@link #isInheritIO()} is {@code false}. Default logs at INFO level.
     *
     * @param outputConsumer the output consumer, must not be null
     * @return this operation instance
     * @throws NullPointerException if outputConsumer is {@code null}
     */
    public CompileKotlinOperation outputConsumer(Consumer<String> outputConsumer) {
        ObjectTools.requireNonNull(outputConsumer, "outputConsumer");
        outputConsumer_ = outputConsumer;
        return this;
    }

    /**
     * Provides compiler plugins.
     *
     * @param directory the directory containing the plugin JARs
     * @param plugins   one or more plugins
     * @return this class instance
     * @throws NullPointerException     if {@code directory} or {@code plugins} is {@code null}
     * @throws IllegalArgumentException if {@code directory} is blank
     */
    public CompileKotlinOperation plugins(String directory, CompilerPlugin... plugins) {
        TextTools.requireNotBlank(directory, "plugins directory");
        Objects.requireNonNull(plugins, PLUGINS);
        return plugins(new File(directory), plugins);
    }

    /**
     * Provides compiler plugins.
     *
     * @param directory the directory containing the plugin JARs
     * @param plugins   one or more plugins
     * @return this class instance
     * @throws NullPointerException if {@code directory} or {@code plugins} is {@code null}
     */
    public CompileKotlinOperation plugins(File directory, CompilerPlugin... plugins) {
        ObjectTools.requireNonNull(directory, "'plugins directory' must not be null");
        ObjectTools.requireNonNull(plugins, PLUGINS);
        for (var p : plugins) {
            plugins_.add(new File(directory, p.getJar()).getAbsolutePath());
        }
        return this;
    }

    /**
     * Retrieves the compiler plugins.
     *
     * @return the compiler plugins
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Set<String> plugins() {
        return plugins_;
    }

    /**
     * Provides compiler plugins.
     *
     * @param plugins one or more plugins
     * @return this class instance
     * @throws NullPointerException     if {@code plugins} is {@code null}
     * @throws IllegalArgumentException if {@code plugins} is empty, or contains {@code null} or empty elements
     */
    public CompileKotlinOperation plugins(String... plugins) {
        ObjectTools.requireNotEmpty(plugins, PLUGINS);
        return plugins(List.of(plugins));
    }

    /**
     * Provides compiler plugins.
     *
     * @param plugins the compiler plugins
     * @return this class instance
     * @throws NullPointerException     if {@code plugins} is {@code null}
     * @throws IllegalArgumentException if {@code plugins} is empty, or contains blank elements
     */
    public final CompileKotlinOperation plugins(Collection<String> plugins) {
        TextTools.requireNotBlank(plugins, PLUGINS);
        plugins_.addAll(plugins);
        return this;
    }

    /**
     * Provides compiler plugins.
     *
     * @param directory the directory containing the plugin JARs
     * @param plugins   one or more plugins
     * @return this class instance
     * @throws NullPointerException if {@code directory} or {@code plugins} is {@code null}
     */
    public CompileKotlinOperation plugins(Path directory, CompilerPlugin... plugins) {
        Objects.requireNonNull(directory, "plugin directory must not be null");
        Objects.requireNonNull(plugins, PLUGINS);
        return plugins(directory.toFile(), plugins);
    }

    /**
     * Provides compiler plugins located in the {@link #kotlinHome()} lib directory.
     *
     * @param plugins one or more plugins
     * @return this class instance
     * @throws NullPointerException     if {@code plugins} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code plugins} is empty
     * @see #plugins(File, CompilerPlugin...)
     */
    public CompileKotlinOperation plugins(CompilerPlugin... plugins) {
        ObjectTools.requireNotEmpty(plugins, PLUGINS);
        for (var plugin : plugins) {
            plugins_.add(plugin.name());
        }
        return this;
    }

    /**
     * Retrieves the test source directories that should be compiled.
     *
     * @return the test source directories
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> testSourceDirectories() {
        return testSourceDirectories_;
    }

    /**
     * Provides test source directories that should be compiled.
     *
     * @param directories one or more test source directories
     * @return this operation instance
     * @throws NullPointerException if {@code directories} is {@code null}
     * @see #testSourceDirectories(Collection)
     */
    public CompileKotlinOperation testSourceDirectories(File... directories) {
        ObjectTools.requireNonNull(directories, TEST_SOURCE_DIRECTORIES);
        return testSourceDirectories(List.of(directories));
    }

    /**
     * Provides test source directories that should be compiled.
     *
     * @param directories one or more test source directories
     * @return this operation instance
     * @throws NullPointerException if {@code directories} is {@code null}
     * @see #testSourceDirectoriesPaths(Collection)
     */
    public CompileKotlinOperation testSourceDirectories(Path... directories) {
        ObjectTools.requireNonNull(directories, TEST_SOURCE_DIRECTORIES);
        return testSourceDirectoriesPaths(List.of(directories));
    }

    /**
     * Provides test source directories that should be compiled.
     *
     * @param directories one or more test source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null}
     * @throws IllegalArgumentException if {@code directories} is empty, or contains blank elements
     * @see #testSourceDirectoriesStrings(Collection)
     */
    public CompileKotlinOperation testSourceDirectories(String... directories) {
        TextTools.requireNotBlank("testSourceDirectories", directories);
        return testSourceDirectoriesStrings(List.of(directories));
    }

    /**
     * Provides the test source directories that should be compiled.
     *
     * @param directories the test source directories
     * @return this operation instance
     * @throws NullPointerException if {@code directories} is {@code null}
     * @see #testSourceDirectories(File...)
     */
    public final CompileKotlinOperation testSourceDirectories(Collection<File> directories) {
        ObjectTools.requireNonNull(directories, TEST_SOURCE_DIRECTORIES);
        testSourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Provides the test source directories that should be compiled.
     *
     * @param directories the test source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null}
     * @throws IllegalArgumentException if {@code directories} is empty, or contains {@code null} elements
     * @see #testSourceDirectories(Path...)
     */
    public final CompileKotlinOperation testSourceDirectoriesPaths(Collection<Path> directories) {
        ObjectTools.requireNotEmpty(directories, "testSourceDirectoriesPaths");
        testSourceDirectories_.addAll(CollectionTools.combinePathsToFiles(directories));
        return this;
    }

    /**
     * Provides the test source directories that should be compiled.
     *
     * @param directories the test source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null}
     * @throws IllegalArgumentException if {@code directories} is empty, or contains {@code null} or empty elements
     * @see #testSourceDirectories(String...)
     */
    public final CompileKotlinOperation testSourceDirectoriesStrings(Collection<String> directories) {
        TextTools.requireNotBlank(directories, "testSourceDirectoriesStrings");
        testSourceDirectories_.addAll(CollectionTools.combineStringsToFiles(directories));
        return this;
    }

    /**
     * Retrieves the test files that should be compiled.
     *
     * @return the test files
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> testSourceFiles() {
        return testSourceFiles_;
    }

    /**
     * Provides test source files that should be compiled.
     *
     * @param files one or more test source files
     * @return this operation instance
     * @throws NullPointerException if {@code files} is {@code null}
     * @see #testSourceFiles(Collection)
     */
    public CompileKotlinOperation testSourceFiles(File... files) {
        ObjectTools.requireNonNull(files, TEST_SOURCE_FILES);
        return testSourceFiles(List.of(files));
    }

    /**
     * Provides the test sources files that should be compiled.
     *
     * @param files one or more test source files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException if {@code files} is empty, or contains blank elements
     * @see #testSourceFilesStrings(Collection)
     */
    public CompileKotlinOperation testSourceFiles(String... files) {
        TextTools.requireNotBlank("testSourceFiles", files);
        return testSourceFilesStrings(List.of(files));
    }

    /**
     * Provides the test sources files that should be compiled.
     *
     * @param files one or more test source files
     * @return this operation instance
     * @throws NullPointerException if {@code files} is {@code null}
     * @see #testSourceFilesPaths(Collection)
     */
    public CompileKotlinOperation testSourceFiles(Path... files) {
        ObjectTools.requireNonNull(files, TEST_SOURCE_FILES);
        return testSourceFilesPaths(List.of(files));
    }

    /**
     * Provides the test source files that should be compiled.
     *
     * @param files the test source files
     * @return this operation instance
     * @throws NullPointerException if {@code files} is {@code null}
     * @see #testSourceFiles(File...)
     */
    public final CompileKotlinOperation testSourceFiles(Collection<File> files) {
        ObjectTools.requireNonNull(files, TEST_SOURCE_FILES);
        testSourceFiles_.addAll(files);
        return this;
    }

    /**
     * Provides the test source files that should be compiled.
     *
     * @param files the test source files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException if {@code files} is empty, or contains {@code null} elements
     * @see #testSourceFiles(Path...)
     */
    public final CompileKotlinOperation testSourceFilesPaths(Collection<Path> files) {
        ObjectTools.requireNotEmpty(files, "testSourceFilesPaths");
        testSourceFiles_.addAll(CollectionTools.combinePathsToFiles(files));
        return this;
    }

    /**
     * Provides the test source files that should be compiled.
     *
     * @param files the test source files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException if {@code files} is empty, or contains blank elements
     * @see #testSourceFiles(String...)
     */
    public final CompileKotlinOperation testSourceFilesStrings(Collection<String> files) {
        TextTools.requireNotBlank(files, "testSourceFilesStrings");
        testSourceFiles_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * Sets the timeout for the Kotlin compiler execution in seconds.
     * <p>
     * If the process does not complete within the specified timeout, it will be terminated
     * and the operation will fail. If set to any negative value, the process will wait indefinitely.
     * Passing {@code 0} is not allowed; use a negative value to indicate no timeout.
     * <p>
     * Default is {@code 600} seconds (10 minutes)
     *
     * @param seconds the timeout in seconds (positive); use a negative value for no timeout
     * @return this operation instance
     * @throws IllegalArgumentException if {@code seconds} is {@code 0}
     * @since 1.2
     */
    public CompileKotlinOperation timeout(long seconds) {
        if (seconds == 0) {
            throw new IllegalArgumentException(
                    "timeout must be a positive number of seconds, or negative for no timeout; 0 is not allowed");
        }
        timeout_ = seconds;
        return this;
    }

    /**
     * Retrieves the timeout for the Kotlin compiler execution in seconds.
     * <p>
     * A positive value is the timeout duration in seconds. A negative value indicates no timeout
     * (wait indefinitely). {@code 0} is not a valid state; the setter disallows it.
     *
     * @return the timeout in seconds (positive), or a negative value if no timeout is set
     * @since 1.2
     */
    public long timeout() {
        return timeout_;
    }

    /**
     * Provides the working directory if it differs from the project's directory.
     *
     * @param dir the directory
     * @return this operation instance
     * @throws NullPointerException if {@code dir} is {@code null}
     */
    public CompileKotlinOperation workDir(File dir) {
        workDir_ = ObjectTools.requireNonNull(dir, WORK_DIR);
        return this;
    }

    /**
     * Provides the working directory if it differs from the project's directory.
     *
     * @param dir the directory
     * @return this operation instance
     * @throws NullPointerException if {@code dir} is {@code null}
     */
    public CompileKotlinOperation workDir(Path dir) {
        ObjectTools.requireNonNull(dir, WORK_DIR);
        return workDir(dir.toFile());
    }

    /**
     * Provides the working directory if it differs from the project's directory.
     *
     * @param dir the directory path
     * @return this operation instance
     * @throws NullPointerException     if {@code dir} is {@code null}
     * @throws IllegalArgumentException if {@code dir} is blank
     */
    public CompileKotlinOperation workDir(String dir) {
        TextTools.requireNotBlank(dir, WORK_DIR);
        return workDir(new File(dir));
    }

    /**
     * Retrieves the working directory.
     *
     * @return the directory
     */
    @Nullable
    public File workDir() {
        return workDir_;
    }

    /**
     * Part of the {@link #execute execute} operation, builds the main sources.
     * <p>
     * Copies the configured classpath and sources to prevent mutation of operation state across
     * multiple {@link #execute()} invocations.
     *
     * @throws ExitStatusException if an error occurs
     */
    @SuppressWarnings("PMD.SystemPrintln")
    protected void executeBuildMainSources() throws ExitStatusException {
        if (!silent()) {
            System.out.println("Compiling Kotlin main sources...");
        }

        var classpath = new LinkedHashSet<>(compileMainClasspath_);
        var sources = CollectionTools.combine(mainSourceFiles_, mainSourceDirectories_);

        executeBuildSources(classpath, sources, buildMainDirectory_, null);
    }

    /**
     * Part of the {@link #execute execute} operation, build sources to a given destination.
     *
     * @param classpath   the classpath list used for the compilation
     * @param sources     the source files to compile
     * @param destination the destination directory
     * @param friendPaths the output directory for friendly modules
     * @throws ExitStatusException if an error occurs
     */
    @SuppressWarnings({"PMD.PreserveStackTrace"})
    @SuppressFBWarnings({"RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "LEST_LOST_EXCEPTION_STACK_TRACE"})
    protected void executeBuildSources(@Nullable Collection<String> classpath,
                                       Collection<File> sources,
                                       @Nullable File destination,
                                       @Nullable File friendPaths)
            throws ExitStatusException {

        ObjectTools.requireNonNull(workDir_, WORK_DIR);

        if (sources.isEmpty()) {
            if (!silent() && logger.isLoggable(Level.WARNING)) {
                logger.warning("Nothing to compile.");
            }
            return;
        } else if (destination == null) {
            if (!silent() && logger.isLoggable(Level.SEVERE)) {
                logger.severe("No destination specified.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }

        var command = new ArrayList<String>(4);
        var args = new ArrayList<String>(
                (classpath != null ? classpath.size() : 0) + sources.size() + 16);

        // kotlinc — resolve once and cache across main + test compilations
        if (kotlinCompiler_ != null) {
            command.add(kotlinCompiler_.getAbsolutePath());
        } else if (kotlinHome_ != null) {
            var kotlinc = findKotlinCompilerInDir(kotlinHome_.getAbsolutePath());
            if (kotlinc != null) {
                command.add(kotlinc);
            } else {
                if (!silent() && logger.isLoggable(Level.SEVERE)) {
                    logger.severe("Could not locate Kotlin compiler in: " + kotlinHome_);
                }
                throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
            }
        } else {
            if (resolvedKotlinCompilerPath_ == null) {
                resolvedKotlinCompilerPath_ = findKotlinCompilerPath(silent());
            }
            command.add(resolvedKotlinCompilerPath_);
        }

        // JVM options
        if (jvmOptions_ != null) {
            jvmOptions_.args().forEach(s -> command.add("-J" + s));
        }

        // classpath
        var cp = new ArrayList<String>();
        if (ObjectTools.isNotEmpty(classpath)) {
            cp.addAll(classpath);
        }
        if (compileOptions_ != null && ObjectTools.isNotEmpty(compileOptions_.classpath())) {
            compileOptions_.classpath().forEach(f -> cp.add(f.getAbsolutePath()));
        }
        if (!cp.isEmpty()) {
            args.add("-cp");
            args.add(FileUtils.joinPaths(cp));
        }

        // compile options
        if (compileOptions_ != null) {
            args.addAll(compileOptions_.args());
        }

        // destination
        args.add("-d");
        args.add(destination.getAbsolutePath());

        // friend-path
        if (IOTools.exists(friendPaths)) {
            args.add("-Xfriend-paths=" + friendPaths.getAbsolutePath());
        }

        if (!plugins_.isEmpty()) {
            var kotlinHomePath = findKotlinHome();

            plugins_.forEach(p -> {
                File pluginJar = null;

                // Try as enum first
                try {
                    var pluginValue = CompilerPlugin.valueOf(p);
                    if (kotlinHomePath != null) {
                        pluginJar = IOTools.resolveFile(kotlinHomePath, "lib", pluginValue.getJar());
                    } else if (!silent() && logger.isLoggable(Level.WARNING)) {
                        logger.warning("The Kotlin home must be set to specify the '"
                                + CompilerPlugin.class.getSimpleName() + '.' + pluginValue.name()
                                + "' compiler plugin.");
                    }
                } catch (IllegalArgumentException ignored) {
                    // Try as a direct file path
                    pluginJar = new File(p);
                }

                if (pluginJar != null) {
                    if (pluginJar.exists()) {
                        args.add("-Xplugin=" + pluginJar.getAbsolutePath());
                    } else if (!silent() && logger.isLoggable(Level.WARNING)) {
                        logger.warning("Could not locate compiler plugin: " + pluginJar.getAbsolutePath());
                    }
                }
            });
        }

        // sources
        sources.forEach(f -> args.add(f.getAbsolutePath()));

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(PathTools.formatCommandLine(CollectionTools.combine(command, args)));
        }

        File argsFile = null;
        try {
            argsFile = File.createTempFile("bld-kotlinc-", ".args");
            argsFile.deleteOnExit();

            Files.writeString(argsFile.toPath(), String.join(System.lineSeparator(), args));

            command.add("@" + argsFile.getAbsolutePath());

            var executor = new ProcessExecutor()
                    .command(command)
                    .workDir(workDir_)
                    .timeout(timeout_)
                    .inheritIO(inheritIO_);

            if (!env_.isEmpty()) {
                executor.env(env_);
            }

            if (!inheritIO_) {
                executor.outputConsumer(outputConsumer_);
            }

            var result = executor.execute();

            if (result.timedOut()) {
                if (logger.isLoggable(Level.SEVERE) && !silent()) {
                    logger.severe("Kotlin compile execution timed out after " + timeout_ + " seconds.");
                }
                throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
            } else {
                ExitStatusException.throwOnFailure(result.exitCode());
            }
        } catch (IOException e) {
            if (logger.isLoggable(Level.SEVERE) && !silent()) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (logger.isLoggable(Level.SEVERE) && !silent()) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } finally {
            if (argsFile != null) {
                //noinspection ResultOfMethodCallIgnored
                argsFile.delete();
            }
        }
    }

    /**
     * Part of the {@link #execute execute} operation, builds the test sources.
     * <p>
     * Copies the configured classpath and sources to prevent mutation of operation state across
     * multiple {@link #execute()} invocations. Test compilation uses the main build directory
     * as a friend path for module visibility.
     *
     * @throws ExitStatusException if an error occurs
     */
    @SuppressWarnings("PMD.SystemPrintln")
    protected void executeBuildTestSources() throws ExitStatusException {
        if (!silent()) {
            System.out.println("Compiling Kotlin test sources...");
        }

        var classpath = new LinkedHashSet<>(compileTestClasspath_);
        var sources = CollectionTools.combine(testSourceFiles_, testSourceDirectories_);

        executeBuildSources(classpath, sources, buildTestDirectory_, buildMainDirectory_);
    }

    /**
     * Part of the {@link #execute execute} operation, creates the build directories.
     *
     * @throws IOException if an error occurs
     */
    protected void executeCreateBuildDirectories() throws IOException {
        if (buildMainDirectory_ == null) {
            throw new IOException("buildMainDirectory must be set");
        }
        if (!IOTools.mkdirs(buildMainDirectory_)) {
            throw new IOException("Could not create build main directory: " + buildMainDirectory_.getAbsolutePath());
        }
        if (buildTestDirectory_ == null) {
            throw new IOException("buildTestDirectory must be set");
        }
        if (!IOTools.mkdirs(buildTestDirectory_)) {
            throw new IOException("Could not create build test directory: " + buildTestDirectory_.getAbsolutePath());
        }
    }

    @Nullable
    private File findKotlinHome() {
        if (kotlinHomeResolved_) {
            return resolvedKotlinHome_;
        }
        resolvedKotlinHome_ = resolveKotlinHome();
        kotlinHomeResolved_ = true;
        return resolvedKotlinHome_;
    }

    @Nullable
    private File resolveKotlinHome() {
        if (kotlinHome_ != null) {
            return kotlinHome_;
        }

        var kotlinHome = System.getenv("KOTLIN_HOME");
        if (kotlinHome != null) {
            return new File(kotlinHome);
        }

        if (kotlinCompiler_ != null) {
            var parent = kotlinCompiler_.getParentFile();
            if (IOTools.isDirectory(parent)) {
                if ("bin".equals(parent.getName())) {
                    var binParent = parent.getParentFile();
                    if (IOTools.isDirectory(binParent)) {
                        return binParent.getParentFile();
                    }
                } else {
                    return parent;
                }
            }
        }
        return null;
    }
}