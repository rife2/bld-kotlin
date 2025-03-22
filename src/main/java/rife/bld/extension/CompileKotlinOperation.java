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

import rife.bld.BaseProject;
import rife.bld.extension.kotlin.CompileOptions;
import rife.bld.extension.kotlin.CompilerPlugin;
import rife.bld.operations.AbstractOperation;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
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
public class CompileKotlinOperation extends AbstractOperation<CompileKotlinOperation> {
    private static final Logger LOGGER = Logger.getLogger(CompileKotlinOperation.class.getName());
    private static final String OS_NAME =
            System.getProperty("os.name") != null ? System.getProperty("os.name").toLowerCase(Locale.US) : null;
    private static final String KOTLINC_EXECUTABLE = "kotlinc" + (isWindows() ? ".bat" : "");
    private final Collection<String> compileMainClasspath_ = new ArrayList<>();
    private final Collection<String> compileTestClasspath_ = new ArrayList<>();
    private final Collection<File> mainSourceDirectories_ = new ArrayList<>();
    private final Collection<File> mainSourceFiles_ = new ArrayList<>();
    private final Collection<String> plugins_ = new ArrayList<>();
    private final Collection<File> testSourceDirectories_ = new ArrayList<>();
    private final Collection<File> testSourceFiles_ = new ArrayList<>();
    private File buildMainDirectory_;
    private File buildTestDirectory_;
    private CompileOptions compileOptions_ = new CompileOptions();
    private File kotlinHome_;
    private File kotlinc_;
    private BaseProject project_;
    private File workDir_;

    private static String findKotlincInDir(String directory) {
        var kotlinc = new File(directory, KOTLINC_EXECUTABLE);

        if (isExecutable(kotlinc)) {
            return kotlinc.getAbsolutePath();
        }

        // Check bin subdirectory if it exists
        var binDir = new File(directory, "bin");
        if (binDir.exists() && binDir.isDirectory()) {
            kotlinc = new File(binDir, KOTLINC_EXECUTABLE);
            if (isExecutable(kotlinc)) {
                return kotlinc.getAbsolutePath();
            }
        }

        return null;
    }

    /**
     * Locates the Kotlin compiler (kotlinc) executable.
     *
     * @return The path to the kotlinc executable, or {@code kotlinc}/{@code kotlinc.bat} if not found.
     */
    public static String findKotlincPath() {
        String kotlincPath;

        // Check KOTLIN_HOME environment variable first
        var kotlinHome = System.getenv("KOTLIN_HOME");
        if (kotlinHome != null && !kotlinHome.isEmpty()) {
            kotlincPath = findKotlincInDir(kotlinHome);
            if (kotlincPath != null) {
                return kotlincPath;
            }
        }

        // Check PATH environment variable
        var pathEnv = System.getenv("PATH");
        if (pathEnv != null && !pathEnv.isEmpty()) {
            var pathDirs = pathEnv.split(File.pathSeparator);
            for (var dir : pathDirs) {
                kotlincPath = findKotlincInDir(dir);
                if (kotlincPath != null) {
                    return kotlincPath;
                }
            }
        }

        // Common installation paths (e.g., SDKMAN!, IntelliJ IDEA, etc.)
        List<String> commonPaths = new ArrayList<>();

        if (isLinux()) {
            commonPaths.add("/usr/bin");
            commonPaths.add("/usr/local/bin");
            commonPaths.add("/usr/local/kotlin/bin");
            commonPaths.add("/opt/kotlin/bin");
            var userHome = System.getProperty("user.home");
            if (userHome != null) {
                commonPaths.add(userHome + "/.sdkman/candidates/kotlin/current/bin"); // SDKMAN!
                commonPaths.add(userHome + "/.local/share/JetBrains/Toolbox/apps/intellij-idea-ultimate/plugins/Kotlin/bin"); // Toolbox IDEA Ultimate
                commonPaths.add(userHome + "/.local/share/JetBrains/Toolbox/apps/intellij-idea-community-edition/plugins/Kotlin/bin"); // Toolbox IDEA CE
                commonPaths.add(userHome + "/.local/share/JetBrains/Toolbox/apps/android-studio/plugins/Kotlin/bin"); // Toolbox Android Studio
            }
        } else if (isWindows()) {
            var localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                commonPaths.add(localAppData + "\\Programs\\IntelliJ IDEA Ultimate\\plugins\\Kotlin\\kotlinc\\bin"); // Toolbox IDEA Ultimate
                commonPaths.add(localAppData + "\\Programs\\IntelliJ IDEA Community Edition\\plugins\\Kotlin\\kotlinc\\bin"); // Toolbox IDEA CE
                commonPaths.add(localAppData + "\\Programs\\Android Studio\\plugins\\Kotlin\\kotlinc\\bin"); // Toolbox Android Studio
            }
            var programFiles = System.getenv("ProgramFiles");
            if (programFiles != null) {
                commonPaths.add(programFiles + File.separator + "Kotlin");
            }
        } else if (isMacOS()) {
            commonPaths.add("/usr/local/bin"); // Homebrew
            commonPaths.add("/opt/homebrew/bin"); // Homebrew
            var userHome = System.getProperty("user.home");
            if (userHome != null) {
                commonPaths.add(userHome + "/.sdkman/candidates/kotlin/current/bin"); // SDKMAN!
            }
            commonPaths.add("/Applications/IntelliJ IDEA Ultimate.app/Contents/plugins/Kotlin/bin"); //IntelliJ IDEA Ultimate
            commonPaths.add("/Applications/IntelliJ IDEA.app/Contents/plugins/Kotlin/bin"); //IntelliJ IDEA
            commonPaths.add("/Applications/Android Studio.app/Contents/plugins/Kotlin/bin"); //Android Studio
        }

        for (var location : commonPaths) {
            kotlincPath = findKotlincInDir(location);
            if (kotlincPath != null) {
                return kotlincPath;
            }
        }

        // Try 'which' or 'where' commands (less reliable but sometimes works)
        try {
            Process process;
            if (isWindows()) {
                process = Runtime.getRuntime().exec("where kotlinc");
            } else {
                process = Runtime.getRuntime().exec("which kotlinc");
            }

            try (var scanner = new Scanner(process.getInputStream())) {
                if (scanner.hasNextLine()) {
                    kotlincPath = scanner.nextLine().trim();
                    if (isExecutable(new File(kotlincPath))) {
                        return kotlincPath;
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore exceptions from which/where, as they might not be available
        }

        return KOTLINC_EXECUTABLE;
    }

    private static boolean isExecutable(File file) {
        return file != null && file.exists() && file.isFile() && file.canExecute();
    }

    /**
     * Determines if the operating system is Linux.
     *
     * @return true if the operating system is Linux, false otherwise.
     */
    public static boolean isLinux() {
        return OS_NAME != null && (OS_NAME.contains("linux") || OS_NAME.contains("unix")); // Consider Unix-like systems as well.
    }

    /**
     * Determines if the current operating system is macOS.
     *
     * @return true if the OS is macOS, false otherwise.
     */
    public static boolean isMacOS() {
        return OS_NAME != null && (OS_NAME.contains("mac") || OS_NAME.contains("darwin"));
    }

    /**
     * Determines if the given string is not blank.
     *
     * @param s the string
     * @return {@code true} if not blank, {@code false} otherwise.
     */
    public static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Determines if the current operating system is Windows.
     *
     * @return true if the operating system is Windows, false otherwise.
     */
    public static boolean isWindows() {
        return OS_NAME != null && OS_NAME.contains("win");
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
    public Collection<String> compileMainClasspath() {
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
    public Collection<String> compileTestClasspath() {
        return compileTestClasspath_;
    }

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
    protected void executeBuildSources(Collection<String> classpath, Collection<File> sources, File destination,
                                       File friendPaths)
            throws ExitStatusException {
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

        var args = new ArrayList<String>();

        // kotlinc
        if (kotlinc_ != null) {
            args.add(kotlinc_.getAbsolutePath());
        } else if (kotlinHome_ != null) {
            args.add(Objects.requireNonNullElseGet(findKotlincInDir(kotlinHome_.getAbsolutePath()),
                    CompileKotlinOperation::findKotlincPath));
        } else {
            args.add(findKotlincPath());
        }

        // classpath
        if (classpath != null && !classpath.isEmpty()) {
            args.add("-cp");
            args.add(FileUtils.joinPaths(classpath.stream().toList()));
        }

        // destination
        args.add("-d");
        args.add(destination.getAbsolutePath());

        // friend-path
        if (friendPaths != null && friendPaths.exists()) {
            args.add("-Xfriend-paths=" + friendPaths.getAbsolutePath());
        }

        // options
        if (compileOptions_ != null) {
            args.addAll(compileOptions_.args());
        }

        // plugins
        if (!plugins_.isEmpty()) {
            plugins_.forEach(p -> args.add("-Xplugin=" + p));
        }

        // sources
        sources.forEach(f -> args.add(f.getAbsolutePath()));

        if (LOGGER.isLoggable(Level.FINE) && !silent()) {
            LOGGER.fine(String.join(" ", args));
        }

        var pb = new ProcessBuilder();
        pb.inheritIO();
        pb.command(args);
        pb.directory(workDir_);

        try {
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
        if (buildMainDirectory() != null && !buildMainDirectory().exists() && !buildMainDirectory().mkdirs()) {
            throw new IOException("Could not created build main directory: " + buildMainDirectory().getAbsolutePath());
        }
        if (buildTestDirectory() != null && !buildTestDirectory().exists() && !buildTestDirectory().mkdirs()) {
            throw new IOException("Could not created build test directory: " + buildTestDirectory().getAbsolutePath());
        }
    }

    /**
     * Configures a compile operation from a {@link BaseProject}.
     * <p>
     * Sets the following from the project:
     * <ul>
     *     <li>{@link #workDir() workDir} to the project's directory.</li>
     *     <li>{@link #buildMainDirectory() buildMainDirectory}</li>
     *     <li>{@link #buildTestDirectory() buildTestDirectory}</li>
     *     <li>{@link #compileMainClasspath() compileMainClassPath}</li>
     *     <li>{@link #compileTestClasspath() compilesTestClassPath}</li>
     *     <li>{@link #mainSourceDirectories() mainSourceDirectories} to the {@code kotlin} directory in
     *     {@link BaseProject#srcMainDirectory() srcMainDirectory}, if present.</li>
     *     <li>{@link #testSourceDirectories() testSourceDirectories} to the {@code kotlin} directory in
     *     {@link BaseProject#srcTestDirectory() srcTestDirectory}, if present.</li>
     *     <li>{@link CompileOptions#jdkRelease jdkRelease} to {@link BaseProject#javaRelease() javaRelease}</li>
     *     <li>{@link CompileOptions#noStdLib(boolean) noStdLib} to {@code true}</li>
     * </ul>
     *
     * @param project the project to configure the compile operation from
     * @return this operation instance
     */
    public CompileKotlinOperation fromProject(BaseProject project) {
        project_ = project;
        workDir_ = new File(project.workDirectory().getAbsolutePath());

        var op = buildMainDirectory(project.buildMainDirectory())
                .buildTestDirectory(project.buildTestDirectory())
                .compileMainClasspath(project.compileMainClasspath())
                .compileTestClasspath(project.compileTestClasspath());

        var mainDir = new File(project.srcMainDirectory(), "kotlin");
        if (mainDir.exists()) {
            op = op.mainSourceDirectories(mainDir);
        }
        var testDir = new File(project.srcTestDirectory(), "kotlin");
        if (testDir.exists()) {
            op = op.testSourceDirectories(testDir);
        }

        if (project.javaRelease() != null && !compileOptions_.hasRelease()) {
            compileOptions_.jdkRelease(project.javaRelease());
        }
        compileOptions_.noStdLib(true);

        return op;
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
    public CompileKotlinOperation kotlinHome(String dir) {
        return kotlinHome(new File(dir));
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
    public CompileKotlinOperation kotlinc(String executable) {
        return kotlinc(new File(executable));
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
     * Retrieves the main source directories that should be compiled.
     *
     * @return the main source directories
     */
    public Collection<File> mainSourceDirectories() {
        return mainSourceDirectories_;
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
     * Retrieves the main files that should be compiled.
     *
     * @return the files
     */
    public Collection<File> mainSourceFiles() {
        return mainSourceFiles_;
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
    public CompileKotlinOperation plugins(String directory, CompilerPlugin... plugins) {
        return plugins(new File(directory), plugins);
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
     * Retrieves the compiler plugins.
     *
     * @return the compiler plugins
     */
    public Collection<String> plugins() {
        return plugins_;
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
    public CompileKotlinOperation plugins(File directory, CompilerPlugin... plugins) {
        for (var plugin : plugins) {
            plugins_.add(new File(directory, plugin.jar).getAbsolutePath());
        }
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
        if (kotlinHome_ != null) {
            var kotlinLib = new File(kotlinHome_, "lib");
            for (var plugin : plugins) {
                plugins(kotlinLib, plugin);
            }
        } else {
            if (LOGGER.isLoggable(Level.WARNING) && !silent()) {
                LOGGER.warning("The Kotlin home must be set to specify compiler plugins directly.");
            }
        }
        return this;
    }

    // Combine Kotlin sources
    private Collection<File> sources(Collection<File> files, Collection<File> directories) {
        var sources = new ArrayList<>(files);
        sources.addAll(directories);
        return sources;
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
     * Retrieves the test source directories that should be compiled.
     *
     * @return the test source directories
     */
    public Collection<File> testSourceDirectories() {
        return testSourceDirectories_;
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
     * Retrieves the test files that should be compiled.
     *
     * @return the test files
     */
    public Collection<File> testSourceFiles() {
        return testSourceFiles_;
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
     * Retrieves the working directory.
     *
     * @return the directory
     */
    public File workDir() {
        return workDir_;
    }

    /**
     * Provides the working directory, if it differs from the project's directory.
     *
     * @param dir the directory
     * @return this operation instance
     */
    public CompileKotlinOperation workDir(File dir) {
        workDir_ = dir;
        return this;
    }

    /**
     * Provides the working directory, if it differs from the project's directory.
     *
     * @param dir the directory
     * @return this operation instance
     */
    public CompileKotlinOperation workDir(Path dir) {
        return workDir(dir.toFile());
    }

    /**
     * Provides the working directory, if it differs from the project's directory.
     *
     * @param dir the directory path
     * @return this operation instance
     */
    public CompileKotlinOperation workDir(String dir) {
        return workDir(new File(dir));
    }
}
