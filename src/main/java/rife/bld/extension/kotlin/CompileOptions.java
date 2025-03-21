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

package rife.bld.extension.kotlin;

import rife.bld.extension.CompileKotlinOperation;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static rife.bld.extension.CompileKotlinOperation.isNotBlank;

/**
 * Configuration for the Kotlin compiler options.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class CompileOptions {
    private final Collection<String> advancedOptions_ = new ArrayList<>();
    private final Collection<File> argFile_ = new ArrayList<>();
    private final Collection<File> classpath_ = new ArrayList<>();
    private final JvmOptions jvmOptions_ = new JvmOptions();
    private final Collection<String> optIn_ = new ArrayList<>();
    private final Collection<String> options_ = new ArrayList<>();
    private final Collection<String> plugin_ = new ArrayList<>();
    private final Collection<String> scriptTemplates_ = new ArrayList<>();
    private String apiVersion_;
    private String expression_;
    private boolean includeRuntime_;
    private boolean javaParameters_;
    private File jdkHome_;
    private String jdkRelease_;
    private String jvmTarget_;
    private File kotlinHome_;
    private String languageVersion_;
    private String moduleName_;
    private boolean noJdk_;
    private boolean noReflect_;
    private boolean noStdLib_;
    private boolean noWarn_;
    private File path_;
    private boolean progressive_;
    private boolean verbose_;
    private boolean wError_;
    private boolean wExtra_;

    /**
     * Specify advanced compiler options.
     *
     * @param options one or more advanced options
     * @return this operation instance
     */
    public CompileOptions advancedOptions(String... options) {
        return advancedOptions(List.of(options));
    }

    /**
     * Specify advanced compiler options.
     *
     * @param options the compiler options
     * @return this operation instance
     */
    public CompileOptions advancedOptions(Collection<String> options) {
        advancedOptions_.addAll(options);
        return this;
    }

    /**
     * Retrieves advanced compiler options.
     *
     * @return the advanced compiler options
     */
    public Collection<String> advancedOptions() {
        return advancedOptions_;
    }

    /**
     * Retrieves the version of Kotlin bundled libraries.
     *
     * @return the API version
     */
    public String apiVersion() {
        return apiVersion_;
    }

    /**
     * Allow using declarations only from the specified version of Kotlin bundled libraries.
     *
     * @param version the API version
     * @return this operation instance
     */
    public CompileOptions apiVersion(String version) {
        apiVersion_ = version;
        return this;
    }


    /**
     * Allow using declarations only from the specified version of Kotlin bundled libraries.
     *
     * @param version the API version
     * @return this operation instance
     */
    public CompileOptions apiVersion(int version) {
        return apiVersion(String.valueOf(version));
    }

    /**
     * Read the compiler options from the given files.
     * <p>
     * Such a file can contain compiler options with values and paths to the source files.
     * Options and paths should be separated by whitespaces. For example:
     * <ul>
     * <li>{@code -include-runtime -d hello.jar hello.kt}</li>
     * </ul>
     * To pass values that contain whitespaces, surround them with single ({@code '}) or double ({@code "}) quotes.
     * If a value contains quotation marks in it, escape them with a backslash (\).
     * <ul>
     * <li>{@code -include-runtime -d 'My folder'}</li>
     * </ul>
     * If the files reside in locations different from the current directory, use relative paths.
     *
     * @param files one or more files
     * @return this operation instance
     * @see #argFileStrings(Collection)
     */
    public CompileOptions argFile(String... files) {
        return argFileStrings(List.of(files));
    }

    /**
     * Read the compiler options from the given files.
     *
     * @param files the compiler options files
     * @return this operation instance
     * @see #argFile(File...)
     */
    public CompileOptions argFile(Collection<File> files) {
        argFile_.addAll(files);
        return this;
    }

    /**
     * Read the compiler options from the given files.
     * <p>
     * Such a file can contain compiler options with values and paths to the source files.
     * Options and paths should be separated by whitespaces. For example:
     * <ul>
     * <li>{@code -include-runtime -d hello.jar hello.kt}</li>
     * </ul>
     * To pass values that contain whitespaces, surround them with single ({@code '}) or double ({@code "}) quotes.
     * If a value contains quotation marks in it, escape them with a backslash (\).
     * <ul>
     * <li>{@code -include-runtime -d 'My folder'}</li>
     * </ul>
     * If the files reside in locations different from the current directory, use relative paths.
     *
     * @param files one or more files
     * @return this operation instance
     * @see #argFile(Collection)
     */
    public CompileOptions argFile(File... files) {
        return argFile(List.of(files));
    }

    /**
     * Read the compiler options from the given files.
     * <p>
     * Such a file can contain compiler options with values and paths to the source files.
     * Options and paths should be separated by whitespaces. For example:
     * <ul>
     * <li>{@code -include-runtime -d hello.jar hello.kt}</li>
     * </ul>
     * To pass values that contain whitespaces, surround them with single ({@code '}) or double ({@code "}) quotes.
     * If a value contains quotation marks in it, escape them with a backslash (\).
     * <ul>
     * <li>{@code -include-runtime -d 'My folder'}</li>
     * </ul>
     * If the files reside in locations different from the current directory, use relative paths.
     *
     * @param files one or more files
     * @return this operation instance
     * @see #argFilePaths(Collection)
     */
    public CompileOptions argFile(Path... files) {
        return argFilePaths(List.of(files));
    }

    /**
     * Retrieves the files containing compiler options.
     *
     * @return the compiler options files
     */
    public Collection<File> argFile() {
        return argFile_;
    }

    /**
     * Read the compiler options from the given files.
     *
     * @param files the compiler options files
     * @return this operation instance
     * @see #argFile(Path...)
     */
    public CompileOptions argFilePaths(Collection<Path> files) {
        return argFile(files.stream().map(Path::toFile).toList());
    }

    /**
     * Read the compiler options from the given files.
     *
     * @param files the compiler options files
     * @return this operation instance
     * @see #argFile(String...)
     */
    public CompileOptions argFileStrings(Collection<String> files) {
        return argFile(files.stream().map(File::new).toList());
    }

    /**
     * Returns the formatted arguments.
     *
     * @return the arguments
     */
    public List<String> args() {
        var args = new ArrayList<String>();

        // api-version
        if (isNotBlank(apiVersion_)) {
            args.add("-api-version");
            args.add(apiVersion_);
        }

        // @argfile
        if (!argFile_.isEmpty()) {
            argFile_.forEach(f -> args.add("@" + f.getAbsolutePath()));
        }

        // classpath
        if (!classpath_.isEmpty()) {
            args.add("-classpath");
            args.add(classpath_.stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
        }

        // expression
        if (isNotBlank(expression_)) {
            args.add("-expression");
            args.add(expression_);
        }

        // java-parameters
        if (javaParameters_) {
            args.add("-java-parameters");
        }

        // jvm-target
        if (isNotBlank(jvmTarget_)) {
            args.add("-jvm-target");
            args.add(jvmTarget_);
        }

        // include-runtime
        if (includeRuntime_) {
            args.add("-include-runtime");
        }

        // jdk-home
        if (jdkHome_ != null) {
            args.add("-jdk-home");
            args.add(jdkHome_.getAbsolutePath());
        }

        // jdk-release
        if (isNotBlank(jdkRelease_)) {
            args.add("-Xjdk-release=" + jdkRelease_);
        }

        // JVM options
        if (!jvmOptions_.isEmpty()) {
            jvmOptions_.forEach(s -> args.add("-J" + s));
        }

        // kotlin-home
        if (kotlinHome_ != null) {
            args.add("-kotlin-home");
            args.add(kotlinHome_.getAbsolutePath());
        }

        // language-version
        if (isNotBlank(languageVersion_)) {
            args.add("-language-version");
            args.add(languageVersion_);
        }

        // module-name
        if (isNotBlank(moduleName_)) {
            args.add("-module-name");
            args.add(moduleName_);
        }

        // no-jdk
        if (noJdk_) {
            args.add("-no-jdk");
        }

        // no-reflect
        if (noReflect_) {
            args.add("-no-reflect");
        }

        // no-std-lib
        if (noStdLib_) {
            args.add("-no-stdlib");
        }

        // no-warn
        if (noWarn_) {
            args.add("-nowarn");
        }

        // opt-in
        optIn_.stream().filter(CompileKotlinOperation::isNotBlank).forEach(o -> {
            args.add("-opt-in");
            args.add(o);
        });

        // options
        if (!options_.isEmpty()) {
            args.addAll(options_);
        }

        // path
        if (path_ != null) {
            args.add("-d");
            args.add(path_.getAbsolutePath());
        }

        // plugin
        plugin_.stream().filter(CompileKotlinOperation::isNotBlank).forEach(p -> {
            args.add("-P");
            args.add("plugin:" + p);
        });

        // progressive
        if (progressive_) {
            args.add("-progressive");
        }

        // script-templates
        if (!scriptTemplates_.isEmpty()) {
            args.add("-script-templates");
            args.add(String.join(",", scriptTemplates_));
        }

        // verbose
        if (verbose_) {
            args.add("-verbose");
        }

        // Wwrror
        if (wError_) {
            args.add("-Werror");
        }

        // Wextra
        if (wExtra_) {
            args.add("-Wextra");
        }

        // advanced option (X)
        if (!advancedOptions_.isEmpty()) {
            advancedOptions_.forEach(it -> args.add("-X" + it));
        }

        return args;
    }

    /**
     * Search for class files in the specified paths.
     * <p>
     * The classpath can contain file and directory paths, ZIP, or JAR files.
     *
     * @param paths one pr more paths
     * @return this operation instance
     * @see #classpathStrings(Collection)
     */
    public CompileOptions classpath(String... paths) {
        return classpathStrings(List.of(paths));
    }

    /**
     * Search for class files in the specified paths.
     * <p>
     * The classpath can contain file and directory paths, ZIP, or JAR files.
     *
     * @param paths one or more path
     * @return this operation instance
     * @see #classpath(Collection)
     */
    public CompileOptions classpath(File... paths) {
        return classpath(List.of(paths));
    }

    /**
     * Search for class files in the specified paths.
     * <p>
     * The classpath can contain file and directory paths, ZIP, or JAR files.
     *
     * @param paths one or more path
     * @return this operation instance
     * @see #classpathPaths(Collection)
     */
    public CompileOptions classpath(Path... paths) {
        return classpathPaths(List.of(paths));
    }

    /**
     * Search for class files in the specified paths.
     * <p>
     * The classpath can contain file and directory paths, ZIP, or JAR files.
     *
     * @param paths the search paths
     * @return this operation instance
     * @see #classpath(File...)
     */
    public CompileOptions classpath(Collection<File> paths) {
        classpath_.addAll(paths);
        return this;
    }

    /**
     * Retrieves the class files classpath.
     *
     * @return the class files classpath
     */
    public Collection<File> classpath() {
        return classpath_;
    }

    /**
     * Search for class files in the specified paths.
     * <p>
     * The classpath can contain file and directory paths, ZIP, or JAR files.
     *
     * @param paths one pr more paths
     * @return this operation instance
     * @see #classpath(Path...)
     */
    public CompileOptions classpathPaths(Collection<Path> paths) {
        return classpath(paths.stream().map(Path::toFile).toList());
    }

    /**
     * Search for class files in the specified paths.
     * <p>
     * The classpath can contain file and directory paths, ZIP, or JAR files.
     *
     * @param paths one pr more paths
     * @return this operation instance
     * @see #classpath(String...)
     */
    public CompileOptions classpathStrings(Collection<String> paths) {
        return classpath(paths.stream().map(File::new).toList());
    }

    /**
     * Retrieves the string to evaluate as a Kotlin script.
     *
     * @return the expression
     */
    public String expression() {
        return expression_;
    }

    /**
     * Evaluate the given string as a Kotlin script.
     *
     * @param expression the expression
     * @return this operation instance
     */
    public CompileOptions expression(String expression) {
        expression_ = expression;
        return this;
    }

    /**
     * Indicates whether the {@link #jdkRelease(String) jdkRelease} was set.
     *
     * @return {@code true} if the release was set; or {@code false} otherwise
     */
    public boolean hasRelease() {
        return jdkRelease_ != null;
    }

    /**
     * Include the Kotlin runtime into the resulting JAR file. Makes the resulting archive runnable on any Java-enabled
     * environment.
     *
     * @param includeRuntime {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileOptions includeRuntime(boolean includeRuntime) {
        includeRuntime_ = includeRuntime;
        return this;
    }

    /**
     * Indicates whether the {@link #includeRuntime(boolean)} was set.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isIncludeRuntime() {
        return includeRuntime_;
    }

    /**
     * Indicates whether {@link #javaParameters(boolean)} was set.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isJavaParameters() {
        return javaParameters_;
    }

    /**
     * Indicates whether {@link #noJdk(boolean) noJdk} was set.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isNoJdk() {
        return noJdk_;
    }

    /**
     * Indicates whether {@link #noReflect(boolean) noRflect} was set.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isNoReflect() {
        return noReflect_;
    }

    /**
     * Indicates whether {@link #noStdLib(boolean) noStdLib} +was set.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isNoStdLib() {
        return noStdLib_;
    }

    /**
     * Indicates whether {@link #noWarn(boolean) noWarn} was set.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isNoWarn() {
        return noWarn_;
    }

    /**
     * Indicates whether {@link #progressive(boolean) progressive} was set.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isProgressive() {
        return progressive_;
    }

    /**
     * Indicates whether {@link #verbose(boolean)} was set.
     *
     * @return {@code true} if verbose was set; or {@code false} otherwise
     */
    public boolean isVerbose() {
        return verbose_;
    }

    /**
     * Indicates whether warnings are turned into a compilation error.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isWError() {
        return wError_;
    }

    /**
     * Indicates whether additional declaration, expression, and type compiler checks emit warnings.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isWExtra() {
        return wExtra_;
    }

    /**
     * Generate metadata for Java 1.8 reflection on method parameters.
     *
     * @param javaParameters {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileOptions javaParameters(boolean javaParameters) {
        javaParameters_ = javaParameters;
        return this;
    }

    /**
     * Use a custom JDK home directory to include into the classpath if it differs from the default {@code JAVA_HOME}.
     *
     * @param jdkHome the JDK home path
     * @return this operation instance
     */
    public CompileOptions jdkHome(File jdkHome) {
        jdkHome_ = jdkHome;
        return this;
    }

    /**
     * Use a custom JDK home directory to include into the classpath if it differs from the default {@code JAVA_HOME}.
     *
     * @param jdkHome the JDK home path
     * @return this operation instance
     */
    public CompileOptions jdkHome(String jdkHome) {
        return jdkHome(new File(jdkHome));
    }

    /**
     * Use a custom JDK home directory to include into the classpath if it differs from the default {@code JAVA_HOME}.
     *
     * @param jdkHome the JDK home path
     * @return this operation instance
     */
    public CompileOptions jdkHome(Path jdkHome) {
        return jdkHome(jdkHome.toFile());
    }

    /**
     * Retrieves the custom JDK home directory.
     *
     * @return the JDK home path.
     */
    public File jdkHome() {
        return jdkHome_;
    }

    /**
     * Return the specified JDK API version.
     *
     * @return the API version
     */
    public String jdkRelease() {
        return jdkRelease_;
    }

    /**
     * Compile against the specified JDK API version.
     * <p>
     * Limit the API of the JDK in the classpath to the specified Java version. Automatically sets
     * {@link #jvmTarget(String) JVM target} version.
     * <p>
     * Possible values are 1.8, 9, 10, ..., 22. The default value is 1.8.
     *
     * @param version the target version
     * @return this operation instance
     */
    public CompileOptions jdkRelease(String version) {
        jdkRelease_ = version;
        return this;
    }

    /**
     * Compile against the specified JDK API version.
     * <p>
     * Limit the API of the JDK in the classpath to the specified Java version. Automatically sets
     * {@link #jvmTarget(String) JVM target} version.
     * <p>
     * Possible values are 1.8, 9, 10, ..., 22. The default value is 1.8.
     *
     * @param version the target version
     * @return this operation instance
     * @see #jdkRelease(String)
     */
    public CompileOptions jdkRelease(int version) {
        return jdkRelease(String.valueOf(version));
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
     * Pass an option directly to the Java Virtual Machine
     *
     * @param jvmOptions the JVM options
     * @return this operation instance
     */
    public CompileOptions jvmOptions(Collection<String> jvmOptions) {
        jvmOptions_.addAll(jvmOptions);
        return this;
    }

    /**
     * Pass an option directly to the Java Virtual Machine
     *
     * @param jvmOptions one or more JVM option
     * @return this operation instance
     */
    public CompileOptions jvmOptions(String... jvmOptions) {
        return jvmOptions(List.of(jvmOptions));
    }

    /**
     * Specify the target version of the generated JVM bytecode.
     *
     * @param target the target version
     * @return this operation instance
     * @see #jvmTarget(String)
     */
    public CompileOptions jvmTarget(int target) {
        return jvmTarget(String.valueOf(target));
    }

    /**
     * Specify the target version of the generated JVM bytecode.
     * <p>
     * Possible values are 1.8, 9, 10, ..., 22. The default value is 1.8.
     *
     * @param target the target version
     * @return this operation instance
     */
    public CompileOptions jvmTarget(String target) {
        jvmTarget_ = target;
        return this;
    }

    /**
     * Retrieves the target version of the generated JVM bytecode.
     *
     * @return the target version
     */
    public String jvmTarget() {
        return jvmTarget_;
    }

    /**
     * Specify a custom path to the Kotlin compiler used for the discovery of runtime libraries.
     *
     * @param path the Kotlin home path
     * @return this operation instance
     */
    public CompileOptions kotlinHome(File path) {
        kotlinHome_ = path;
        return this;
    }

    /**
     * Retrieves the custom path of the Kotlin compiler.
     *
     * @return the Kotlin home path
     */
    public File kotlinHome() {
        return kotlinHome_;
    }

    /**
     * Specify a custom path to the Kotlin compiler used for the discovery of runtime libraries.
     *
     * @param path the Kotlin home path
     * @return this operation instance
     */
    public CompileOptions kotlinHome(Path path) {
        return kotlinHome(path.toFile());
    }

    /**
     * Specify a custom path to the Kotlin compiler used for the discovery of runtime libraries.
     *
     * @param path the Kotlin home path
     * @return this operation instance
     */
    public CompileOptions kotlinHome(String path) {
        return kotlinHome(new File(path));
    }

    /**
     * Provide source compatibility with the specified version of Kotlin.
     *
     * @param version the language version
     * @return this operation instance
     */
    public CompileOptions languageVersion(String version) {
        languageVersion_ = version;
        return this;
    }

    /**
     * Retrieves the {@link #languageVersion(String) language version}.
     *
     * @return the language version
     */
    public String languageVersion() {
        return languageVersion_;
    }

    /**
     * Set a custom name for the generated {@code .kotlin_module} file.
     *
     * @param name the module name
     * @return this operation instance
     */
    public CompileOptions moduleName(String name) {
        moduleName_ = name;
        return this;
    }

    /**
     * Retrieves the {@link #moduleName(String) module name}.
     *
     * @return the module name
     */
    public String moduleName() {
        return moduleName_;
    }

    /**
     * Don't automatically include the Java runtime into the classpath.
     *
     * @param noJdk {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileOptions noJdk(boolean noJdk) {
        noJdk_ = noJdk;
        return this;
    }

    /**
     * Don't automatically include the Kotlin reflection ({@code kotlin-reflect.jar}) into the classpath.
     *
     * @param noReflect {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileOptions noReflect(boolean noReflect) {
        noReflect_ = noReflect;
        return this;
    }

    /**
     * Don't automatically include the Kotlin/JVM stdlib ({@code kotlin-stdlib.jar}) and Kotlin reflection
     * ({@code kotlin-reflect.jar}) into the classpath.
     *
     * @param noStdLib {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileOptions noStdLib(boolean noStdLib) {
        noStdLib_ = noStdLib;
        return this;
    }

    /**
     * Suppress the compiler from displaying warnings during compilation.
     *
     * @param noWarn {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileOptions noWarn(boolean noWarn) {
        noWarn_ = noWarn;
        return this;
    }

    /**
     * Enable usages of API that requires opt-in with a requirement annotation with the given fully qualified name.
     *
     * @param annotations one or more annotation names
     * @return this operation instance
     */
    public CompileOptions optIn(String... annotations) {
        return optIn(List.of(annotations));
    }

    /**
     * Retrieves the opt-in fully qualified names.
     *
     * @return the fully qualified names
     */
    public Collection<String> optIn() {
        return optIn_;
    }

    /**
     * Enable usages of API that requires opt-in with a requirement annotation with the given fully qualified name.
     *
     * @param annotations the annotation names
     * @return this operation instance
     */
    public CompileOptions optIn(Collection<String> annotations) {
        optIn_.addAll(annotations);
        return this;
    }

    /**
     * Specify additional compiler options.
     *
     * @param options one or more compiler options
     * @return this operation instance
     */
    public CompileOptions options(String... options) {
        return options(List.of(options));
    }

    /**
     * Retrieves additional compiler options.
     *
     * @return the compiler options
     */
    public Collection<String> options() {
        return options_;
    }

    /**
     * Specify additional compiler options.
     *
     * @param options the compiler options
     * @return this operation instance
     */
    public CompileOptions options(Collection<String> options) {
        options_.addAll(options);
        return this;
    }

    /**
     * Place the generated class files into the specified location.
     * <p>
     * The location can be a directory, a ZIP, or a JAR file.
     *
     * @param path the location path
     * @return this operation instance
     */
    public CompileOptions path(File path) {
        path_ = path;
        return this;
    }

    /**
     * Retrieves the location to place generated class files into.
     *
     * @return the location path.
     */
    public File path() {
        return path_;
    }

    /**
     * Place the generated class files into the specified location.
     * <p>
     * The location can be a directory, a ZIP, or a JAR file.
     *
     * @param path the location path
     * @return this operation instance
     */
    public CompileOptions path(Path path) {
        return path(path.toFile());
    }

    /**
     * Place the generated class files into the specified location.
     * <p>
     * The location can be a directory, a ZIP, or a JAR file.
     *
     * @param path the location path
     * @return this operation instance
     */
    public CompileOptions path(String path) {
        return path(new File(path));
    }

    /**
     * Pass an option to a plugin.
     *
     * @param id         the plugin ID
     * @param optionName the plugin option name
     * @param value      the plugin option value
     * @return this operation instance
     */
    public CompileOptions plugin(String id, String optionName, String value) {
        plugin_.add(id + ':' + optionName + ':' + value);
        return this;
    }

    /**
     * Retrieves the plugin options.
     *
     * @return the plugin options.
     */
    public Collection<String> plugin() {
        return plugin_;
    }

    /**
     * Allow using declarations only from the specified version of Kotlin bundled libraries.
     *
     * @param progressive {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileOptions progressive(boolean progressive) {
        progressive_ = progressive;
        return this;
    }

    /**
     * Script definition template classes.
     * <p>
     * Use fully qualified class names.
     *
     * @param classNames one or more class names
     * @return this operation instance
     */
    public CompileOptions scriptTemplates(String... classNames) {
        return scriptTemplates(List.of(classNames));
    }

    /**
     * Retrieves the script templates.
     *
     * @return the script templates.
     */
    public Collection<String> scriptTemplates() {
        return scriptTemplates_;
    }

    /**
     * Script definition template classes.
     * <p>
     * Use fully qualified class names.
     *
     * @param classNames the class names
     * @return this operation instance
     */
    public CompileOptions scriptTemplates(Collection<String> classNames) {
        scriptTemplates_.addAll(classNames);
        return this;
    }

    /**
     * Enable verbose logging output which includes details of the compilation process.
     *
     * @param verbose {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileOptions verbose(boolean verbose) {
        verbose_ = verbose;
        return this;
    }

    /**
     * Turn any warnings into a compilation error.
     *
     * @param wError {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileOptions wError(boolean wError) {
        wError_ = wError;
        return this;
    }

    /**
     * Enable additional declaration, expression, and type compiler checks that emit warnings if {@code true}.
     *
     * @param wExtra {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileOptions wExtra(boolean wExtra) {
        wExtra_ = wExtra;
        return this;
    }
}
