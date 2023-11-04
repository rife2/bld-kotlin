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

import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import rife.bld.BaseProject;
import rife.bld.operations.AbstractOperation;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static rife.tools.FileUtils.getFileList;

/**
 * Compiles main and test Kotlin sources in the relevant build directories.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class CompileKotlinOperation extends AbstractOperation<CompileKotlinOperation> {
    public static final Pattern KOTLIN_FILE_PATTERN = Pattern.compile("^.*\\.kt$");
    private static final Logger LOGGER = Logger.getLogger(CompileKotlinOperation.class.getName());
    public final List<String> compileOptions_ = new ArrayList<>();
    private final List<String> compileMainClasspath_ = new ArrayList<>();
    private final List<String> compileTestClasspath_ = new ArrayList<>();
    private final List<File> mainSourceDirectories_ = new ArrayList<>();
    private final List<File> mainSourceFiles_ = new ArrayList<>();
    private final List<File> testSourceDirectories_ = new ArrayList<>();
    private final List<File> testSourceFiles_ = new ArrayList<>();
    private File buildMainDirectory_;
    private File buildTestDirectory_;

    public static List<File> getKotlinFileList(File directory) {
        if (directory == null) {
            return Collections.emptyList();
        } else {
            var dir_abs = directory.getAbsoluteFile();
            return getFileList(dir_abs, KOTLIN_FILE_PATTERN, null).stream().map((file) ->
                    new File(dir_abs, file)).toList();
        }
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
     * @return the main build destination
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
     * @return the test build destination
     */
    public File buildTestDirectory() {
        return buildTestDirectory_;
    }

    /**
     * Provides entries for the main compilation classpath.
     *
     * @param classpath classpath entries
     * @return this operation instance
     */
    public CompileKotlinOperation compileMainClasspath(String... classpath) {
        compileMainClasspath_.addAll(Arrays.asList(classpath));
        return this;
    }

    /**
     * Provides a list of entries for the main compilation classpath.
     *
     * @param classpath a list of classpath entries
     * @return this operation instance
     */
    public CompileKotlinOperation compileMainClasspath(List<String> classpath) {
        compileMainClasspath_.addAll(classpath);
        return this;
    }

    /**
     * Retrieves the list of entries for the main compilation classpath.
     *
     * @return the main compilation classpath list
     */
    public List<String> compileMainClasspath() {
        return compileMainClasspath_;
    }

    /**
     * Provides a list of compilation options to pass to the {@code kotlinc} compiler.
     *
     * @param options the list of compiler options
     * @return this operation instance
     */
    public CompileKotlinOperation compileOptions(List<String> options) {
        compileOptions_.addAll(options);
        return this;
    }

    /**
     * Provides the compilation options to pass to the {@code kotlinc} compiler.
     *
     * @param option the compiler option
     * @return this operation instance
     */
    public CompileKotlinOperation compileOptions(String... option) {
        compileOptions_.addAll(Arrays.asList(option));
        return this;
    }

    /**
     * Retrieves the list of compilation options for the {@code kotlinc} compiler.
     *
     * @return the list of compiler options
     */
    public List<String> compileOptions() {
        return compileOptions_;
    }

    /**
     * Provides entries for the test compilation classpath.
     *
     * @param classpath classpath entries
     * @return this operation instance
     */
    public CompileKotlinOperation compileTestClasspath(String... classpath) {
        compileTestClasspath_.addAll(Arrays.asList(classpath));
        return this;
    }

    /**
     * Provides a list of entries for the test compilation classpath.
     *
     * @param classpath a list of classpath entries
     * @return this operation instance
     */
    public CompileKotlinOperation compileTestClasspath(List<String> classpath) {
        compileTestClasspath_.addAll(classpath);
        return this;
    }

    /**
     * Retrieves the list of entries for the test compilation classpath.
     *
     * @return the test compilation classpath list
     */
    public List<String> compileTestClasspath() {
        return compileTestClasspath_;
    }


    /**
     * Performs the compile operation.
     */
    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public void execute()
            throws IOException {
        executeCreateBuildDirectories();
        executeBuildMainSources();
        executeBuildTestSources();
        if (!silent()) {
            System.out.println("Kotlin compilation finished successfully.");
        }
    }

    /**
     * Part of the {@link #execute} operation, builds the main sources.
     */
    protected void executeBuildMainSources()
            throws IOException {
        var sources = new ArrayList<>(mainSourceFiles());
        for (var directory : mainSourceDirectories()) {
            sources.addAll(getKotlinFileList(directory));
        }
        executeBuildSources(
                compileMainClasspath(),
                sources,
                buildMainDirectory());
    }

    /**
     * Part of the {@link #execute} operation, build sources to a destination.
     *
     * @param classpath   the classpath list used for the compilation
     * @param sources     the source files to compile
     * @param destination the destination directory
     */
    protected void executeBuildSources(List<String> classpath, List<File> sources, File destination)
            throws IOException {
        if (sources.isEmpty() || destination == null) {
            return;
        }

        var k2 = new K2JVMCompiler();
        var args = new ArrayList<String>();

        // classpath
        args.add("-cp");
        args.add(FileUtils.joinPaths(classpath));

        // destination
        args.add("-d");
        args.add(destination.getAbsolutePath());

        args.add("-no-reflect");
        args.add("-no-stdlib");

        // options
        args.addAll(compileOptions());

        // source
        sources.forEach(f -> args.add(f.getAbsolutePath()));

        if (LOGGER.isLoggable(Level.FINE) && !silent()) {
            LOGGER.fine("kotlinc " + String.join(" ", args));
        }

        var exitCode = k2.exec(System.err, args.toArray(new String[0]));
        if (exitCode.getCode() != 0) {
            throw new IOException("Kotlin compilation failed.");
        }
    }

    /**
     * Part of the {@link #execute} operation, builds the test sources.
     */
    protected void executeBuildTestSources()
            throws IOException {
        var sources = new ArrayList<>(testSourceFiles());
        for (var directory : testSourceDirectories()) {
            sources.addAll(getKotlinFileList(directory));
        }
        executeBuildSources(
                compileTestClasspath(),
                sources,
                buildTestDirectory());
    }

    /**
     * Part of the {@link #execute} operation, creates the build directories.
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
     *
     * @param project the project to configure the compile operation from
     */
    public CompileKotlinOperation fromProject(BaseProject project) {
        var srcMainKotlinDirectory = new File(project.srcMainDirectory(), "kotlin");
        var srcTestKotlinDirectory = new File(project.srcTestDirectory(), "kotlin");

        return buildMainDirectory(project.buildMainDirectory())
                .buildTestDirectory(project.buildTestDirectory())
                .compileMainClasspath(project.compileMainClasspath())
                .compileTestClasspath(project.compileTestClasspath())
                .mainSourceFiles(getKotlinFileList(srcMainKotlinDirectory))
                .testSourceFiles(getKotlinFileList(srcTestKotlinDirectory));
    }

    public String getMessage() {
        return "Hello World!";
    }

    /**
     * Provides main source directories that should be compiled.
     *
     * @param directories main source directories
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceDirectories(File... directories) {
        mainSourceDirectories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Provides a list of main source directories that should be compiled.
     *
     * @param directories a list of main source directories
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceDirectories(List<File> directories) {
        mainSourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Retrieves the list of main source directories that should be compiled.
     *
     * @return the list of main source directories to compile
     */
    public List<File> mainSourceDirectories() {
        return mainSourceDirectories_;
    }

    /**
     * Provides main files that should be compiled.
     *
     * @param files main files
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceFiles(File... files) {
        mainSourceFiles_.addAll(Arrays.asList(files));
        return this;
    }

    /**
     * Provides a list of main files that should be compiled.
     *
     * @param files a list of main files
     * @return this operation instance
     */
    public CompileKotlinOperation mainSourceFiles(List<File> files) {
        mainSourceFiles_.addAll(files);
        return this;
    }

    /**
     * Retrieves the list of main files that should be compiled.
     *
     * @return the list of main files to compile
     */
    public List<File> mainSourceFiles() {
        return mainSourceFiles_;
    }

    /**
     * Provides test source directories that should be compiled.
     *
     * @param directories test source directories
     * @return this operation instance
     */
    public CompileKotlinOperation testSourceDirectories(File... directories) {
        testSourceDirectories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Provides a list of test source directories that should be compiled.
     *
     * @param directories a list of test source directories
     * @return this operation instance
     */
    public CompileKotlinOperation testSourceDirectories(List<File> directories) {
        testSourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Retrieves the list of test source directories that should be compiled.
     *
     * @return the list of test source directories to compile
     */
    public List<File> testSourceDirectories() {
        return testSourceDirectories_;
    }

    /**
     * Provides test files that should be compiled.
     *
     * @param files test files
     * @return this operation instance
     */
    public CompileKotlinOperation testSourceFiles(File... files) {
        testSourceFiles_.addAll(Arrays.asList(files));
        return this;
    }

    /**
     * Provides a list of test files that should be compiled.
     *
     * @param files a list of test files
     * @return this operation instance
     */
    public CompileKotlinOperation testSourceFiles(List<File> files) {
        testSourceFiles_.addAll(files);
        return this;
    }

    /**
     * Retrieves the list of test files that should be compiled.
     *
     * @return the list of test files to compile
     */
    public List<File> testSourceFiles() {
        return testSourceFiles_;
    }
}