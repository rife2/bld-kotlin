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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Compiles main and test Kotlin sources in the relevant build directories.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressFBWarnings({"PATH_TRAVERSAL_IN"})
public class CompileKotlinOperation extends AbstractOperation<CompileKotlinOperation> {

    private static final String KOTLINC = "kotlinc";
    private static final String KOTLINC_EXECUTABLE = KOTLINC + (SystemTools.isWindows() ? ".bat" : "");
    private static final String MAIN_SOURCE_DIRECTORIES = "mainSourceDirectories";
    private static final String MAIN_SOURCE_FILES = "mainSourceFiles";
    private static final String PLUGINS = "plugins";
    private static final String TEST_SOURCE_DIRECTORIES = "testSourceDirectories";
    private static final String TEST_SOURCE_FILES = "testSourceFiles";
    private static final String WORK_DIR = "workDir";
    private static final Logger logger = Logger.getLogger(CompileKotlinOperation.class.getName());
    private final Set<String> compileMainClasspath_ = new LinkedHashSet<>();
    private final Set<String> compileTestClasspath_ = new LinkedHashSet<>();
    private final List<File> mainSourceDirectories_ = new ArrayList<>();
    private final List<File> mainSourceFiles_ = new ArrayList<>();
    private final Set<String> plugins_ = new LinkedHashSet<>();
    private final List<File> testSourceDirectories_ = new ArrayList<>();
    private final List<File> testSourceFiles_ = new ArrayList<>();
    private File buildMainDirectory_;
    private File buildTestDirectory_;
    private CompileOptions compileOptions_ = new CompileOptions();
    private JvmOptions jvmOptions_ = new JvmOptions();
    private boolean kotlinHomeResolved_;
    private File kotlinHome_;
    private File kotlinc_;
    private BaseProject project_;
    private File resolvedKotlinHome_;
    private String resolvedKotlincPath_;
    private File workDir_;

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

    private static String findKotlincInDir(String directory) {
        var kotlinc = new File(directory, KOTLINC_EXECUTABLE);

        if (IOTools.canExecute(kotlinc)) {
            return kotlinc.getAbsolutePath();
        }

        // Check the bin subdirectory if it exists
        var binDir = new File(directory, "bin");
        if (binDir.isDirectory()) {
            kotlinc = new File(binDir, KOTLINC_EXECUTABLE);
            if (IOTools.canExecute(kotlinc)) {
                return kotlinc.getAbsolutePath();
            }
        }

        return null;
    }

    /**
     * Locates the Kotlin compiler (kotlinc) executable.
     *
     * @return The path to the kotlinc executable, or {@code kotlinc}/{@code kotlinc.bat} if not found.
     * @since 1.1.0
     */
    public static String findKotlincPath() {
        return findKotlincPath(false);
    }

    /**
     * Locates the Kotlin compiler (kotlinc) executable.
     *
     * @param isSilent do not log the path to the kotlinc executable, if {@code true}
     * @return The path to the kotlinc executable, or {@code kotlinc}/{@code kotlinc.bat} if not found.
     * @since 1.1.0
     */
    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    protected static String findKotlincPath(boolean isSilent) {
        String kotlincPath;

        // Check the KOTLIN_HOME environment variable first
        var kotlinHome = System.getenv("KOTLIN_HOME");
        if (TextTools.isNotEmpty(kotlinHome)) {
            kotlincPath = findKotlincInDir(kotlinHome);
            if (kotlincPath != null) {
                logKotlincPath(kotlincPath, isSilent, "KOTLIN_HOME");
                return kotlincPath;
            }
        }

        // Check PATH environment variable
        var pathEnv = System.getenv("PATH");
        if (TextTools.isNotEmpty(pathEnv)) {
            var pathDirs = pathEnv.split(File.pathSeparator);
            for (var dir : pathDirs) {
                kotlincPath = findKotlincInDir(dir);
                if (kotlincPath != null) {
                    logKotlincPath(kotlincPath, isSilent, "PATH");
                    return kotlincPath;
                }
            }
        }

        // Common installation paths (e.g., SDKMAN!, IntelliJ IDEA, etc.)
        var commonPaths = new LinkedHashMap<String, String>();

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
            kotlincPath = findKotlincInDir(path.getKey());
            if (kotlincPath != null) {
                logKotlincPath(kotlincPath, isSilent, commonPaths.get(path.getKey()));
                return kotlincPath;
            }
        }

        // Try 'which' or 'where' commands (less reliable but sometimes works)
        try {
            Process process;
            if (SystemTools.isWindows()) {
                process = Runtime.getRuntime().exec("where " + KOTLINC);
            } else {
                process = Runtime.getRuntime().exec("which " + KOTLINC);
            }

            try (var scanner = new Scanner(process.getInputStream())) {
                if (scanner.hasNextLine()) {
                    kotlincPath = scanner.nextLine().trim();
                    if (IOTools.canExecute(new File(kotlincPath))) {
                        logKotlincPath(kotlincPath, isSilent);
                        return kotlincPath;
                    }
                }
            }
        } catch (IOException | SecurityException | IllegalArgumentException ignored) {
            // Ignore exceptions from which/where, as they might not be available
        }

        return KOTLINC_EXECUTABLE;
    }

    private static void logKotlincPath(@NonNull String kotlincPath, boolean isSilent) {
        logKotlincPath(kotlincPath, isSilent, null);
    }

    private static void logKotlincPath(@NonNull String kotlincPath, boolean isSilent, String from) {
        if (logger.isLoggable(Level.INFO) && !isSilent) {
            if (from != null) {
                logger.info("Using Kotlin compiler inferred from " + from + ": " + kotlincPath);
            } else {
                logger.info("Using Kotlin compiler found at: " + kotlincPath);
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
    public CompileKotlinOperation buildMainDirectory(@NonNull Path directory) {
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
    public CompileKotlinOperation buildMainDirectory(@NonNull File directory) {
        buildMainDirectory_ = ObjectTools.requireNonNull(directory, "buildMainDirectory");

        return this;
    }

    /**
     * Provides the main build destination directory.
     *
     * @param directory the directory to use for the main build destination
     * @return this operation instance
     * @throws NullPointerException     if {@code directory} is {@code null}
     * @throws IllegalArgumentException if {@code directory} is empty
     */
    public CompileKotlinOperation buildMainDirectory(@NonNull String directory) {
        ObjectTools.requireNotEmpty(directory, "buildMainDirectory");
        return buildMainDirectory(new File(directory));
    }

    /**
     * Retrieves the main build destination directory.
     *
     * @return the main build directory
     */
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
    public CompileKotlinOperation buildTestDirectory(@NonNull File directory) {
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
    public CompileKotlinOperation buildTestDirectory(@NonNull Path directory) {
        ObjectTools.requireNonNull(directory, "buildTestDirectory");
        return buildTestDirectory(directory.toFile());
    }

    /**
     * Provides the test build destination directory.
     *
     * @param directory the directory to use for the test build destination
     * @return this operation instance
     * @throws NullPointerException     if {@code directory} is {@code null}
     * @throws IllegalArgumentException if {@code directory} is empty
     */
    public CompileKotlinOperation buildTestDirectory(@NonNull String directory) {
        ObjectTools.requireNotEmpty(directory, "buildTestDirectory");
        return buildTestDirectory(new File(directory));
    }

    /**
     * Retrieves the test build destination directory.
     *
     * @return the test build directory
     */
    public File buildTestDirectory() {
        return buildTestDirectory_;
    }

    /**
     * Provides entries for the main compilation classpath.
     *
     * @param classpath one or more classpath entries
     * @return this operation instance
     * @throws NullPointerException     if {@code classpath} is {@code null}
     * @throws IllegalArgumentException if {@code classpath} is empty, or contains {@code null} or empty elements
     * @see #compileMainClasspath(Collection)
     */
    public CompileKotlinOperation compileMainClasspath(@NonNull String... classpath) {
        ObjectTools.requireNotEmpty(classpath, "compileMainClasspath");
        return compileMainClasspath(List.of(classpath));
    }

    /**
     * Provides the entries for the main compilation classpath.
     *
     * @param classpath the classpath entries
     * @return this operation instance
     * @throws NullPointerException     if {@code classpath} is {@code null}
     * @throws IllegalArgumentException if {@code classpath} is empty, or contains {@code null} or empty elements
     */
    public final CompileKotlinOperation compileMainClasspath(@NonNull Collection<String> classpath) {
        ObjectTools.requireNotEmpty(classpath, "compileMainClasspath");
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
    public CompileKotlinOperation compileOptions(@NonNull CompileOptions options) {
        compileOptions_ = ObjectTools.requireNonNull(options, "compileOptions");
        return this;
    }

    /**
     * Provides entries for the test compilation classpath.
     *
     * @param classpath one or more classpath entries
     * @return this operation instance
     * @throws NullPointerException     if {@code classpath} is {@code null}
     * @throws IllegalArgumentException if {@code classpath} is empty, or contains {@code null} or empty elements
     */
    public CompileKotlinOperation compileTestClasspath(@NonNull String... classpath) {
        ObjectTools.requireNotEmpty(classpath, "compileTestClasspath");
        return compileTestClasspath(List.of(classpath));
    }

    /**
     * Provides the entries for the test compilation classpath.
     *
     * @param classpath the classpath entries
     * @return this operation instance
     * @throws NullPointerException     if {@code classpath} is {@code null}
     * @throws IllegalArgumentException if {@code classpath} is empty, or contains {@code null} or empty elements
     */
    public final CompileKotlinOperation compileTestClasspath(@NonNull Collection<String> classpath) {
        ObjectTools.requireNotEmpty(classpath, "compileTestClasspath");
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
    public CompileKotlinOperation fromProject(@NonNull BaseProject project) {
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

        if (project.javaRelease() != null) {
            if (!compileOptions_.hasRelease()) {
                compileOptions_.jdkRelease(project.javaRelease());
            }
            if (!compileOptions_.hasTarget()) {
                compileOptions_.jvmTarget(project.javaRelease());
            }
        }

        compileOptions_.noStdLib(true);

        return this;
    }

    /**
     * Retrieves the Java Virtual Machine options.
     *
     * @return the JVM options
     */
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
    public CompileKotlinOperation jvmOptions(@NonNull JvmOptions options) {
        jvmOptions_ = ObjectTools.requireNonNull(options, "jvmOptions");
        return this;
    }

    /**
     * Provides the Kotlin home directory, if it differs from the default {@code KOTLIN_HOME}.
     *
     * @param dir the directory path
     * @return this operation instance
     * @throws NullPointerException     if {@code dir} is {@code null}
     * @throws IllegalArgumentException if {@code dir} is empty
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public CompileKotlinOperation kotlinHome(@NonNull String dir) {
        ObjectTools.requireNotEmpty(dir, "kotlinHome");
        return kotlinHome(new File(dir));
    }

    /**
     * Provides the Kotlin home directory, if it differs from the default {@code KOTLIN_HOME}.
     *
     * @param dir the directory
     * @return this operation instance
     * @throws NullPointerException if {@code dir} is {@code null}
     */
    public CompileKotlinOperation kotlinHome(@NonNull File dir) {
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
    public CompileKotlinOperation kotlinHome(@NonNull Path dir) {
        ObjectTools.requireNonNull(dir, "kotlinHome");
        return kotlinHome(dir.toFile());
    }

    /**
     * Retrieves the Kotlin home directory.
     *
     * @return the directory
     */
    public File kotlinHome() {
        return kotlinHome_;
    }

    /**
     * Provides the path to the Kotlin compiler ({@code kotlinc}) executable, if not in {@link #kotlinHome()}.
     *
     * @param executable the executable path
     * @return this operation instance
     * @throws NullPointerException     if {@code executable} is {@code null}
     * @throws IllegalArgumentException if {@code executable} is empty
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public CompileKotlinOperation kotlinc(@NonNull String executable) {
        ObjectTools.requireNotEmpty(executable, KOTLINC);
        return kotlinc(new File(executable));
    }

    /**
     * Retrieves the path to the Kotlin compiler ({@code kotlinc}) executable, if not in {@link #kotlinHome()}.
     *
     * @return the executable path
     */
    public File kotlinc() {
        return kotlinc_;
    }

    /**
     * Provides the path to the Kotlin compiler ({@code kotlinc}) executable, if not in {@link #kotlinHome()}.
     *
     * @param executable the executable path
     * @return this operation instance
     * @throws NullPointerException if {@code executable} is {@code null}
     */
    public CompileKotlinOperation kotlinc(@NonNull File executable) {
        kotlinc_ = ObjectTools.requireNonNull(executable, KOTLINC);
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
    public CompileKotlinOperation kotlinc(@NonNull Path executable) {
        ObjectTools.requireNonNull(executable, KOTLINC);
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
    public CompileKotlinOperation mainSourceDirectories(@NonNull File... directories) {
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
    public CompileKotlinOperation mainSourceDirectories(@NonNull Path... directories) {
        ObjectTools.requireNonNull(directories, MAIN_SOURCE_DIRECTORIES);
        return mainSourceDirectoriesPaths(List.of(directories));
    }

    /**
     * Provides main source directories that should be compiled.
     *
     * @param directories one or more main source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null}
     * @throws IllegalArgumentException if {@code directories} is empty, or contains {@code null} or empty elements
     * @see #mainSourceDirectoriesStrings(Collection)
     */
    public CompileKotlinOperation mainSourceDirectories(@NonNull String... directories) {
        ObjectTools.requireNotEmpty(directories, MAIN_SOURCE_DIRECTORIES);
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
    public final CompileKotlinOperation mainSourceDirectories(@NonNull Collection<File> directories) {
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
    public final CompileKotlinOperation mainSourceDirectoriesPaths(@NonNull Collection<Path> directories) {
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
     * @throws IllegalArgumentException if {@code directories} is empty, or contains {@code null} or empty elements
     * @see #mainSourceDirectories(String...)
     */
    public final CompileKotlinOperation mainSourceDirectoriesStrings(@NonNull Collection<String> directories) {
        ObjectTools.requireNotEmpty(directories, "mainSourceDirectoriesStrings");
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
    public CompileKotlinOperation mainSourceFiles(@NonNull File... files) {
        ObjectTools.requireNonNull(files, MAIN_SOURCE_FILES);
        return mainSourceFiles(List.of(files));
    }

    /**
     * Provides main source files that should be compiled.
     *
     * @param files one or more main source files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException if {@code files} is empty, or contains {@code null} or empty elements
     * @see #mainSourceFilesStrings(Collection)
     */
    public CompileKotlinOperation mainSourceFiles(@NonNull String... files) {
        ObjectTools.requireNotEmpty(files, MAIN_SOURCE_FILES);
        return mainSourceFilesStrings(List.of(files));
    }

    /**
     * Provides main source files that should be compiled.
     *
     * @param files one or more main source files
     * @return this operation instance
     * @throws NullPointerException if {@code files} is {@code null}
     * @see #mainSourceFilesPaths(Collection)
     */
    public CompileKotlinOperation mainSourceFiles(@NonNull Path... files) {
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
    public CompileKotlinOperation mainSourceFiles(@NonNull Collection<File> files) {
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
    public final CompileKotlinOperation mainSourceFilesPaths(@NonNull Collection<Path> files) {
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
     * @throws IllegalArgumentException if {@code files} is empty, or contains {@code null} or empty elements
     * @see #mainSourceFiles(String...)
     */
    public final CompileKotlinOperation mainSourceFilesStrings(@NonNull Collection<String> files) {
        ObjectTools.requireNotEmpty(files, "mainSourceFilesStrings");
        mainSourceFiles_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * Provides compiler plugins.
     *
     * @param directory the directory containing the plugin JARs
     * @param plugins   one or more plugins
     * @return this class instance
     * @throws NullPointerException     if {@code directory} or {@code plugins} is {@code null}
     * @throws IllegalArgumentException if {@code directory} is empty
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public CompileKotlinOperation plugins(@NonNull String directory, @NonNull CompilerPlugin... plugins) {
        ObjectTools.requireNotEmpty(directory, "plugins directory");
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
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public CompileKotlinOperation plugins(@NonNull File directory, @NonNull CompilerPlugin... plugins) {
        Objects.requireNonNull(directory, "'plugins directory' must not be null");
        Objects.requireNonNull(plugins, PLUGINS);
        for (var p : plugins) {
            if (p == null) {
                throw new IllegalArgumentException("'plugins' elements should not be null");
            }
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
    public CompileKotlinOperation plugins(@NonNull String... plugins) {
        ObjectTools.requireNotEmpty(plugins, PLUGINS);
        return plugins(List.of(plugins));
    }

    /**
     * Provides compiler plugins.
     *
     * @param plugins the compiler plugins
     * @return this class instance
     * @throws NullPointerException     if {@code plugins} is {@code null}
     * @throws IllegalArgumentException if {@code plugins} is empty, or contains {@code null} or empty elements
     */
    public final CompileKotlinOperation plugins(@NonNull Collection<String> plugins) {
        ObjectTools.requireNotEmpty(plugins, PLUGINS);
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
    public CompileKotlinOperation plugins(@NonNull Path directory, @NonNull CompilerPlugin... plugins) {
        Objects.requireNonNull(directory, "plugin directory must not be null");
        Objects.requireNonNull(plugins, PLUGINS);
        return plugins(directory.toFile(), plugins);
    }

    /**
     * Provides compiler plugins located in the {@link #kotlinHome()} lib directory.
     *
     * @param plugins one or more plugins
     * @return this class instance
     * @throws NullPointerException     if {@code plugins} is {@code null}
     * @throws IllegalArgumentException if {@code plugins} is empty, or contains {@code null} elements
     * @see #plugins(File, CompilerPlugin...)
     */
    public CompileKotlinOperation plugins(@NonNull CompilerPlugin... plugins) {
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
    public CompileKotlinOperation testSourceDirectories(@NonNull File... directories) {
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
    public CompileKotlinOperation testSourceDirectories(@NonNull Path... directories) {
        ObjectTools.requireNonNull(directories, TEST_SOURCE_DIRECTORIES);
        return testSourceDirectoriesPaths(List.of(directories));
    }

    /**
     * Provides test source directories that should be compiled.
     *
     * @param directories one or more test source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null}
     * @throws IllegalArgumentException if {@code directories} is empty, or contains {@code null} or empty elements
     * @see #testSourceDirectoriesStrings(Collection)
     */
    public CompileKotlinOperation testSourceDirectories(@NonNull String... directories) {
        ObjectTools.requireNotEmpty(directories, TEST_SOURCE_DIRECTORIES);
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
    public final CompileKotlinOperation testSourceDirectories(@NonNull Collection<File> directories) {
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
    public final CompileKotlinOperation testSourceDirectoriesPaths(@NonNull Collection<Path> directories) {
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
    public final CompileKotlinOperation testSourceDirectoriesStrings(@NonNull Collection<String> directories) {
        ObjectTools.requireNotEmpty(directories, "testSourceDirectoriesStrings");
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
    public CompileKotlinOperation testSourceFiles(@NonNull File... files) {
        ObjectTools.requireNonNull(files, TEST_SOURCE_FILES);
        return testSourceFiles(List.of(files));
    }

    /**
     * Provides the test sources files that should be compiled.
     *
     * @param files one or more test source files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException if {@code files} is empty, or contains {@code null} or empty elements
     * @see #testSourceFilesStrings(Collection)
     */
    public CompileKotlinOperation testSourceFiles(@NonNull String... files) {
        ObjectTools.requireNotEmpty(files, TEST_SOURCE_FILES);
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
    public CompileKotlinOperation testSourceFiles(@NonNull Path... files) {
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
    public final CompileKotlinOperation testSourceFiles(@NonNull Collection<File> files) {
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
    public final CompileKotlinOperation testSourceFilesPaths(@NonNull Collection<Path> files) {
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
     * @throws IllegalArgumentException if {@code files} is empty, or contains {@code null} or empty elements
     * @see #testSourceFiles(String...)
     */
    public final CompileKotlinOperation testSourceFilesStrings(@NonNull Collection<String> files) {
        ObjectTools.requireNotEmpty(files, "testSourceFilesStrings");
        testSourceFiles_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * Provides the working directory if it differs from the project's directory.
     *
     * @param dir the directory
     * @return this operation instance
     * @throws NullPointerException if {@code dir} is {@code null}
     */
    public CompileKotlinOperation workDir(@NonNull File dir) {
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
    public CompileKotlinOperation workDir(@NonNull Path dir) {
        ObjectTools.requireNonNull(dir, WORK_DIR);
        return workDir(dir.toFile());
    }

    /**
     * Provides the working directory if it differs from the project's directory.
     *
     * @param dir the directory path
     * @return this operation instance
     * @throws NullPointerException     if {@code dir} is {@code null}
     * @throws IllegalArgumentException if {@code dir} is empty
     */
    public CompileKotlinOperation workDir(@NonNull String dir) {
        ObjectTools.requireNotEmpty(dir, WORK_DIR);
        return workDir(new File(dir));
    }

    /**
     * Retrieves the working directory.
     *
     * @return the directory
     */
    public File workDir() {
        return workDir_;
    }

    private String cleanPath(@NonNull File path) {
        ObjectTools.requireNonNull(path, "cleanPath");
        return cleanPath(path.getAbsolutePath());
    }

    private String cleanPath(@NonNull String path) {
        ObjectTools.requireNotEmpty(path, "cleanPath");
        if (SystemTools.isWindows()) {
            return path.replace("\\", "\\\\");
        }
        return path;
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
            System.out.println("Compiling Kotlin main sources.");
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
    @SuppressFBWarnings({"COMMAND_INJECTION", "LEST_LOST_EXCEPTION_STACK_TRACE", "MDM_STRING_BYTES_ENCODING",
            "DM_DEFAULT_ENCODING", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"})
    protected void executeBuildSources(Collection<String> classpath, Collection<File> sources, File destination,
                                       File friendPaths)
            throws ExitStatusException {

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
        if (kotlinc_ != null) {
            command.add(kotlinc_.getAbsolutePath());
        } else if (kotlinHome_ != null) {
            var kotlinc = findKotlincInDir(kotlinHome_.getAbsolutePath());
            if (kotlinc != null) {
                command.add(kotlinc);
            } else {
                if (!silent() && logger.isLoggable(Level.SEVERE)) {
                    logger.severe("Could not locate Kotlin compiler in: " + kotlinHome_);
                }
                throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
            }
        } else {
            if (resolvedKotlincPath_ == null) {
                resolvedKotlincPath_ = findKotlincPath(silent());
            }
            command.add(resolvedKotlincPath_);
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
            args.add('"' + FileUtils.joinPaths(cp.stream().map(this::cleanPath).toList()) + '"');
        }

        // compile options
        if (compileOptions_ != null) {
            args.addAll(compileOptions_.args());
        }

        // destination
        args.add("-d");
        args.add('"' + cleanPath(destination) + '"');

        // friend-path
        if (IOTools.exists(friendPaths)) {
            args.add("-Xfriend-paths=\"" + cleanPath(friendPaths) + '"');
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
                        args.add("-Xplugin=\"" + cleanPath(pluginJar) + '"');
                    } else if (!silent() && logger.isLoggable(Level.WARNING)) {
                        logger.warning("Could not locate compiler plugin: " + pluginJar.getAbsolutePath());
                    }
                }
            });
        }

        // sources
        sources.forEach(f -> args.add('"' + cleanPath(f) + '"'));

        var argsLine = String.join(" ", args);

        // log the command line
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.join(" ", command) + " " + argsLine);
        }

        File argsFile = null;
        try {
            argsFile = File.createTempFile("bld-kotlinc-", ".args");
            argsFile.deleteOnExit();

            Files.write(argsFile.toPath(), argsLine.getBytes());

            command.add("@" + argsFile.getAbsolutePath());

            // run the command
            var pb = new ProcessBuilder();
            pb.inheritIO();
            pb.command(command);
            pb.directory(workDir_);

            @SuppressWarnings("PMD.CloseResource")
            var proc = pb.start();
            try {
                proc.waitFor();
                ExitStatusException.throwOnFailure(proc.exitValue());
            } finally {
                if (proc.isAlive()) {
                    proc.destroyForcibly();
                }
            }
        } catch (IOException | InterruptedException e) {
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
            System.out.println("Compiling Kotlin test sources.");
        }

        var classpath = new LinkedHashSet<>(compileTestClasspath_);
        var sources = CollectionTools.combine(testSourceFiles_, testSourceDirectories_);

        executeBuildSources(classpath, sources, buildTestDirectory_, buildMainDirectory());
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

    private File findKotlinHome() {
        if (kotlinHomeResolved_) {
            return resolvedKotlinHome_;
        }
        resolvedKotlinHome_ = resolveKotlinHome();
        kotlinHomeResolved_ = true;
        return resolvedKotlinHome_;
    }

    private File resolveKotlinHome() {
        if (kotlinHome_ != null) {
            return kotlinHome_;
        }

        // Deduct from KOTLIN_HOME environment variable
        var kotlinHome = System.getenv("KOTLIN_HOME");
        if (kotlinHome != null) {
            return new File(kotlinHome);
        }

        // Deduct from kotlinc location if provided
        if (kotlinc_ != null) {
            var parent = kotlinc_.getParentFile();
            if (IOTools.isDirectory(parent)) {
                if (parent.getPath().endsWith("bin")) {
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