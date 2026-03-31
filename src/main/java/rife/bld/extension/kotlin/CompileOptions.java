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

package rife.bld.extension.kotlin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import rife.bld.extension.tools.CollectionTools;
import rife.bld.extension.tools.ObjectTools;
import rife.bld.extension.tools.TextTools;
import rife.bld.operations.AbstractToolProviderOperation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for the Kotlin compiler options.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class CompileOptions {

    private static final Logger LOGGER = Logger.getLogger(CompileOptions.class.getName());

    private final List<String> advancedOptions_ = new ArrayList<>();
    private final List<File> argFile_ = new ArrayList<>();
    private final List<File> classpath_ = new ArrayList<>();
    private final List<String> optIn_ = new ArrayList<>();
    private final List<String> options_ = new ArrayList<>();
    private final List<String> plugin_ = new ArrayList<>();
    private final List<String> scriptTemplates_ = new ArrayList<>();

    private String apiVersion_;
    private String expression_;
    private boolean includeRuntime_;
    private boolean javaParameters_;
    private File jdkHome_;
    private String jdkRelease_;
    private JvmDefault jvmDefault_;
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

    @Override
    public String toString() {
        return "CompileOptions{" +
                "advancedOptions_=" + advancedOptions_ +
                ", argFile_=" + argFile_ +
                ", classpath_=" + classpath_ +
                ", optIn_=" + optIn_ +
                ", options_=" + options_ +
                ", plugin_=" + plugin_ +
                ", scriptTemplates_=" + scriptTemplates_ +
                ", apiVersion_='" + apiVersion_ + '\'' +
                ", expression_='" + expression_ + '\'' +
                ", includeRuntime_=" + includeRuntime_ +
                ", javaParameters_=" + javaParameters_ +
                ", jdkHome_=" + jdkHome_ +
                ", jdkRelease_='" + jdkRelease_ + '\'' +
                ", jvmDefault_=" + jvmDefault_ +
                ", jvmTarget_='" + jvmTarget_ + '\'' +
                ", kotlinHome_=" + kotlinHome_ +
                ", languageVersion_='" + languageVersion_ + '\'' +
                ", moduleName_='" + moduleName_ + '\'' +
                ", noJdk_=" + noJdk_ +
                ", noReflect_=" + noReflect_ +
                ", noStdLib_=" + noStdLib_ +
                ", noWarn_=" + noWarn_ +
                ", path_=" + path_ +
                ", progressive_=" + progressive_ +
                ", verbose_=" + verbose_ +
                ", wError_=" + wError_ +
                ", wExtra_=" + wExtra_ +
                '}';
    }

    /**
     * Appends {@code flag value} to {@code args} when {@code value} is not blank.
     */
    private static void addFlag(List<String> args, String flag, String value) {
        if (TextTools.isNotBlank(flag, value)) {
            args.add(flag);
            args.add(value);
        }
    }

    /**
     * Appends {@code flag} to {@code args} when {@code condition} is {@code true}.
     * <p>
     * The blank-flag guard was removed: every call site passes a compile-time
     * string literal, so checking {@code TextTools.isNotBlank(flag)} on every
     * invocation was pure overhead.
     */
    private static void addFlag(List<String> args, String flag, boolean condition) {
        if (condition) {
            args.add(flag);
        }
    }

    /**
     * Specify advanced compiler options.
     *
     * @param options one or more advanced options
     * @return this operation instance
     */
    public CompileOptions advancedOptions(String... options) {
        if (ObjectTools.isNotEmpty(options)) {
            advancedOptions_.addAll(List.of(options));
        }
        return this;
    }

    /**
     * Specify advanced compiler options.
     *
     * @param options the compiler options
     * @return this operation instance
     */
    @SafeVarargs
    public final CompileOptions advancedOptions(Collection<String>... options) {
        advancedOptions_.addAll(CollectionTools.combine(options));
        return this;
    }

    /**
     * Retrieves advanced compiler options.
     *
     * @return the advanced compiler options
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<String> advancedOptions() {
        return advancedOptions_;
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
     * Retrieves the version of Kotlin bundled libraries.
     *
     * @return the API version
     */
    public String apiVersion() {
        return apiVersion_;
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
    public CompileOptions argFile(String... files) {
        argFile_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * Read the compiler options from the given files.
     *
     * @param files one or more files
     * @return this operation instance
     */
    public CompileOptions argFile(File... files) {
        if (ObjectTools.isNotEmpty(files)) {
            argFile_.addAll(List.of(files));
        }
        return this;
    }

    /**
     * Read the compiler options from the given files.
     *
     * @param files one or more files
     * @return this operation instance
     */
    public CompileOptions argFile(Path... files) {
        argFile_.addAll(CollectionTools.combinePathsToFiles(files));
        return this;
    }

    /**
     * Read the compiler options from the given files.
     *
     * @param files the compiler options files
     * @return this operation instance
     */
    @SafeVarargs
    public final CompileOptions argFile(Collection<File>... files) {
        argFile_.addAll(CollectionTools.combine(files));
        return this;
    }

    /**
     * Retrieves the files containing compiler options.
     *
     * @return the compiler options files
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> argFile() {
        return argFile_;
    }

    /**
     * Read the compiler options from the given files.
     *
     * @param files the compiler options files (as {@link Path})
     * @return this operation instance
     */
    @SafeVarargs
    public final CompileOptions argFilePaths(Collection<Path>... files) {
        argFile_.addAll(CollectionTools.combinePathsToFiles(files));
        return this;
    }

    /**
     * Read the compiler options from the given files.
     *
     * @param files the compiler options files (as {@link String})
     * @return this operation instance
     */
    @SafeVarargs
    public final CompileOptions argFileStrings(Collection<String>... files) {
        argFile_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * Returns the formatted arguments.
     * <p>
     * The list is pre-sized to 32 entries to avoid repeated internal array
     * copies for the typical number of compiler flags.
     *
     * @return the arguments
     */
    public List<String> args() {
        // Pre-sized to avoid ArrayList reallocation for the typical flag count.
        var args = new ArrayList<String>(32);

        // Version flags
        addFlag(args, "-api-version", apiVersion_);
        addFlag(args, "-language-version", languageVersion_);

        addArgFileArgs(args);
        addJvmArgs(args);
        addLibraryArgs(args);
        addOutputArgs(args);

        // Warning / diagnostic flags
        addFlag(args, "-nowarn", noWarn_);
        addFlag(args, "-progressive", progressive_);
        addFlag(args, "-verbose", verbose_);
        addFlag(args, "-Werror", wError_);
        addFlag(args, "-Wextra", wExtra_);

        addMiscArgs(args);
        return args;
    }

    /**
     * Search for class files in the specified paths.
     * <p>
     * The classpath can contain file and directory paths, ZIP, or JAR files.
     *
     * @param paths one or more paths
     * @return this operation instance
     */
    public CompileOptions classpath(String... paths) {
        classpath_.addAll(CollectionTools.combineStringsToFiles(paths));
        return this;
    }

    /**
     * Search for class files in the specified paths.
     * <p>
     * The classpath can contain file and directory paths, ZIP, or JAR files.
     *
     * @param paths one or more paths
     * @return this operation instance
     */
    public CompileOptions classpath(File... paths) {
        if (ObjectTools.isNotEmpty(paths)) {
            classpath_.addAll(List.of(paths));
        }
        return this;
    }

    /**
     * Search for class files in the specified paths.
     * <p>
     * The classpath can contain file and directory paths, ZIP, or JAR files.
     *
     * @param paths one or more paths
     * @return this operation instance
     */
    public CompileOptions classpath(Path... paths) {
        classpath_.addAll(CollectionTools.combinePathsToFiles(paths));
        return this;
    }

    /**
     * Search for class files in the specified paths.
     * <p>
     * The classpath can contain file and directory paths, ZIP, or JAR files.
     *
     * @param paths the search paths
     * @return this operation instance
     */
    @SafeVarargs
    public final CompileOptions classpath(Collection<File>... paths) {
        classpath_.addAll(CollectionTools.combine(paths));
        return this;
    }

    /**
     * Retrieves the class files classpath.
     *
     * @return the class files classpath
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> classpath() {
        return classpath_;
    }

    /**
     * Search for class files in the specified paths.
     *
     * @param paths the search paths (as {@link Path})
     * @return this operation instance
     */
    @SafeVarargs
    public final CompileOptions classpathPaths(Collection<Path>... paths) {
        classpath_.addAll(CollectionTools.combinePathsToFiles(paths));
        return this;
    }

    /**
     * Search for class files in the specified paths.
     *
     * @param paths the search paths (as {@link String})
     * @return this operation instance
     */
    @SafeVarargs
    public final CompileOptions classpathStrings(Collection<String>... paths) {
        classpath_.addAll(CollectionTools.combineStringsToFiles(paths));
        return this;
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
     * Retrieves the string to evaluate as a Kotlin script.
     *
     * @return the expression
     */
    public String expression() {
        return expression_;
    }

    /**
     * Indicates whether {@link #jdkRelease(String)} was set.
     *
     * @return {@code true} if the release was set; or {@code false} otherwise
     */
    public boolean hasRelease() {
        return jdkRelease_ != null;
    }

    /**
     * Indicates whether {@link #jvmTarget(String)} was set.
     *
     * @return {@code true} if the target was set; or {@code false} otherwise
     */
    public boolean hasTarget() {
        return jvmTarget_ != null;
    }

    /**
     * Include the Kotlin runtime in the resulting JAR file.
     *
     * @param includeRuntime {@code true} or {@code false}
     * @return this operation instance
     */
    public CompileOptions includeRuntime(boolean includeRuntime) {
        includeRuntime_ = includeRuntime;
        return this;
    }

    /**
     * Indicates whether {@link #includeRuntime(boolean)} was set.
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
     * Indicates whether {@link #noJdk(boolean)} was set.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isNoJdk() {
        return noJdk_;
    }

    /**
     * Indicates whether {@link #noReflect(boolean)} was set.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isNoReflect() {
        return noReflect_;
    }

    /**
     * Indicates whether {@link #noStdLib(boolean)} was set.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isNoStdLib() {
        return noStdLib_;
    }

    /**
     * Indicates whether {@link #noWarn(boolean)} was set.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isNoWarn() {
        return noWarn_;
    }

    /**
     * Indicates whether {@link #progressive(boolean)} was set.
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
     * Use a custom JDK home directory to include in the classpath if it differs from the default {@code JAVA_HOME}.
     *
     * @param jdkHome the JDK home path
     * @return this operation instance
     */
    public CompileOptions jdkHome(File jdkHome) {
        jdkHome_ = jdkHome;
        return this;
    }

    /**
     * Use a custom JDK home directory to include in the classpath if it differs from the default {@code JAVA_HOME}.
     *
     * @param jdkHome the JDK home path
     * @return this operation instance
     */
    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "caller controls path")
    public CompileOptions jdkHome(String jdkHome) {
        Objects.requireNonNull(jdkHome, "JDK home path cannot be null");
        return jdkHome(new File(jdkHome));
    }

    /**
     * Use a custom JDK home directory to include in the classpath if it differs from the default {@code JAVA_HOME}.
     *
     * @param jdkHome the JDK home path
     * @return this operation instance
     */
    public CompileOptions jdkHome(Path jdkHome) {
        Objects.requireNonNull(jdkHome, "JDK home path cannot be null");
        return jdkHome(jdkHome.toFile());
    }

    /**
     * Retrieves the custom JDK home directory.
     *
     * @return the JDK home path
     */
    public File jdkHome() {
        return jdkHome_;
    }

    /**
     * Compile against the specified JDK API version.
     * <p>
     * Limit the API of the JDK in the classpath to the specified Java version. Automatically sets
     * {@link #jvmTarget(String) JVM target} version.
     * <p>
     * Possible values are 1.8, 9, 10, ..., 23. The default value is 1.8.
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
     *
     * @param version the target version
     * @return this operation instance
     * @see #jdkRelease(String)
     */
    public CompileOptions jdkRelease(int version) {
        return jdkRelease(String.valueOf(version));
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
     * Emit JVM default methods for interface declarations with bodies.
     *
     * @param jvmDefault the default methods option
     * @return this operation instance
     * @since 1.1.0
     */
    public CompileOptions jvmDefault(JvmDefault jvmDefault) {
        jvmDefault_ = jvmDefault;
        return this;
    }

    /**
     * Retrieves the JVM default methods option.
     *
     * @return the default methods option
     * @since 1.1.0
     */
    public JvmDefault jvmDefault() {
        return jvmDefault_;
    }

    /**
     * Specify the target version of the generated JVM bytecode.
     * <p>
     * Possible values are 1.8, 9, 10, ..., 23. The default value is 1.8.
     *
     * @param target the target version
     * @return this operation instance
     */
    public CompileOptions jvmTarget(String target) {
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
    public CompileOptions jvmTarget(int target) {
        return jvmTarget(String.valueOf(target));
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
     * Specify a custom path to the Kotlin compiler used for the discovery of runtime libraries.
     *
     * @param path the Kotlin home path
     * @return this operation instance
     */
    public CompileOptions kotlinHome(Path path) {
        Objects.requireNonNull(path, "Kotlin home path cannot be null");
        return kotlinHome(path.toFile());
    }

    /**
     * Specify a custom path to the Kotlin compiler used for the discovery of runtime libraries.
     *
     * @param path the Kotlin home path
     * @return this operation instance
     */
    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "caller controls path")
    public CompileOptions kotlinHome(String path) {
        Objects.requireNonNull(path, "Kotlin home path cannot be null");
        return kotlinHome(new File(path));
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
        if (ObjectTools.isNotEmpty(annotations)) {
            optIn_.addAll(List.of(annotations));
        }
        return this;
    }

    /**
     * Enable usages of API that requires opt-in with a requirement annotation with the given fully qualified name.
     *
     * @param annotations the annotation names
     * @return this operation instance
     */
    @SafeVarargs
    public final CompileOptions optIn(Collection<String>... annotations) {
        optIn_.addAll(CollectionTools.combine(annotations));
        return this;
    }

    /**
     * Retrieves the opt-in fully qualified names.
     *
     * @return the fully qualified names
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<String> optIn() {
        return optIn_;
    }

    /**
     * Specify additional compiler options.
     *
     * @param options one or more compiler options
     * @return this operation instance
     */
    public CompileOptions options(String... options) {
        if (ObjectTools.isNotEmpty(options)) {
            options_.addAll(List.of(options));
        }
        return this;
    }

    /**
     * Specify additional compiler options.
     *
     * @param options the compiler options
     * @return this operation instance
     */
    @SafeVarargs
    public final CompileOptions options(Collection<String>... options) {
        options_.addAll(CollectionTools.combine(options));
        return this;
    }

    /**
     * Retrieves additional compiler options.
     *
     * @return the compiler options
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<String> options() {
        return options_;
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
     * Place the generated class files into the specified location.
     * <p>
     * The location can be a directory, a ZIP, or a JAR file.
     *
     * @param path the location path
     * @return this operation instance
     */
    public CompileOptions path(Path path) {
        Objects.requireNonNull(path, "Path cannot be null");
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
    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "caller controls path")
    public CompileOptions path(String path) {
        Objects.requireNonNull(path, "Path cannot be null");
        return path(new File(path));
    }

    /**
     * Retrieves the location to place generated class files into.
     *
     * @return the location path
     */
    public File path() {
        return path_;
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
        if (TextTools.isNotBlank(id, optionName, value)) {
            plugin_.add(id + ':' + optionName + ':' + value);
        }
        return this;
    }

    /**
     * Retrieves the plugin options.
     *
     * @return the plugin options
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<String> plugin() {
        return plugin_;
    }

    /**
     * Enable progressive compilation mode.
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
        if (ObjectTools.isNotEmpty(classNames)) {
            scriptTemplates_.addAll(List.of(classNames));
        }
        return this;
    }

    /**
     * Script definition template classes.
     * <p>
     * Use fully qualified class names.
     *
     * @param classNames the class names
     * @return this operation instance
     */
    @SafeVarargs
    public final CompileOptions scriptTemplates(Collection<String>... classNames) {
        scriptTemplates_.addAll(CollectionTools.combine(classNames));
        return this;
    }

    /**
     * Retrieves the script templates.
     *
     * @return the script templates
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<String> scriptTemplates() {
        return scriptTemplates_;
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

    /**
     * Reads and inlines tokens from each arg file.
     * <p>
     * {@link AbstractToolProviderOperation.CommandLineTokenizer} wraps the
     * {@link java.io.BufferedReader} passed to it and does not hold additional
     * closeable resources of its own, so closing the reader is sufficient.
     */
    private void addArgFileArgs(List<String> args) {
        for (var f : argFile_) {
            if (f.exists()) {
                try (var reader = Files.newBufferedReader(f.toPath(), Charset.defaultCharset())) {
                    var tokenizer = new AbstractToolProviderOperation.CommandLineTokenizer(reader); // NOPMD
                    String token;
                    while ((token = tokenizer.nextToken()) != null) {
                        args.add(token);
                    }
                } catch (IOException e) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, "Could not read: " + f.getAbsolutePath(), e);
                    }
                }
            } else if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("File not found: " + f.getAbsolutePath());
            }
        }
    }

    /**
     * Adds JVM-targeting flags.
     */
    private void addJvmArgs(List<String> args) {
        addFlag(args, "-expression", expression_);
        addFlag(args, "-java-parameters", javaParameters_);
        addFlag(args, "-jvm-target", jvmTarget_);
        addFlag(args, "-include-runtime", includeRuntime_);

        if (jdkHome_ != null) {
            args.add("-jdk-home");
            args.add(jdkHome_.getAbsolutePath());
        }

        if (TextTools.isNotBlank(jdkRelease_)) {
            args.add("-Xjdk-release=" + jdkRelease_);
        }

        if (jvmDefault_ != null) {
            args.add("-jvm-default=" + jvmDefault_.getValue());
        }
    }

    /**
     * Adds library/classpath-related flags.
     */
    private void addLibraryArgs(List<String> args) {
        if (kotlinHome_ != null) {
            args.add("-kotlin-home");
            args.add(kotlinHome_.getAbsolutePath());
        }

        addFlag(args, "-module-name", moduleName_);
        addFlag(args, "-no-jdk", noJdk_);
        addFlag(args, "-no-reflect", noReflect_);
        addFlag(args, "-no-stdlib", noStdLib_);
    }

    /**
     * Adds advanced ({@code -X}) options.
     * <p>
     * Options that do not already start with {@code -X} are silently prefixed.
     * Uses a plain loop instead of a stream to avoid lambda and iterator
     * allocation overhead on what is typically a small list.
     */
    private void addMiscArgs(List<String> args) {
        for (var opt : advancedOptions_) {
            args.add(opt.startsWith("-X") ? opt : "-X" + opt);
        }
    }

    /**
     * Adds output path and plugin flags.
     * <p>
     * Plain loops replace stream pipelines throughout to avoid lambda and
     * iterator allocation on typically small lists. Empty-list guards keep
     * the behaviour consistent with the {@code options_} pattern already
     * present in the original code.
     */
    private void addOutputArgs(List<String> args) {
        if (path_ != null) {
            args.add("-d");
            args.add(path_.getAbsolutePath());
        }

        if (!optIn_.isEmpty()) {
            for (var o : optIn_) {
                if (TextTools.isNotBlank(o)) {
                    args.add("-opt-in");
                    args.add(o);
                }
            }
        }

        if (!options_.isEmpty()) {
            args.addAll(options_);
        }

        if (!plugin_.isEmpty()) {
            for (var p : plugin_) {
                if (TextTools.isNotBlank(p)) {
                    args.add("-P");
                    args.add("plugin:" + p);
                }
            }
        }

        if (!scriptTemplates_.isEmpty()) {
            args.add("-script-templates");
            args.add(String.join(",", scriptTemplates_));
        }
    }
}