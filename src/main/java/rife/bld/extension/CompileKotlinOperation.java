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

import rife.bld.BaseProject;
import rife.bld.extension.kotlin.CompileOptions;
import rife.bld.extension.kotlin.CompilerPlugin;
import rife.bld.operations.AbstractOperation;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
     */
    public CompileKotlinOperation compileMainClasspath(String... classpath) {
        compileMainClasspath_.addAll(List.of(classpath));
        return this;
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
        compileTestClasspath_.addAll(List.of(classpath));
        return this;
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
        if (sources.isEmpty() || destination == null) {
            return;
        }

        var args = new ArrayList<String>();

        // kotlinc
        args.add(kotlinCompiler());

        // classpath
        args.add("-cp");
        args.add(FileUtils.joinPaths(classpath.stream().toList()));

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
     *     <li>{@link #kotlinHome()} to the {@code KOTLIN_HOME} environment variable, if set.</li>
     *     <li>{@link #workDir()} to the project's directory.</li>
     *     <li>{@link #buildMainDirectory() buildMainDirectory}</li>
     *     <li>{@link #buildTestDirectory() buildTestDirectory}</li>
     *     <li>{@link #compileMainClasspath() compileMainClassPath}</li>
     *     <li>{@link #compileTestClasspath() compilesTestClassPath}</li>
     *     <li>{@link #mainSourceDirectories()} () mainSourceDirectories} to the {@code kotlin} directory in
     *     {@link BaseProject#srcMainDirectory() srcMainDirectory}</li>
     *     <li>{@link #testSourceDirectories() testSourceDirectories} to the {@code kotlin} directory in
     *     {@link BaseProject#srcTestDirectory() srcTestDirectory}</li>
     *     <li>{@link CompileOptions#jdkRelease jdkRelease} to {@link BaseProject#javaRelease() javaRelease}</li>
     *     <li>{@link CompileOptions#noStdLib(boolean) noStdLib} to {@code true}</li>
     * </ul>
     *
     * @param project the project to configure the compile operation from
     * @return this operation instance
     */
    public CompileKotlinOperation fromProject(BaseProject project) {
        project_ = project;

        var env = System.getenv("KOTLIN_HOME");
        if (env != null) {
            kotlinHome_ = new File(env);
        }

        workDir_ = new File(project.workDirectory().getAbsolutePath());

        var op = buildMainDirectory(project.buildMainDirectory())
                .buildTestDirectory(project.buildTestDirectory())
                .compileMainClasspath(project.compileMainClasspath())
                .compileTestClasspath(project.compileTestClasspath())
                .mainSourceDirectories(new File(project.srcMainDirectory(), "kotlin"))
                .testSourceDirectories(new File(project.srcTestDirectory(), "kotlin"));
        if (project.javaRelease() != null && !compileOptions_.hasRelease()) {
            compileOptions_.jdkRelease(project.javaRelease());
        }
        compileOptions_.noStdLib(true);

        return op;
    }

    private String kotlinCompiler() {
        if (kotlinc_ != null) {
            return kotlinc_.getAbsolutePath();
        } else if (kotlinHome_ != null) {
            var kotlinc = Path.of(kotlinHome_.getAbsolutePath(), "bin", "kotlinc").toFile();
            if (kotlinc.exists() && kotlinc.canExecute()) {
                return kotlinc.getAbsolutePath();
            }
        }
        return "kotlinc";
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
     * Returns the Kotlin home directory.
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
     * Provides main source directories that should be compiled.
     *
     * @param directories one or more main source directories
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceDirectories(File... directories) {
        mainSourceDirectories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Provides main source directories that should be compiled.
     *
     * @param directories one or more main source directories
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceDirectories(String... directories) {
        mainSourceDirectories_.addAll(Arrays.stream(directories).map(File::new).toList());
        return this;
    }

    /**
     * Provides the main source directories that should be compiled.
     *
     * @param directories the main source directories
     * @return this operation instance
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
     * Provides main source files that should be compiled.
     *
     * @param files one or more main source files
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceFiles(File... files) {
        mainSourceFiles_.addAll(List.of(files));
        return this;
    }

    /**
     * Provides main source files that should be compiled.
     *
     * @param files one or more main source files
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceFiles(String... files) {
        mainSourceFiles_.addAll(Arrays.stream(files).map(File::new).toList());
        return this;
    }

    /**
     * Provides the main source files that should be compiled.
     *
     * @param files the main source files
     * @return this operation instance
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
     * Provides compiler plugins.
     *
     * @param plugins one or more plugins
     * @return this class instance
     */
    public CompileKotlinOperation plugins(String... plugins) {
        plugins_.addAll(List.of(plugins));
        return this;
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
     */
    public CompileKotlinOperation testSourceDirectories(File... directories) {
        testSourceDirectories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Provides test source directories that should be compiled.
     *
     * @param directories one or more test source directories
     * @return this operation instance
     */
    public CompileKotlinOperation testSourceDirectories(String... directories) {
        testSourceDirectories_.addAll(Arrays.stream(directories).map(File::new).toList());
        return this;
    }

    /**
     * Provides the test source directories that should be compiled.
     *
     * @param directories the test source directories
     * @return this operation instance
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
     * Provides test source files that should be compiled.
     *
     * @param files one or more test source files
     * @return this operation instance
     */
    public CompileKotlinOperation testSourceFiles(File... files) {
        testSourceFiles_.addAll(List.of(files));
        return this;
    }

    /**
     * Provides the test sources files that should be compiled.
     *
     * @param files one or more test source files
     * @return this operation instance
     */
    public CompileKotlinOperation testSourceFiles(String... files) {
        testSourceFiles_.addAll(Arrays.stream(files).map(File::new).toList());
        return this;
    }

    /**
     * Provides the test source files that should be compiled.
     *
     * @param files the test source files
     * @return this operation instance
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
     * @param dir the directory path
     * @return this operation instance
     */
    public CompileKotlinOperation workDir(String dir) {
        return workDir(new File(dir));
    }
}
