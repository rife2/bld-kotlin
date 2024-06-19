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

import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import rife.bld.BaseProject;
import rife.bld.extension.kotlin.CompileOptions;
import rife.bld.extension.kotlin.CompilerPlugin;
import rife.bld.operations.AbstractOperation;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
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
    private BaseProject project_;

    /**
     * Returns the list of JARs contained in a given directory.
     *
     * @param directory the directory
     * @param regex     the regular expression to match
     * @return the list of JARs
     */
    public static List<String> getJarList(File directory, String regex) {
        var jars = new ArrayList<String>();

        if (directory.isDirectory()) {
            var files = directory.listFiles();
            if (files != null) {
                for (var f : files) {
                    if (!f.getName().endsWith("-sources.jar") && (!f.getName().endsWith("-javadoc.jar")) &&
                            f.getName().matches(regex)) {
                        jars.add(f.getAbsolutePath());
                    }
                }
            }
        }

        return jars;
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
        Collections.addAll(compileMainClasspath_, classpath);
        return this;
    }

    /**
     * Provides a list of entries for the main compilation classpath.
     *
     * @param classpath a list of classpath entries
     * @return this operation instance
     */
    public CompileKotlinOperation compileMainClasspath(Collection<String> classpath) {
        compileMainClasspath_.addAll(classpath);
        return this;
    }

    /**
     * Retrieves the list of entries for the main compilation classpath.
     *
     * @return the list of classpath
     */
    public Collection<String> compileMainClasspath() {
        return compileMainClasspath_;
    }

    /**
     * Retrieves the list of compilation options for the compiler.
     *
     * @return the compilation options
     */
    public CompileOptions compileOptions() {
        return compileOptions_;
    }

    /**
     * Provides a list of compilation options to pass to the Kotlin compiler.
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
        Collections.addAll(compileTestClasspath_, classpath);
        return this;
    }

    /**
     * Provides a list of entries for the test compilation classpath.
     *
     * @param classpath a list of classpath entries
     * @return this operation instance
     */
    public CompileKotlinOperation compileTestClasspath(Collection<String> classpath) {
        compileTestClasspath_.addAll(classpath);
        return this;
    }

    /**
     * Retrieves the entries for the test compilation classpath.
     *
     * @return the list of classpath
     */
    public Collection<String> compileTestClasspath() {
        return compileTestClasspath_;
    }

    /**
     * Performs the compile operation.
     */
    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public void execute()
            throws IOException {
        if (project_ == null) {
            throw new IllegalArgumentException("A project must be specified.");
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
     * @throws IOException if an error occurs
     */
    @SuppressWarnings("PMD.SystemPrintln")
    protected void executeBuildMainSources()
            throws IOException {
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
     * @throws IOException if an error occurs
     */
    protected void executeBuildSources(Collection<String> classpath, Collection<File> sources, File destination,
                                       File friendPaths)
            throws IOException {
        if (sources.isEmpty() || destination == null) {
            return;
        }

        var k2 = new K2JVMCompiler();
        var args = new ArrayList<String>();

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
            LOGGER.fine("kotlinc " + String.join(" ", args));
        }

        var exitCode = k2.exec(System.err, args.toArray(String[]::new));
        if (exitCode.getCode() != 0) {
            throw new IOException("Kotlin compilation failed.");
        }
    }

    /**
     * Part of the {@link #execute execute} operation, builds the test sources.
     *
     * @throws IOException if an error occurs
     */
    @SuppressWarnings("PMD.SystemPrintln")
    protected void executeBuildTestSources()
            throws IOException {
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

    /**
     * Provides main source directories that should be compiled.
     *
     * @param directories one or more main source directories
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceDirectories(File... directories) {
        Collections.addAll(mainSourceDirectories_, directories);
        return this;
    }

    /**
     * Provides main source directories that should be compiled.
     *
     * @param directories one or more main source directories
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceDirectories(String... directories) {
        Collections.addAll(mainSourceDirectories_, Arrays.stream(directories)
                .map(File::new)
                .toArray(File[]::new));
        return this;
    }

    /**
     * Provides a list of main source directories that should be compiled.
     *
     * @param directories a list of main source directories
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceDirectories(Collection<File> directories) {
        mainSourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Retrieves the main source directories that should be compiled.
     *
     * @return the list of directories
     */
    public Collection<File> mainSourceDirectories() {
        return mainSourceDirectories_;
    }

    /**
     * Provides main files that should be compiled.
     *
     * @param files one or more main files
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceFiles(File... files) {
        mainSourceFiles_.addAll(Arrays.asList(files));
        return this;
    }

    /**
     * Provides main files that should be compiled.
     *
     * @param files one or more main files
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceFiles(String... files) {
        Collections.addAll(mainSourceFiles_, Arrays.stream(files)
                .map(File::new)
                .toArray(File[]::new));
        return this;
    }

    /**
     * Provides a list of main files that should be compiled.
     *
     * @param files a list of main files
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceFiles(Collection<File> files) {
        mainSourceFiles_.addAll(files);
        return this;
    }

    /**
     * Retrieves the main files that should be compiled.
     *
     * @return the list of files
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
        Collections.addAll(plugins_, plugins);
        return this;
    }

    /**
     * Retrieves the compiler plugins.
     *
     * @return the list of plugins
     */
    public Collection<String> plugins() {
        return plugins_;
    }

    /**
     * Provides compiler plugins.
     *
     * @param plugins a list of plugins
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
            plugins_.addAll(getJarList(directory, plugin.regex));
        }
        return this;
    }

    /**
     * Provides compiler plugins.
     *
     * @param plugins one or more plugins
     * @return this class instance
     */
    public CompileKotlinOperation plugins(CompilerPlugin... plugins) {
        for (var plugin : plugins) {
            plugins_.addAll(getJarList(project_.libBldDirectory(), plugin.regex));
        }
        return this;
    }

    /**
     * Provides compiler plugins.
     *
     * @param jars    the list of plugin JARs
     * @param plugins one or more plugins
     * @return this class instance
     */
    public CompileKotlinOperation plugins(Collection<File> jars, CompilerPlugin... plugins) {
        jars.forEach(jar -> {
            for (var plugin : plugins) {
                if (jar.getName().matches(plugin.regex)) {
                    plugins_.add(jar.getAbsolutePath());
                    break;
                }
            }
        });

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
        Collections.addAll(testSourceDirectories_, directories);
        return this;
    }

    /**
     * Provides test source directories that should be compiled.
     *
     * @param directories one or more test source directories
     * @return this operation instance
     */
    public CompileKotlinOperation testSourceDirectories(String... directories) {
        Collections.addAll(testSourceDirectories_, Arrays.stream(directories)
                .map(File::new)
                .toArray(File[]::new));
        return this;
    }

    /**
     * Provides a list of test source directories that should be compiled.
     *
     * @param directories a list of test source directories
     * @return this operation instance
     */
    public CompileKotlinOperation testSourceDirectories(Collection<File> directories) {
        testSourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Retrieves the test source directories that should be compiled.
     *
     * @return the list of directories
     */
    public Collection<File> testSourceDirectories() {
        return testSourceDirectories_;
    }

    /**
     * Provides test files that should be compiled.
     *
     * @param files one or more test files
     * @return this operation instance
     */
    public CompileKotlinOperation testSourceFiles(File... files) {
        testSourceFiles_.addAll(Arrays.asList(files));
        return this;
    }

    /**
     * Provides test files that should be compiled.
     *
     * @param files one or more test files
     * @return this operation instance
     */
    public CompileKotlinOperation testSourceFiles(String... files) {
        Collections.addAll(testSourceFiles_, Arrays.stream(files)
                .map(File::new)
                .toArray(size -> new File[files.length]));
        return this;
    }

    /**
     * Provides a list of test files that should be compiled.
     *
     * @param files a list of test files
     * @return this operation instance
     */
    public CompileKotlinOperation testSourceFiles(Collection<File> files) {
        testSourceFiles_.addAll(files);
        return this;
    }

    /**
     * Retrieves the test files that should be compiled.
     *
     * @return the list of files
     */
    public Collection<File> testSourceFiles() {
        return testSourceFiles_;
    }
}
