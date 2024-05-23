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

package rife.bld.extension.dokka;

import rife.bld.BaseProject;
import rife.bld.extension.CompileKotlinOperation;
import rife.bld.operations.AbstractProcessOperation;
import rife.tools.StringUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static rife.bld.extension.CompileKotlinOperation.isNotBlank;

/**
 * Builds documentation (javadoc, HTML, etc.) using Dokka.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class DokkaOperation extends AbstractProcessOperation<DokkaOperation> {
    private final static String GFM_PLUGIN_REGEXP =
            "^.*(dokka-base|analysis-kotlin-descriptors|gfm-plugin|freemarker).*\\.jar$";
    private final static String HTML_PLUGIN_REGEXP =
            "^.*(dokka-base|analysis-kotlin-descriptors|kotlinx-html-jvm|freemarker).*\\.jar$";
    private final static String JAVADOC_PLUGIN_REGEXP =
            "^.*(dokka-base|analysis-kotlin-descriptors|javadoc-plugin|kotlin-as-java-plugin|korte-jvm).*\\.jar$";
    private final static String JEKYLL_PLUGIN_REGEXP =
            "^.*(dokka-base|analysis-kotlin-descriptors|jekyll-plugin|gfm-plugin|freemarker).*\\.jar$";
    private final Logger LOGGER = Logger.getLogger(DokkaOperation.class.getName());
    private final Map<String, String> globalLinks_ = new ConcurrentHashMap<>();
    private final Collection<String> globalPackageOptions_ = new ArrayList<>();
    private final Collection<String> globalSrcLinks_ = new ArrayList<>();
    private final Collection<String> includes_ = new ArrayList<>();
    private final Map<String, String> pluginsConfiguration_ = new ConcurrentHashMap<>();
    private final Collection<String> pluginsClasspath_ = new ArrayList<>();
    private boolean delayTemplateSubstitution_;
    private boolean failOnWarning_;
    private LoggingLevel loggingLevel_;
    private String moduleName_;
    private String moduleVersion_;
    private boolean noSuppressObviousFunctions_;
    private boolean offlineMode_;
    private File outputDir_;
    private BaseProject project_;
    private SourceSet sourceSet_;
    private boolean suppressInheritedMembers_;

    // Encodes to JSON adding braces as needed
    private static String encodeJson(final String json) {
        var sb = new StringBuilder(json);
        if (!json.startsWith("{") || !json.endsWith("}")) {
            sb.insert(0, "{").append("}");
        }
        return StringUtils.encodeJson(sb.toString());
    }

    /**
     * Sets the delay substitution of some elements.
     * <p>
     * Used in incremental builds of multimodule projects.
     *
     * @param delayTemplateSubstitution the delay
     * @return this operation instance
     */
    public DokkaOperation delayTemplateSubstitution(Boolean delayTemplateSubstitution) {
        delayTemplateSubstitution_ = delayTemplateSubstitution;
        return this;
    }

    /**
     * Part of the {@link #execute execute} operation, constructs the command list to use for building the process.
     *
     * @since 1.5
     */
    @Override
    protected List<String> executeConstructProcessCommandList() {
        if (project_ == null) {
            throw new IllegalArgumentException("A project must be specified.");
        }

        final List<String> args = new ArrayList<>();

        // java
        args.add(javaTool());

        var cli = CompileKotlinOperation.getJarList(project_.libBldDirectory(), "^.*dokka-cli.*\\.jar$");

        if (cli.size() != 1) {
            throw new RuntimeException("The dokka-cli JAR could not be found.");
        }

        // -jar dokka-cli
        args.add("-jar");
        args.add(cli.get(0));

        // -pluginClasspath
        if (!pluginsClasspath_.isEmpty()) {
            args.add("-pluginsClasspath");
            args.add(String.join(";", pluginsClasspath_));
        }

        // -sourceSet
        var sourceSetArgs = sourceSet_.args();
        if (sourceSetArgs.isEmpty()) {
            throw new IllegalArgumentException("At least one sourceSet is required.");
        } else {
            args.add("-sourceSet");
            args.add(String.join(" ", sourceSet_.args()));
        }

        // -outputDir
        if (outputDir_ != null) {
            if (!outputDir_.exists() && !outputDir_.mkdirs()) {
                throw new RuntimeException("Could not create: " + outputDir_.getAbsolutePath());
            }

            args.add("-outputDir");
            args.add(outputDir_.getAbsolutePath());
        }

        // -delayTemplateSubstitution
        if (delayTemplateSubstitution_) {
            args.add("-delayTemplateSubstitution");
        }

        // -failOnWarning
        if (failOnWarning_) {
            args.add("-failOnWarning");
        }

        // -globalLinks_
        if (!globalLinks_.isEmpty()) {
            args.add("-globalLinks");
            var links = new ArrayList<String>();
            globalLinks_.forEach((k, v) ->
                    links.add(String.format("%s^%s", k, v)));
            args.add(String.join("^^", links));
        }

        // -globalPackageOptions
        if (!globalPackageOptions_.isEmpty()) {
            args.add("-globalPackageOptions");
            args.add(String.join(";", globalPackageOptions_));
        }

        // -globalSrcLinks
        if (!globalSrcLinks_.isEmpty()) {
            args.add("-globalSrcLinks_");
            args.add(String.join(";", globalSrcLinks_));
        }

        // -includes
        if (!includes_.isEmpty()) {
            args.add("-includes");
            args.add(String.join(";", includes_));
        }

        // -loggingLevel
        if (loggingLevel_ != null) {
            args.add("-loggingLevel");
            args.add(loggingLevel_.name().toLowerCase());
        }

        // -moduleName
        if (isNotBlank(moduleName_)) {
            args.add("-moduleName");
            args.add(moduleName_);
        }

        // -moduleVersion
        if (isNotBlank(moduleVersion_)) {
            args.add("-moduleVersion");
            args.add(moduleVersion_);
        }

        // -noSuppressObviousFunctions
        if (noSuppressObviousFunctions_) {
            args.add("-noSuppressObviousFunctions");
        }

        // -offlineMode
        if (offlineMode_) {
            args.add("-offlineMode");
        }

        // -pluginConfiguration
        if (!pluginsConfiguration_.isEmpty()) {
            args.add("-pluginsConfiguration");
            var confs = new ArrayList<String>();
            pluginsConfiguration_.forEach((k, v) ->
                    confs.add(String.format("%s=%s", encodeJson(k), encodeJson(v))));
            args.add(String.join("^^", confs));
        }

        // -suppressInheritedMembers
        if (suppressInheritedMembers_) {
            args.add("-suppressInheritedMembers");
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.join(" ", args));
        }

        return args;
    }

    /**
     * Configures the operation from a {@link BaseProject}.
     * <p>
     * Sets the {@link #sourceSet sourceSet}, {@link SourceSet#jdkVersion jdkVersion} and {@link #moduleName moduleName}
     * from the project.
     *
     * @param project the project to configure the operation from
     */
    @Override
    public DokkaOperation fromProject(BaseProject project) {
        project_ = project;
        sourceSet_ = new SourceSet().src(new File(project.srcMainDirectory(), "kotlin").getAbsolutePath());
        if (project.javaRelease() != null) {
            sourceSet_ = sourceSet_.jdkVersion(project.javaRelease());
        }
        moduleName_ = project.name();
        return this;
    }

    /**
     * Sets whether to fail documentation generation if Dokka has emitted a warning or an error.
     * <p>
     * Whether to fail documentation generation if Dokka has emitted a warning or an error. The process waits until all
     * errors and warnings have been emitted first.
     * <p>
     * This setting works well with {@link SourceSet#reportUndocumented}
     *
     * @param failOnWarning {@code true} or {@code false}
     * @return this operation instance
     */
    public DokkaOperation failOnWarning(Boolean failOnWarning) {
        failOnWarning_ = failOnWarning;
        return this;
    }

    /**
     * Set the global external documentation links.
     *
     * @param url            the external documentation URL
     * @param packageListUrl the external documentation package list URL
     * @return this operation instance
     */
    public DokkaOperation globalLinks(String url, String packageListUrl) {
        globalLinks_.put(url, packageListUrl);
        return this;
    }

    /**
     * Set the global external documentation links.
     *
     * @param globalLinks the map of global links
     * @return this operation instance
     * @see #globalSrcLink(String...) #globalSrcLink(String...)#globalSrcLink(String...)
     */
    public DokkaOperation globalLinks(Map<String, String> globalLinks) {
        globalLinks_.putAll(globalLinks);
        return this;
    }

    /**
     * Sets the global list of package configurations.
     * <p>
     * Using format:
     * <ul>
     * <li>matchingRegexp</li>
     * <li>-deprecated</li>
     * <li>-privateApi</li>
     * <li>+warnUndocumented</li>
     * <li>+suppress</li>
     * <li>+visibility:PUBLIC</li>
     * <li>...</li>
     * </ul>
     *
     * @param options ome pr more package configurations
     * @return this operation instance
     */
    public DokkaOperation globalPackageOptions(String... options) {
        globalPackageOptions_.addAll(Arrays.asList(options));
        return this;
    }

    /**
     * Sets the global list of package configurations.
     * <p>
     * Using format:
     * <ul>
     * <li>matchingRegexp</li>
     * <li>-deprecated</li>
     * <li>-privateApi</li>
     * <li>+warnUndocumented</li>
     * <li>+suppress</li>
     * <li>+visibility:PUBLIC</li>
     * <li>...</li>
     * </ul>
     *
     * @param options the list of package configurations
     * @return this operation instance
     */
    public DokkaOperation globalPackageOptions(Collection<String> options) {
        globalPackageOptions_.addAll(options);
        return this;
    }

    /**
     * Sets the global mapping between a source directory and a Web service for browsing the code.
     *
     * @param links one or more links mapping
     * @return this operation instance
     */
    public DokkaOperation globalSrcLink(String... links) {
        globalSrcLinks_.addAll(Arrays.asList(links));
        return this;
    }

    /**
     * Sets the global mapping between a source directory and a Web service for browsing the code.
     *
     * @param links the links mapping
     * @return this operation instance
     */
    public DokkaOperation globalSrcLink(Collection<String> links) {
        globalSrcLinks_.addAll(links);
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The contents of specified files are parsed and embedded into documentation as module and package descriptions.
     * <p>
     * This can be configured on per-package basis.
     *
     * @param files one or more files
     * @return this operation instance
     */
    public DokkaOperation includes(String... files) {
        includes_.addAll(Arrays.asList(files));
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The contents of specified files are parsed and embedded into documentation as module and package descriptions.
     * <p>
     * This can be configured on per-package basis.
     *
     * @param files the list of files
     * @return this operation instance
     */
    public DokkaOperation includes(Collection<String> files) {
        includes_.addAll(files);
        return this;
    }

    /**
     * Sets the logging level.
     *
     * @param loggingLevel the logging level
     * @return this operation instance
     */
    public DokkaOperation loggingLevel(LoggingLevel loggingLevel) {
        loggingLevel_ = loggingLevel;
        return this;
    }

    /**
     * Sets the name of the project/module. Default is {@code root}.
     * <p>
     * The display name used to refer to the module. It is used for the table of contents, navigation, logging, etc.
     *
     * @param moduleName the project/module name
     * @return this operation instance
     */
    public DokkaOperation moduleName(String moduleName) {
        moduleName_ = moduleName;
        return this;
    }

    /**
     * Set the documented version.
     *
     * @param version the version
     * @return this operation instance
     */
    public DokkaOperation moduleVersion(String version) {
        moduleVersion_ = version;
        return this;
    }

    /**
     * Sets whether to suppress obvious functions such as inherited from
     * <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/">kotlin.Any</a> and {@link java.lang.Object}.
     * <p>
     * A function is considered to be obvious if it is:
     * <ul>
     * <li>Inherited from <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/">kotlin.Any</a>,
     * <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/">Kotlin.Enum</a>, {@link java.lang.Object}
     * or {@link java.lang.Enum}, such as {@code equals}, {@code hashCode}, {@code toString}.
     * <li>Synthetic (generated by the compiler) and does not have any documentation, such as
     * {@code dataClass.componentN} or {@code dataClass.copy}.
     * </ul>
     *
     * @param noSuppressObviousFunctions {@code true} or {@code false}
     * @return this operation instance
     */
    public DokkaOperation noSuppressObviousFunctions(Boolean noSuppressObviousFunctions) {
        noSuppressObviousFunctions_ = noSuppressObviousFunctions;
        return this;
    }

    /**
     * Sets whether to resolve remote files/links over network.
     * <p>
     * This includes package-lists used for generating external documentation links. For example, to make classes from
     * the standard library clickable.
     * <p>
     * Setting this to true can significantly speed up build times in certain cases, but can also worsen documentation
     * quality and user experience. For example, by not resolving class/member links from your dependencies, including
     * the standard library.
     * <p>
     * Note: You can cache fetched files locally and provide them to Dokka as local paths.
     *
     * @param offlineMode the offline mode
     * @return this operation instance
     * @see SourceSet#externalDocumentationLinks(String, String)
     */
    public DokkaOperation offlineMode(Boolean offlineMode) {
        offlineMode_ = offlineMode;
        return this;
    }

    /**
     * Sets the output directory path, {@code ./dokka} by default.
     * <p>
     * The directory to where documentation is generated, regardless of output format.
     *
     * @param outputDir the output directory
     * @return this operation instance
     */
    public DokkaOperation outputDir(File outputDir) {
        outputDir_ = outputDir;
        return this;
    }

    /**
     * Sets the output directory path, {@code ./dokka} by default.
     *
     * @param outputDir the output directory
     * @return this operation instance
     */
    public DokkaOperation outputDir(String outputDir) {
        outputDir_ = new File(outputDir);
        return this;
    }

    /**
     * Sets the Dokka {@link OutputFormat output format}.
     *
     * @param format The {@link OutputFormat output format}
     * @return this operation instance
     */
    public DokkaOperation outputFormat(OutputFormat format) {
        pluginsClasspath_.clear();
        if (format.equals(OutputFormat.JAVADOC)) {
            pluginsClasspath_.addAll(CompileKotlinOperation.getJarList(project_.libBldDirectory(),
                    JAVADOC_PLUGIN_REGEXP));
        } else if (format.equals(OutputFormat.HTML)) {
            pluginsClasspath_.addAll(CompileKotlinOperation.getJarList(project_.libBldDirectory(),
                    HTML_PLUGIN_REGEXP));
        } else if (format.equals(OutputFormat.MARKDOWN)) {
            pluginsClasspath_.addAll(CompileKotlinOperation.getJarList(project_.libBldDirectory(),
                    GFM_PLUGIN_REGEXP));
        } else if (format.equals(OutputFormat.JEKYLL)) {
            pluginsClasspath_.addAll(CompileKotlinOperation.getJarList(project_.libBldDirectory(),
                    JEKYLL_PLUGIN_REGEXP));
        }
        return this;
    }

    /**
     * Sets the configuration for Dokka plugins.
     *
     * @param name              The fully-qualified plugin name
     * @param jsonConfiguration The plugin JSON configuration
     * @return this operation instance
     */
    public DokkaOperation pluginConfigurations(String name, String jsonConfiguration) {
        pluginsConfiguration_.put(name, jsonConfiguration);
        return this;
    }

    /**
     * Sets the configuration for Dokka plugins.
     *
     * @param pluginConfiguratione the map of configurations
     * @return this operation instance
     * @see #pluginConfigurations(String, String)
     */
    public DokkaOperation pluginConfigurations(Map<String, String> pluginConfiguratione) {
        pluginsConfiguration_.putAll(pluginConfiguratione);
        return this;
    }

    /**
     * Sets the list of jars with Dokka plugins and their dependencies.
     *
     * @param jars one or more jars
     * @return this operation instance
     */
    public DokkaOperation pluginsClasspath(String... jars) {
        pluginsClasspath_.addAll(Arrays.asList(jars));
        return this;
    }

    /**
     * Sets the list of jars with Dokka plugins and their dependencies.
     *
     * @param jars the list of jars
     * @return this operation instance
     */
    public DokkaOperation pluginsClasspath(Collection<String> jars) {
        pluginsClasspath_.addAll(jars);
        return this;
    }

    /**
     * Clears the list of Dokka plugins.
     *
     * @param clear set to clear the list
     * @return this operation instance
     */
    public DokkaOperation pluginsClasspath(boolean clear) {
        if (clear) {
            pluginsClasspath_.clear();
        }
        return this;
    }

    /**
     * Sets the configurations for a source set.
     * <p>
     * Individual and additional configuration of Kotlin source sets.
     *
     * @param sourceSet the source set configurations
     * @return this operation instance
     */
    public DokkaOperation sourceSet(SourceSet sourceSet) {
        sourceSet_ = sourceSet;
        return this;
    }

    /**
     * Sets whether to suppress inherited members that aren't explicitly overridden in a given class.
     *
     * @param suppressInheritedMembers {@code true} or {@code false}
     * @return this operation instance
     */
    public DokkaOperation suppressInheritedMembers(Boolean suppressInheritedMembers) {
        suppressInheritedMembers_ = suppressInheritedMembers;
        return this;
    }
}
