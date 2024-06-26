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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static rife.bld.extension.CompileKotlinOperation.isNotBlank;

/**
 * Configuration for the Kotlin compiler options.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class CompileKotlinOptions {
    private final List<String> advancedOptions_ = new ArrayList<>();
    private final List<String> argFile_ = new ArrayList<>();
    private final List<String> classpath_ = new ArrayList<>();
    private final List<String> jvmOptions_ = new ArrayList<>();
    private final List<String> optIn_ = new ArrayList<>();
    private final List<String> options_ = new ArrayList<>();
    private final List<String> plugin_ = new ArrayList<>();
    private final List<String> scriptTemplates_ = new ArrayList<>();
    private String apiVersion_;
    private String expression_;
    private boolean includeRuntime_;
    private boolean javaParameters_;
    private String jdkHome_;
    private String jdkRelease_;
    private String jvmTarget_;
    private String kotlinHome_;
    private String languageVersion_;
    private String moduleName_;
    private boolean noJdk_;
    private boolean noReflect_;
    private boolean noStdLib_;
    private boolean noWarn_;
    private String path_;
    private boolean progressive_;
    private boolean verbose_;
    private boolean wError_;

    /**
     * Specify advanced compiler options.
     *
     * @param options one or more advanced options
     * @return this operation instance
     */
    public CompileKotlinOptions advancedOptions(String... options) {
        Collections.addAll(advancedOptions_, options);
        return this;
    }

    /**
     * Specify advanced compiler options.
     *
     * @param options list of compiler options
     * @return this operation instance
     */
    public CompileKotlinOptions advancedOptions(Collection<String> options) {
        advancedOptions_.addAll(options);
        return this;
    }

    /**
     * Allow using declarations only from the specified version of Kotlin bundled libraries.
     *
     * @param version the api version
     * @return this operation instance
     */
    public CompileKotlinOptions apiVersion(String version) {
        apiVersion_ = version;
        return this;
    }

    /**
     * Allow using declarations only from the specified version of Kotlin bundled libraries.
     *
     * @param version the api version
     * @return this operation instance
     */
    public CompileKotlinOptions apiVersion(int version) {
        apiVersion_ = String.valueOf(version);
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
     */
    public CompileKotlinOptions argFile(String... files) {
        Collections.addAll(argFile_, files);
        return this;
    }

    /**
     * Read the compiler options from the given files.
     *
     * @param files the list of files
     * @return this operation instance
     * @see #argFile(String...)
     */
    public CompileKotlinOptions argFile(Collection<String> files) {
        argFile_.addAll(files);
        return this;
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
            argFile_.forEach(f -> args.add("@" + f));
        }

        // classpath
        if (!classpath_.isEmpty()) {
            args.add("-classpath");
            args.add(String.join(File.pathSeparator, classpath_));
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
        if (isNotBlank(jdkHome_)) {
            args.add("-jdk-home");
            args.add(jdkHome_);
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
        if (isNotBlank(kotlinHome_)) {
            args.add("-kotlin-home");
            args.add(kotlinHome_);
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
        if (isNotBlank(path_)) {
            args.add("-d");
            args.add(path_);
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

        // Werror
        if (wError_) {
            args.add("-Werror");
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
     */
    public CompileKotlinOptions classpath(String... paths) {
        Collections.addAll(classpath_, paths);
        return this;
    }

    /**
     * Search for class files in the specified paths.
     * <p>
     * The classpath can contain file and directory paths, ZIP, or JAR files.
     *
     * @param paths the list of paths
     * @return this operation instance
     */
    public CompileKotlinOptions classpath(Collection<String> paths) {
        classpath_.addAll(paths);
        return this;
    }

    /**
     * Evaluate the given string as a Kotlin script.
     *
     * @param expression the expression
     * @return this operation instance
     */
    public CompileKotlinOptions expression(String expression) {
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
    public CompileKotlinOptions includeRuntime(boolean includeRuntime) {
        includeRuntime_ = includeRuntime;
        return this;
    }

    /**
     * Generate metadata for Java 1.8 reflection on method parameters.
     *
     * @param javaParameters {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileKotlinOptions javaParameters(boolean javaParameters) {
        javaParameters_ = javaParameters;
        return this;
    }

    /**
     * Use a custom JDK home directory to include into the classpath if it differs from the default {@code JAVA_HOME}.
     *
     * @param jdkHome the JDK home path
     * @return this operation instance
     */
    public CompileKotlinOptions jdkHome(String jdkHome) {
        jdkHome_ = jdkHome;
        return this;
    }

    /**
     * Specify the target version of the generated JVM bytecode.
     * <p>
     * Limit the API of the JDK in the classpath to the specified Java version. Automatically sets
     * {@link #jvmTarget(String) JVM target} version.
     * <p>
     * Possible values are 1.8, 9, 10, ..., 21. The default value is 1.8.
     *
     * @param version the target version
     * @return this operation instance
     */
    public CompileKotlinOptions jdkRelease(String version) {
        jdkRelease_ = version;
        return this;
    }

    /**
     * Specify the target version of the generated JVM bytecode.
     *
     * @param version the target version
     * @return this operation instance
     * @see #jdkRelease(String)
     */
    public CompileKotlinOptions jdkRelease(int version) {
        jdkRelease_ = String.valueOf(version);
        return this;
    }

    /**
     * Pass an option directly to JVM
     *
     * @param jvmOptions one or more JVM option
     * @return this operation instance
     */
    public CompileKotlinOptions jvmOptions(String... jvmOptions) {
        Collections.addAll(jvmOptions_, jvmOptions);
        return this;
    }

    /**
     * Pass an option directly to JVM
     *
     * @param jvmOptions the list JVM options
     * @return this operation instance
     */
    public CompileKotlinOptions jvmOptions(Collection<String> jvmOptions) {
        jvmOptions_.addAll(jvmOptions);
        return this;
    }

    /**
     * Specify the target version of the generated JVM bytecode.
     * <p>
     * Possible values are 1.8, 9, 10, ..., 21. The default value is 1.8.
     *
     * @param target the target version
     * @return this operation instance
     */
    public CompileKotlinOptions jvmTarget(String target) {
        jvmTarget_ = target;
        return this;
    }

    /**
     * Specify the target version of the generated JVM bytecode.
     *
     * @param target the target version
     * @return this operation instance
     * @see #jvmTarget(String)
     */
    public CompileKotlinOptions jvmTarget(int target) {
        jvmTarget_ = String.valueOf(target);
        return this;
    }

    /**
     * Specify a custom path to the Kotlin compiler used for the discovery of runtime libraries.
     *
     * @param path the Kotlin home path
     * @return this operation instance
     */
    public CompileKotlinOptions kotlinHome(String path) {
        kotlinHome_ = path;
        return this;
    }

    /**
     * Provide source compatibility with the specified version of Kotlin.
     *
     * @param version the language version
     * @return this operation instance
     */
    public CompileKotlinOptions languageVersion(String version) {
        languageVersion_ = version;
        return this;
    }

    /**
     * Set a custom name for the generated {@code .kotlin_module} file.
     *
     * @param name the module name
     * @return this operation instance
     */
    public CompileKotlinOptions moduleName(String name) {
        moduleName_ = name;
        return this;
    }

    /**
     * Don't automatically include the Java runtime into the classpath.
     *
     * @param noJdk {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileKotlinOptions noJdk(boolean noJdk) {
        noJdk_ = noJdk;
        return this;
    }

    /**
     * Don't automatically include the Kotlin reflection ({@code kotlin-reflect.jar}) into the classpath.
     *
     * @param noReflect {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileKotlinOptions noReflect(boolean noReflect) {
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
    public CompileKotlinOptions noStdLib(boolean noStdLib) {
        noStdLib_ = noStdLib;
        return this;
    }

    /**
     * Suppress the compiler from displaying warnings during compilation.
     *
     * @param noWarn {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileKotlinOptions noWarn(boolean noWarn) {
        noWarn_ = noWarn;
        return this;
    }

    /**
     * Enable usages of API that requires opt-in with a requirement annotation with the given fully qualified name.
     *
     * @param annotations one or more annotation names
     * @return this operation instance
     */
    public CompileKotlinOptions optIn(String... annotations) {
        Collections.addAll(optIn_, annotations);
        return this;
    }

    /**
     * Enable usages of API that requires opt-in with a requirement annotation with the given fully qualified name.
     *
     * @param annotations list of annotation names
     * @return this operation instance
     */
    public CompileKotlinOptions optIn(Collection<String> annotations) {
        optIn_.addAll(annotations);
        return this;
    }

    /**
     * Specify additional compiler options.
     *
     * @param options one or more compiler options
     * @return this operation instance
     */
    public CompileKotlinOptions options(String... options) {
        Collections.addAll(options_, options);
        return this;
    }

    /**
     * Specify additional compiler options.
     *
     * @param options list of compiler options
     * @return this operation instance
     */
    public CompileKotlinOptions options(Collection<String> options) {
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
    public CompileKotlinOptions path(File path) {
        path_ = path.getAbsolutePath();
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
    public CompileKotlinOptions path(String path) {
        path_ = path;
        return this;
    }

    /**
     * Pass an option to a plugin.
     *
     * @param id         the plugin ID
     * @param optionName the plugin option name
     * @param value      the plugin option value
     * @return this operation instance
     */
    public CompileKotlinOptions plugin(String id, String optionName, String value) {
        plugin_.add(id + ':' + optionName + ':' + value);
        return this;
    }

    /**
     * Allow using declarations only from the specified version of Kotlin bundled libraries.
     *
     * @param progressive {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileKotlinOptions progressive(boolean progressive) {
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
    public CompileKotlinOptions scriptTemplates(String... classNames) {
        Collections.addAll(scriptTemplates_, classNames);
        return this;
    }

    /**
     * Script definition template classes.
     * <p>
     * Use fully qualified class names.
     *
     * @param classNames the list class names
     * @return this operation instance
     */
    public CompileKotlinOptions scriptTemplates(Collection<String> classNames) {
        scriptTemplates_.addAll(classNames);
        return this;
    }

    /**
     * Enable verbose logging output which includes details of the compilation process.
     *
     * @param verbose {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileKotlinOptions verbose(boolean verbose) {
        verbose_ = verbose;
        return this;
    }

    /**
     * Turn any warnings into a compilation error.
     *
     * @param wError {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileKotlinOptions wError(boolean wError) {
        wError_ = wError;
        return this;
    }
}
