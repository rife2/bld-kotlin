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
import rife.bld.BaseProject;
import rife.bld.extension.kotlin.CompileOptions;
import rife.bld.extension.kotlin.CompilerPlugin;
import rife.bld.extension.kotlin.JvmOptions;
import rife.bld.extension.tools.CollectionUtils;
import rife.bld.extension.tools.IOUtils;
import rife.bld.extension.tools.SystemUtils;
import rife.bld.extension.tools.TextUtils;
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

    private static final String KOTLINC_EXECUTABLE = "kotlinc" + (SystemUtils.isWindows() ? ".bat" : "");
    private static final Logger LOGGER = Logger.getLogger(CompileKotlinOperation.class.getName());
    private final List<String> compileMainClasspath_ = new ArrayList<>();
    private final List<String> compileTestClasspath_ = new ArrayList<>();
    private final JvmOptions jvmOptions_ = new JvmOptions();
    private final List<File> mainSourceDirectories_ = new ArrayList<>();
    private final List<File> mainSourceFiles_ = new ArrayList<>();
    private final List<String> plugins_ = new ArrayList<>();
    private final List<File> testSourceDirectories_ = new ArrayList<>();
    private final List<File> testSourceFiles_ = new ArrayList<>();
    private File buildMainDirectory_;
    private File buildTestDirectory_;
    private CompileOptions compileOptions_ = new CompileOptions();
    private File kotlinHome_;
    private File kotlinc_;
    private BaseProject project_;
    private File workDir_;

    /**
     * Performs the compile operation.
     */
    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public void execute() throws Exception {
        if (project_ == null) {
            if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.severe("A project must be specified.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } else if (!workDir_.isDirectory()) {
            if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.severe("Invalid working directory: " + workDir_.getAbsolutePath());
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }

        executeCreateBuildDirectories();
        executeBuildMainSources();
        executeBuildTestSources();

        if (!silent()) {
            System.out.println("Kotlin compilation finished successfully.");
        }
    }

    private static String findKotlincInDir(String directory) {
        var kotlinc = new File(directory, KOTLINC_EXECUTABLE);

        if (IOUtils.canExecute(kotlinc)) {
            return kotlinc.getAbsolutePath();
        }

        // Check the bin subdirectory if it exists
        var binDir = new File(directory, "bin");
        if (binDir.isDirectory()) {
            kotlinc = new File(binDir, KOTLINC_EXECUTABLE);
            if (IOUtils.canExecute(kotlinc)) {
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
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public static String findKotlincPath(boolean isSilent) {
        String kotlincPath;

        // Check the KOTLIN_HOME environment variable first
        var kotlinHome = System.getenv("KOTLIN_HOME");
        if (TextUtils.isNotEmpty(kotlinHome)) {
            kotlincPath = findKotlincInDir(kotlinHome);
            if (kotlincPath != null) {
                logKotlincPath(kotlincPath, isSilent, "KOTLIN_HOME");
                return kotlincPath;
            }
        }

        // Check PATH environment variable
        var pathEnv = System.getenv("PATH");
        if (TextUtils.isNotEmpty(pathEnv)) {
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

        if (SystemUtils.isLinux()) {
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
        } else if (SystemUtils.isWindows()) {
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
        } else if (SystemUtils.isMacOS()) {
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
            if (SystemUtils.isWindows()) {
                process = Runtime.getRuntime().exec("where kotlinc");
            } else {
                process = Runtime.getRuntime().exec("which kotlinc");
            }

            try (var scanner = new Scanner(process.getInputStream())) {
                if (scanner.hasNextLine()) {
                    kotlincPath = scanner.nextLine().trim();
                    if (IOUtils.canExecute(new File(kotlincPath))) {
                        logKotlincPath(kotlincPath, isSilent);
                        return kotlincPath;
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore exceptions from which/where, as they might not be available
        }

        return KOTLINC_EXECUTABLE;
    }

    private static void logKotlincPath(String kotlincPath, boolean isSilent) {
        logKotlincPath(kotlincPath, isSilent, null);
    }

    private static void logKotlincPath(String kotlincPath, boolean isSilent, String from) {
        if (LOGGER.isLoggable(Level.INFO) && !isSilent) {
            if (from != null) {
                LOGGER.info("Using Kotlin compiler inferred from " + from + ": " + kotlincPath);
            } else {
                LOGGER.info("Using Kotlin compiler found at: " + kotlincPath);
            }
        }
    }

    /**
     * Provides the main build destination directory.
     *
     * @param directory the directory to use for the main build destination
     * @return this operation instance
     */
    public CompileKotlinOperation buildMainDirectory(Path directory) {
        return buildMainDirectory(directory.toFile());
    }

    /**
     * Provides the main build destination directory.
     *
     * @param directory the directory to use for the main build destination
     * @return this operation instance
     */
    public CompileKotlinOperation buildMainDirectory(File directory) {
        buildMainDirectory_ = directory;
        return this;
    }

    /**
     * Provides the main build destination directory.
     *
     * @param directory the directory to use for the main build destination
     * @return this operation instance
     */
    public CompileKotlinOperation buildMainDirectory(String directory) {
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
     */
    public CompileKotlinOperation buildTestDirectory(File directory) {
        buildTestDirectory_ = directory;
        return this;
    }

    /**
     * Provides the test build destination directory.
     *
     * @param directory the directory to use for the test build destination
     * @return this operation instance
     */
    public CompileKotlinOperation buildTestDirectory(Path directory) {
        return buildTestDirectory(directory.toFile());
    }

    /**
     * Provides the test build destination directory.
     *
     * @param directory the directory to use for the test build destination
     * @return this operation instance
     */
    public CompileKotlinOperation buildTestDirectory(String directory) {
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
     * @see #compileMainClasspath(Collection)
     */
    public CompileKotlinOperation compileMainClasspath(String... classpath) {
        return compileMainClasspath(List.of(classpath));
    }

    /**
     * Provides the entries for the main compilation classpath.
     *
     * @param classpath the classpath entries
     * @return this operation instance
     */
    public CompileKotlinOperation compileMainClasspath(Collection<String> classpath) {
        compileMainClasspath_.addAll(classpath);
        return this;
    }

    /**
     * Retrieves the entries for the main compilation classpath.
     *
     * @return the classpath entries
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<String> compileMainClasspath() {
        return compileMainClasspath_;
    }

    /**
     * Retrieves the compilation options for the compiler.
     *
     * @return the compilation options
     */
    public CompileOptions compileOptions() {
        return compileOptions_;
    }

    /**
     * Provides the compilation options to pass to the Kotlin compiler.
     *
     * @param options the compiler options
     * @return this operation instance
     */
    public CompileKotlinOperation compileOptions(CompileOptions options) {
        compileOptions_ = options;
        return this;
    }

    /**
     * Provides entries for the test compilation classpath.
     *
     * @param classpath one or more classpath entries
     * @return this operation instance
     */
    public CompileKotlinOperation compileTestClasspath(String... classpath) {
        return compileTestClasspath(List.of(classpath));
    }

    /**
     * Provides the entries for the test compilation classpath.
     *
     * @param classpath the classpath entries
     * @return this operation instance
     */
    public CompileKotlinOperation compileTestClasspath(Collection<String> classpath) {
        compileTestClasspath_.addAll(classpath);
        return this;
    }

    /**
     * Retrieves the entries for the test compilation classpath.
     *
     * @return the classpath entries
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<String> compileTestClasspath() {
        return compileTestClasspath_;
    }

    /**
     * Configures a compile operation from a {@link BaseProject}.
     * <p>
     * Sets the following from the project:
     * <ul>
     *     <li>{@link #workDir() workDir} to the project's directory.</li>
     *     <li>{@link #buildMainDirectory() buildMainDirectory}</li>
     *     <li>{@link #buildTestDirectory() buildTestDirectory}</li>
     *     <li>{@link #compileMainClasspath() compileMainClasspath}</li>
     *     <li>{@link #compileTestClasspath() compilesTestClasspath}</li>
     *     <li>{@link #mainSourceDirectories() mainSourceDirectories} to the {@code kotlin} directory in
     *     {@link BaseProject#srcMainDirectory() srcMainDirectory}, if present.</li>
     *     <li>{@link #testSourceDirectories() testSourceDirectories} to the {@code kotlin} directory in
     *     {@link BaseProject#srcTestDirectory() srcTestDirectory}, if present.</li>
     *     <li>{@link CompileOptions#jdkRelease jdkRelease} to {@link BaseProject#javaRelease() javaRelease}</li>
     *     <li>{@link CompileOptions#jvmTarget jvmTarget} to {@link BaseProject#javaRelease() javaRelease}</li>
     *     <li>{@link CompileOptions#noStdLib(boolean) noStdLib} to {@code true}</li>
     * </ul>
     *
     * @param project the project to configure the compile operation from
     * @return this operation instance
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public CompileKotlinOperation fromProject(BaseProject project) {
        project_ = project;
        workDir_ = new File(project.workDirectory().getAbsolutePath());

        buildMainDirectory_ = project.buildMainDirectory();
        buildTestDirectory_ = project.buildTestDirectory();
        compileMainClasspath_.addAll(project.compileMainClasspath());
        compileTestClasspath_.addAll(project.compileTestClasspath());

        var mainDir = new File(project.srcMainDirectory(), "kotlin");
        if (mainDir.exists()) {
            mainSourceDirectories_.add(mainDir);
        }
        var testDir = new File(project.srcTestDirectory(), "kotlin");
        if (testDir.exists()) {
            testSourceDirectories_.add(testDir);
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
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public JvmOptions jvmOptions() {
        return jvmOptions_;
    }

    /**
     * Pass an option directly to the Java Virtual Machine
     *
     * @param jvmOptions the JVM options
     * @return this operation instance
     */
    public CompileKotlinOperation jvmOptions(Collection<String> jvmOptions) {
        jvmOptions_.addAll(jvmOptions);
        return this;
    }

    /**
     * Pass an option directly to the Java Virtual Machine
     *
     * @param jvmOptions one or more JVM option
     * @return this operation instance
     */
    public CompileKotlinOperation jvmOptions(String... jvmOptions) {
        return jvmOptions(List.of(jvmOptions));
    }

    /**
     * Provides the Kotlin home directory, if it differs from the default {@code KOTLIN_HOME}.
     *
     * @param dir the directory path
     * @return this operation instance
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public CompileKotlinOperation kotlinHome(String dir) {
        return kotlinHome(new File(dir));
    }

    /**
     * Provides the Kotlin home directory, if it differs from the default {@code KOTLIN_HOME}.
     *
     * @param dir the directory
     * @return this operation instance
     */
    public CompileKotlinOperation kotlinHome(File dir) {
        kotlinHome_ = dir;
        return this;
    }

    /**
     * Provides the Kotlin home directory, if it differs from the default {@code KOTLIN_HOME}.
     *
     * @param dir the directory path
     * @return this operation instance
     */
    public CompileKotlinOperation kotlinHome(Path dir) {
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
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public CompileKotlinOperation kotlinc(String executable) {
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
     */
    public CompileKotlinOperation kotlinc(File executable) {
        kotlinc_ = executable;
        return this;
    }

    /**
     * Provides the path to the Kotlin compiler ({@code kotlinc}) executable, if not in {@link #kotlinHome()}.
     *
     * @param executable the executable path
     * @return this operation instance
     */
    public CompileKotlinOperation kotlinc(Path executable) {
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
     * @see #mainSourceDirectories(Collection)
     */
    public CompileKotlinOperation mainSourceDirectories(File... directories) {
        return mainSourceDirectories(List.of(directories));
    }

    /**
     * Provides main source directories that should be compiled.
     *
     * @param directories one or more main source directories
     * @return this operation instance
     * @see #mainSourceDirectoriesPaths(Collection)
     */
    public CompileKotlinOperation mainSourceDirectories(Path... directories) {
        return mainSourceDirectoriesPaths(List.of(directories));
    }

    /**
     * Provides main source directories that should be compiled.
     *
     * @param directories one or more main source directories
     * @return this operation instance
     * @see #mainSourceDirectoriesStrings(Collection)
     */
    public CompileKotlinOperation mainSourceDirectories(String... directories) {
        return mainSourceDirectoriesStrings(List.of(directories));
    }

    /**
     * Provides the main source directories that should be compiled.
     *
     * @param directories the main source directories
     * @return this operation instance
     * @see #mainSourceDirectories(File...)
     */
    public CompileKotlinOperation mainSourceDirectories(Collection<File> directories) {
        mainSourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Provides the main source directories that should be compiled.
     *
     * @param directories the main source directories
     * @return this operation instance
     * @see #mainSourceDirectories(Path...)
     */
    public CompileKotlinOperation mainSourceDirectoriesPaths(Collection<Path> directories) {
        return mainSourceDirectories(directories.stream().map(Path::toFile).toList());
    }

    /**
     * Provides the main source directories that should be compiled.
     *
     * @param directories the main source directories
     * @return this operation instance
     * @see #mainSourceDirectories(String...)
     */
    public CompileKotlinOperation mainSourceDirectoriesStrings(Collection<String> directories) {
        return mainSourceDirectories(directories.stream().map(File::new).toList());
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
     * @see #mainSourceFiles(Collection)
     */
    public CompileKotlinOperation mainSourceFiles(File... files) {
        return mainSourceFiles(List.of(files));
    }

    /**
     * Provides main source files that should be compiled.
     *
     * @param files one or more main source files
     * @return this operation instance
     * @see #mainSourceFilesStrings(Collection)
     */
    public CompileKotlinOperation mainSourceFiles(String... files) {
        return mainSourceFilesStrings(List.of(files));
    }

    /**
     * Provides main source files that should be compiled.
     *
     * @param files one or more main source files
     * @return this operation instance
     * @see #mainSourceFilesPaths(Collection)
     */
    public CompileKotlinOperation mainSourceFiles(Path... files) {
        return mainSourceFilesPaths(List.of(files));
    }

    /**
     * Provides the main source files that should be compiled.
     *
     * @param files the main source files
     * @return this operation instance
     * @see #mainSourceFiles(File...)
     */
    public CompileKotlinOperation mainSourceFiles(Collection<File> files) {
        mainSourceFiles_.addAll(files);
        return this;
    }

    /**
     * Provides the main source files that should be compiled.
     *
     * @param files the main source files
     * @return this operation instance
     * @see #mainSourceFiles(Path...)
     */
    public CompileKotlinOperation mainSourceFilesPaths(Collection<Path> files) {
        return mainSourceFiles(files.stream().map(Path::toFile).toList());
    }

    /**
     * Provides the main source files that should be compiled.
     *
     * @param files the main source files
     * @return this operation instance
     * @see #mainSourceFiles(String...)
     */
    public CompileKotlinOperation mainSourceFilesStrings(Collection<String> files) {
        return mainSourceFiles(files.stream().map(File::new).toList());
    }

    /**
     * Provides compiler plugins.
     *
     * @param directory the directory containing the plugin JARs
     * @param plugins   one or more plugins
     * @return this class instance
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public CompileKotlinOperation plugins(String directory, CompilerPlugin... plugins) {
        return plugins(new File(directory), plugins);
    }

    /**
     * Provides compiler plugins.
     *
     * @param directory the directory containing the plugin JARs
     * @param plugins   one or more plugins
     * @return this class instance
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public CompileKotlinOperation plugins(File directory, CompilerPlugin... plugins) {
        for (var p : plugins) {
            plugins_.add(new File(directory, p.jar).getAbsolutePath());
        }
        return this;
    }

    /**
     * Retrieves the compiler plugins.
     *
     * @return the compiler plugins
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<String> plugins() {
        return plugins_;
    }

    /**
     * Provides compiler plugins.
     *
     * @param plugins one or more plugins
     * @return this class instance
     */
    public CompileKotlinOperation plugins(String... plugins) {
        return plugins(List.of(plugins));
    }

    /**
     * Provides compiler plugins.
     *
     * @param plugins the compiler plugins
     * @return this class instance
     */
    public CompileKotlinOperation plugins(Collection<String> plugins) {
        plugins_.addAll(plugins);
        return this;
    }

    /**
     * Provides compiler plugins.
     *
     * @param directory the directory containing the plugin JARs
     * @param plugins   one or more plugins
     * @return this class instance
     */
    public CompileKotlinOperation plugins(Path directory, CompilerPlugin... plugins) {
        return plugins(directory.toFile(), plugins);
    }

    /**
     * Provides compiler plugins located in the {@link #kotlinHome()} lib directory.
     *
     * @param plugins one or more plugins
     * @return this class instance
     * @see #plugins(File, CompilerPlugin...)
     */
    public CompileKotlinOperation plugins(CompilerPlugin... plugins) {
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
     * @see #testSourceDirectories(Collection)
     */
    public CompileKotlinOperation testSourceDirectories(File... directories) {
        return testSourceDirectories(List.of(directories));
    }

    /**
     * Provides test source directories that should be compiled.
     *
     * @param directories one or more test source directories
     * @return this operation instance
     * @see #testSourceDirectoriesPaths(Collection)
     */
    public CompileKotlinOperation testSourceDirectories(Path... directories) {
        return testSourceDirectoriesPaths(List.of(directories));
    }

    /**
     * Provides test source directories that should be compiled.
     *
     * @param directories one or more test source directories
     * @return this operation instance
     * @see #testSourceDirectoriesStrings(Collection)
     */
    public CompileKotlinOperation testSourceDirectories(String... directories) {
        return testSourceDirectoriesStrings(List.of(directories));
    }

    /**
     * Provides the test source directories that should be compiled.
     *
     * @param directories the test source directories
     * @return this operation instance
     * @see #testSourceDirectories(File...)
     */
    public CompileKotlinOperation testSourceDirectories(Collection<File> directories) {
        testSourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Provides the test source directories that should be compiled.
     *
     * @param directories the test source directories
     * @return this operation instance
     * @see #testSourceDirectories(Path...)
     */
    public CompileKotlinOperation testSourceDirectoriesPaths(Collection<Path> directories) {
        return testSourceDirectories(directories.stream().map(Path::toFile).toList());
    }

    /**
     * Provides the test source directories that should be compiled.
     *
     * @param directories the test source directories
     * @return this operation instance
     * @see #testSourceDirectories(String...)
     */
    public CompileKotlinOperation testSourceDirectoriesStrings(Collection<String> directories) {
        return testSourceDirectories(directories.stream().map(File::new).toList());
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
     * @see #testSourceFiles(Collection)
     */
    public CompileKotlinOperation testSourceFiles(File... files) {
        return testSourceFiles(List.of(files));
    }

    /**
     * Provides the test sources files that should be compiled.
     *
     * @param files one or more test source files
     * @return this operation instance
     * @see #testSourceFilesStrings(Collection)
     */
    public CompileKotlinOperation testSourceFiles(String... files) {
        return testSourceFilesStrings(List.of(files));
    }

    /**
     * Provides the test sources files that should be compiled.
     *
     * @param files one or more test source files
     * @return this operation instance
     * @see #testSourceFilesPaths(Collection)
     */
    public CompileKotlinOperation testSourceFiles(Path... files) {
        return testSourceFilesPaths(List.of(files));
    }

    /**
     * Provides the test source files that should be compiled.
     *
     * @param files the test source files
     * @return this operation instance
     * @see #testSourceFiles(File...)
     */
    public CompileKotlinOperation testSourceFiles(Collection<File> files) {
        testSourceFiles_.addAll(files);
        return this;
    }

    /**
     * Provides the test source files that should be compiled.
     *
     * @param files the test source files
     * @return this operation instance
     * @see #testSourceFiles(Path...)
     */
    public CompileKotlinOperation testSourceFilesPaths(Collection<Path> files) {
        return testSourceFiles(files.stream().map(Path::toFile).toList());
    }

    /**
     * Provides the test source files that should be compiled.
     *
     * @param files the test source files
     * @return this operation instance
     * @see #testSourceFiles(String...)
     */
    public CompileKotlinOperation testSourceFilesStrings(Collection<String> files) {
        return testSourceFiles(files.stream().map(File::new).toList());
    }

    /**
     * Provides the working directory if it differs from the project's directory.
     *
     * @param dir the directory
     * @return this operation instance
     */
    public CompileKotlinOperation workDir(File dir) {
        workDir_ = dir;
        return this;
    }

    /**
     * Provides the working directory if it differs from the project's directory.
     *
     * @param dir the directory
     * @return this operation instance
     */
    public CompileKotlinOperation workDir(Path dir) {
        return workDir(dir.toFile());
    }

    /**
     * Provides the working directory if it differs from the project's directory.
     *
     * @param dir the directory path
     * @return this operation instance
     */
    public CompileKotlinOperation workDir(String dir) {
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

    private String cleanPath(File path) {
        return cleanPath(path.getAbsolutePath());
    }

    private String cleanPath(String path) {
        if (SystemUtils.isWindows()) {
            return path.replaceAll("\\\\", "\\\\\\\\");
        }
        return path;
    }

    /**
     * Part of the {@link #execute execute} operation, builds the main sources.
     *
     * @throws ExitStatusException if an error occurs
     */
    @SuppressWarnings("PMD.SystemPrintln")
    protected void executeBuildMainSources() throws ExitStatusException {
        if (!silent()) {
            System.out.println("Compiling Kotlin main sources.");
        }

        executeBuildSources(
                compileMainClasspath(),
                sources(mainSourceFiles(), mainSourceDirectories()),
                buildMainDirectory(),
                null);
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
    @SuppressWarnings("PMD.PreserveStackTrace")
    @SuppressFBWarnings({"COMMAND_INJECTION", "LEST_LOST_EXCEPTION_STACK_TRACE", "MDM_STRING_BYTES_ENCODING",
            "DM_DEFAULT_ENCODING"})
    protected void executeBuildSources(Collection<String> classpath, Collection<File> sources, File destination,
                                       File friendPaths)
            throws ExitStatusException {

        var cp = new ArrayList<String>();
        if (CollectionUtils.isNotEmpty(classpath)) {
            cp.addAll(classpath);
        }

        if (sources.isEmpty()) {
            if (!silent() && LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Nothing to compile.");
            }
            return;
        } else if (destination == null) {
            if (!silent() && LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.severe("No destination specified.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }

        var command = new ArrayList<String>();
        var args = new ArrayList<String>();

        // kotlinc
        if (kotlinc_ != null) {
            command.add(kotlinc_.getAbsolutePath());
        } else if (kotlinHome_ != null) {
            var kotlinc = findKotlincInDir(kotlinHome_.getAbsolutePath());
            if (kotlinc != null) {
                command.add(kotlinc);
            } else {
                if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                    LOGGER.severe("Could not locate Kotlin compiler in: " + kotlinHome_);
                }
                throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
            }
        } else {
            command.add(findKotlincPath(silent()));
        }

        // jVM options
        if (!jvmOptions_.isEmpty()) {
            jvmOptions_.forEach(s -> command.add("-J" + s));
        }

        // classpath
        if (compileOptions_ != null && CollectionUtils.isNotEmpty(compileOptions_.classpath())) {
            cp.addAll(compileOptions_.classpath().stream().map(this::cleanPath).toList());
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
        if (IOUtils.exists(friendPaths)) {
            args.add("-Xfriend-paths=\"" + cleanPath(friendPaths) + '"');
        }

        // plugins
        if (!plugins_.isEmpty()) {
            var kotlinHomePath = findKotlinHome();

            plugins_.forEach(p -> {
                File pluginJar = null;

                // Try as enum first
                try {
                    var pluginValue = CompilerPlugin.valueOf(p);
                    if (kotlinHomePath != null) {
                        pluginJar = IOUtils.resolveFile(kotlinHomePath, "lib", pluginValue.jar);
                    } else if (LOGGER.isLoggable(Level.WARNING) && !silent()) {
                        LOGGER.warning("The Kotlin home must be set to specify the '"
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
                    } else if (LOGGER.isLoggable(Level.WARNING) && !silent()) {
                        LOGGER.warning("Could not locate compiler plugin: " + pluginJar.getAbsolutePath());
                    }
                }
            });
        }

        // sources
        sources.forEach(f -> args.add('"' + cleanPath(f) + '"'));

        var argsLine = String.join(" ", args);

        // log the command line
        if (LOGGER.isLoggable(Level.FINE) && !silent()) {
            LOGGER.fine(String.join(" ", command) + " " + argsLine);
        }

        try {
            // create and write the @argfile
            var argsFile = File.createTempFile("bld-kotlinc-", ".args");
            argsFile.deleteOnExit();

            Files.write(argsFile.toPath(), argsLine.getBytes());

            command.add("@" + argsFile.getAbsolutePath());

            // run the command
            var pb = new ProcessBuilder();
            pb.inheritIO();
            pb.command(command);
            pb.directory(workDir_);

            var proc = pb.start();
            proc.waitFor();
            ExitStatusException.throwOnFailure(proc.exitValue());
        } catch (IOException | InterruptedException e) {
            if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.severe(e.getLocalizedMessage());
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }
    }

    /**
     * Part of the {@link #execute execute} operation, builds the test sources.
     *
     * @throws ExitStatusException if an error occurs
     */
    @SuppressWarnings("PMD.SystemPrintln")
    protected void executeBuildTestSources() throws ExitStatusException {
        if (!silent()) {
            System.out.println("Compiling Kotlin test sources.");
        }
        executeBuildSources(
                compileTestClasspath(),
                sources(testSourceFiles(), testSourceDirectories()),
                buildTestDirectory(),
                buildMainDirectory());
    }

    /**
     * Part of the {@link #execute execute} operation, creates the build directories.
     *
     * @throws IOException if an error occurs
     */
    protected void executeCreateBuildDirectories() throws IOException {
        if (!IOUtils.mkdirs(buildMainDirectory())) {
            throw new IOException("Could not create build main directory: " + buildMainDirectory().getAbsolutePath());
        }
        if (!IOUtils.mkdirs(buildTestDirectory())) {
            throw new IOException("Could not create build test directory: " + buildTestDirectory().getAbsolutePath());
        }
    }

    private File findKotlinHome() {
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
            if (IOUtils.isDirectory(parent)) {
                if (parent.getPath().endsWith("bin")) {
                    var binParent = parent.getParentFile();
                    if (IOUtils.isDirectory(binParent)) {
                        return binParent.getParentFile();
                    }
                } else {
                    return parent;
                }
            }
        }

        return null;
    }

    // Combine Kotlin sources
    private List<File> sources(Collection<File> files, Collection<File> directories) {
        var sources = new ArrayList<>(files);
        sources.addAll(directories);
        return sources;
    }
}
