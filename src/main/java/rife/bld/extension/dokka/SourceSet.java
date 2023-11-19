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

package rife.bld.extension.dokka;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for a Dokka source set.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class SourceSet {
    private static final String SEMICOLON = ";";
    private final Collection<String> classpath_ = new ArrayList<>();
    private final Map<String, String> dependentSourceSets_ = new ConcurrentHashMap<>();
    private final Collection<DocumentedVisibility> documentedVisibilities_ = new ArrayList<>();
    private final Map<String, String> externalDocumentationLinks_ = new ConcurrentHashMap<>();
    private final Collection<String> includes_ = new ArrayList<>();
    private final Collection<String> perPackageOptions_ = new ArrayList<>();
    private final Collection<String> samples_ = new ArrayList<>();
    private final Map<String, String> srcLinks_ = new ConcurrentHashMap<>();
    private final Collection<String> src_ = new ArrayList<>();
    private final Collection<String> suppressedFiles_ = new ArrayList<>();
    private AnalysisPlatform analysisPlatform_;
    private String apiVersion_;
    private String displayName_;
    private int jdkVersion_;
    private String languageVersion_;
    private boolean noJdkLink_;
    private boolean noSkipEmptyPackages_;
    private boolean noStdlibLink_;
    private boolean reportUndocumented_;
    private boolean skipDeprecated_;
    private String sourceSetName_;

    /**
     * Sets the platform used for setting up analysis. Default is {@link AnalysisPlatform#JVM JVM}
     * <p>
     * Platform to be used for setting up code analysis and {@code @sample} environment.
     *
     * @param analysisPlatform the analysis platform
     * @return this operation instance
     */
    public SourceSet analysisPlatform(AnalysisPlatform analysisPlatform) {
        analysisPlatform_ = analysisPlatform;
        return this;
    }

    /**
     * Sets the Kotlin API version used for setting up analysis and samples.
     *
     * @param apiVersion the api version
     * @return this operation instance
     */
    public SourceSet apiVersion(String apiVersion) {
        apiVersion_ = apiVersion;
        return this;
    }

    /**
     * Returns the formatted arguments.
     *
     * @return the arguments
     */
    public List<String> args() {
        var args = new ArrayList<String>();

        // -analysisPlatform
        if (analysisPlatform_ != null) {
            args.add("-analysisPlatform");
            args.add(analysisPlatform_.name().toLowerCase());
        }

        // -apiVersion
        if (apiVersion_ != null) {
            args.add("-apiVersion");
            args.add(apiVersion_);
        }

        // -classpath
        if (!classpath_.isEmpty()) {
            args.add("-classpath");
            args.add(String.join(SEMICOLON, classpath_));
        }

        // -dependentSourceSets
        if (!dependentSourceSets_.isEmpty()) {
            args.add("-dependentSourceSets");
            var deps = new ArrayList<String>();
            dependentSourceSets_.forEach((k, v) -> deps.add(String.format("%s/%s", k, v)));
            args.add(String.join(SEMICOLON, deps));
        }

        // -displayName
        if (displayName_ != null) {
            args.add("-displayName");
            args.add(displayName_);
        }

        // -documentedVisibilities
        if (!documentedVisibilities_.isEmpty()) {
            args.add("-documentedVisibilities");
            var vis = new ArrayList<String>();
            documentedVisibilities_.forEach(d -> vis.add(d.name().toLowerCase()));
            args.add(String.join(SEMICOLON, vis));
        }

        // -externalDocumentationLinks
        if (!externalDocumentationLinks_.isEmpty()) {
            args.add("-externalDocumentationLinks");
            var links = new ArrayList<String>();
            externalDocumentationLinks_.forEach((k, v) -> links.add(String.format("%s^%s", k, v)));
            args.add(String.join("^^", links));
        }

        // -jdkVersion
        if (jdkVersion_ > 0) {
            args.add("-jdkVersion");
            args.add(String.valueOf(jdkVersion_));
        }

        // -includes
        if (!includes_.isEmpty()) {
            args.add("-includes");
            args.add(String.join(SEMICOLON, includes_));
        }

        // -languageVersion
        if (languageVersion_ != null) {
            args.add("-languageVersion");
            args.add(languageVersion_);
        }

        // -noJdkLink
        if (noJdkLink_) {
            args.add("-noJdkLink");
            args.add(String.valueOf(noJdkLink_));
        }

        // -noSkipEmptyPackages
        if (noSkipEmptyPackages_) {
            args.add("-noSkipEmptyPackages");
            args.add(String.valueOf(noSkipEmptyPackages_));
        }

        // -noStdlibLink
        if (noStdlibLink_) {
            args.add("-noStdlibLink");
            args.add(String.valueOf(noStdlibLink_));
        }

        // -reportUndocumented
        if (reportUndocumented_) {
            args.add("-reportUndocumented");
            args.add(String.valueOf(reportUndocumented_));
        }

        // -perPackageOptions
        if (!perPackageOptions_.isEmpty()) {
            args.add("-perPackageOptions");
            args.add(String.join(SEMICOLON, perPackageOptions_));
        }

        // -samples
        if (!samples_.isEmpty()) {
            args.add("-samples");
            args.add(String.join(SEMICOLON, samples_));
        }

        // -skipDeprecated
        if (skipDeprecated_) {
            args.add("-skipDeprecated");
            args.add(String.valueOf(skipDeprecated_));
        }

        // -src
        if (!src_.isEmpty()) {
            args.add("-src");
            args.add(String.join(SEMICOLON, src_));
        }

        // -srcLink
        if (!srcLinks_.isEmpty()) {
            args.add("-srcLink");
            var links = new ArrayList<String>();
            srcLinks_.forEach((k, v) -> links.add(String.format("%s=%s", k, v)));
            args.add(String.join(SEMICOLON, links));
        }

        // -sourceSetName
        if (sourceSetName_ != null) {
            args.add("-sourceSetName");
            args.add(sourceSetName_);
        }

        // -suppressedFiles
        if (!suppressedFiles_.isEmpty()) {
            args.add("-suppressedFiles");
            args.add(String.join(SEMICOLON, suppressedFiles_));
        }

        return args;
    }

    /**
     * Sets classpath for analysis and interactive samples.
     * <p>
     * This is useful if some types that come from dependencies are not resolved/picked up automatically.
     * <p>
     * This option accepts both {@code .jar} and {@code .klib} files.
     *
     * @param classpath one or more classpath
     * @return this operation instance
     */
    public SourceSet classpath(String... classpath) {
        classpath_.addAll(Arrays.asList(classpath));
        return this;
    }

    /**
     * Sets classpath for analysis and interactive samples.
     * <p>
     * This is useful if some types that come from dependencies are not resolved/picked up automatically.
     * <p>
     * This option accepts both {@code .jar} and {@code .klib} files.
     *
     * @param classpath the list of classpath
     * @return this operation instance
     */
    public SourceSet classpath(Collection<String> classpath) {
        classpath_.addAll(classpath);
        return this;
    }

    /**
     * Sets the names of dependent source sets.
     *
     * @param moduleName    the module name
     * @param sourceSetName the source set name
     * @return this operation instance
     */
    public SourceSet dependentSourceSets(String moduleName, String sourceSetName) {
        dependentSourceSets_.put(moduleName, sourceSetName);
        return this;
    }

    /**
     * Sets the names of dependent source sets.
     *
     * @param dependentSourceSets the map of dependent source set names
     * @return this operation instance
     * @see #dependentSourceSets(String, String)
     */
    public SourceSet dependentSourceSets(Map<String, String> dependentSourceSets) {
        dependentSourceSets_.putAll(dependentSourceSets);
        return this;
    }

    /**
     * Sets the display name of the source set, used both internally and externally.
     * <p>
     * The name is used both externally (for example, the source set name is visible to documentation readers) and
     * internally (for example, for logging messages of {@link #reportUndocumented}).
     * <p>
     * The platform name can be used if you don't have a better alternative.
     *
     * @param displayName the display name
     * @return this operation instance
     */
    public SourceSet displayName(String displayName) {
        displayName_ = displayName;
        return this;
    }

    /**
     * Sets visibilities to be documented.
     * <p>
     * This can be used if you want to document protected/internal/private declarations, as well as if you want to
     * exclude public declarations and only document internal API.
     * <p>
     * This can be configured on per-package basis.
     *
     * @param visibilities one or more visibilities
     * @return this operation instance
     */
    public SourceSet documentedVisibilities(DocumentedVisibility... visibilities) {
        documentedVisibilities_.addAll(Arrays.asList(visibilities));
        return this;
    }

    /**
     * Sets the external documentation links.
     * <p>
     * A set of parameters for external documentation links that is applied only for this source set.
     *
     * @param url            the external documentation URL
     * @param packageListUrl the external documentation package list URL
     * @return this operation instance
     */
    public SourceSet externalDocumentationLinks(String url, String packageListUrl) {
        externalDocumentationLinks_.put(url, packageListUrl);
        return this;
    }

    /**
     * Sets the external documentation links.
     * <p>
     * A set of parameters for external documentation links that is applied only for this source set.
     *
     * @param externalDocumentationLinks the map of external documentation links
     * @return this operation instance
     * @see #externalDocumentationLinks(String, String)
     */
    public SourceSet externalDocumentationLinks(Map<String, String> externalDocumentationLinks) {
        externalDocumentationLinks_.putAll(externalDocumentationLinks);
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * A list of Markdown files that contain module and package documentation.
     * <p>
     * The contents of the specified files are parsed and embedded into documentation as module and package
     * descriptions.
     *
     * @param files one or more files
     * @return this operation instance
     */
    public SourceSet includes(String... files) {
        includes_.addAll(Arrays.asList(files));
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * A list of Markdown files that contain module and package documentation.
     * <p>
     * The contents of the specified files are parsed and embedded into documentation as module and package
     * descriptions.
     *
     * @param files the list of files
     * @return this operation instance
     */
    public SourceSet includes(Collection<String> files) {
        includes_.addAll(files);
        return this;
    }

    /**
     * Sets the version of JDK to use for linking to JDK Javadocs.
     * <p>
     * The JDK version to use when generating external documentation links for Java types.
     * <p>
     * For example, if you use {@link java.util.UUID} in some public declaration signature, and this option is set to 8,
     * Dokka generates an external documentation link to JDK 8 Javadocs for it.
     *
     * @param jdkVersion the JDK version
     * @return this operation instance
     */
    public SourceSet jdkVersion(int jdkVersion) {
        jdkVersion_ = jdkVersion;
        return this;
    }

    /**
     * Sets the language version used for setting up analysis and samples.
     *
     * @param languageVersion the language version
     * @return this operation instance
     */
    public SourceSet languageVersion(String languageVersion) {
        languageVersion_ = languageVersion;
        return this;
    }

    /**
     * Sets whether to generate links to JDK Javadocs.
     * <p>
     * Whether to generate external documentation links to JDK's Javadocs.
     * <p>
     * The version of JDK Javadocs is determined by the {@link #jdkVersion} option.
     * <p>
     * Note: Links are generated when noJdkLink is set to false.
     *
     * @param noJdkLink {@code true} or {@code false}
     * @return this operation instance
     */
    public SourceSet noJdkLink(Boolean noJdkLink) {
        noJdkLink_ = noJdkLink;
        return this;
    }

    /**
     * Sets whether to create pages for empty packages.
     * <p>
     * Whether to skip packages that contain no visible declarations after various filters have been applied.
     *
     * @param noSkipEmptyPackages {@code true} or {@code false}
     * @return this operation instance
     */
    public SourceSet noSkipEmptyPackages(boolean noSkipEmptyPackages) {
        noSkipEmptyPackages_ = noSkipEmptyPackages;
        return this;
    }

    /**
     * Sets whether to generate links to Standard library.
     * <p>
     * Whether to generate external documentation links that lead to the API reference documentation of Kotlin's
     * standard library.
     * <p>
     * Note: Links are generated when noStdLibLink is set to {@code false}.
     *
     * @param noStdlibLink {@code true} or {@code false}
     * @return this operation instance
     */
    public SourceSet noStdlibLink(Boolean noStdlibLink) {
        noStdlibLink_ = noStdlibLink;
        return this;
    }

    /**
     * Set the list of package source set configuration.
     * <p>
     * A set of parameters specific to matched packages within this source set.
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
     * @param perPackageOptions the list of per package options
     * @return this operation instance
     */
    public SourceSet perPackageOptions(Collection<String> perPackageOptions) {
        perPackageOptions_.addAll(perPackageOptions);
        return this;
    }

    /**
     * Set the list of package source set configuration.
     * <p>
     * A set of parameters specific to matched packages within this source set.
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
     * @param perPackageOptions the list of per package options
     * @return this operation instance
     */
    public SourceSet perPackageOptions(String... perPackageOptions) {
        perPackageOptions_.addAll(List.of(perPackageOptions));
        return this;
    }

    /**
     * Sets whether to report undocumented declarations.
     * <p>
     * Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs after they
     * have been filtered by documentedVisibilities and other filters.
     * <p>
     * This setting works well with {@link DokkaOperation#failOnWarning}.
     * <p>
     * This can be configured on per-package basis.
     *
     * @param reportUndocumented {@code true} or {@code false}
     * @return this operation instance
     */
    public SourceSet reportUndocumented(Boolean reportUndocumented) {
        reportUndocumented_ = reportUndocumented;
        return this;
    }

    /**
     * Set the list of directories or files that contain sample functions.
     * <p>
     * A list of directories or files that contain sample functions which are referenced via the {@code @sample} KDoc
     * tag.
     *
     * @param samples the list of samples
     * @return this operation instance
     */
    public SourceSet samples(Collection<String> samples) {
        samples_.addAll(samples);
        return this;
    }

    /**
     * Set the list of directories or files that contain sample functions.
     * <p>
     * A list of directories or files that contain sample functions which are referenced via the {@code @sample} KDoc
     * tag.
     *
     * @param samples nne or more samples
     * @return this operation instance
     */
    public SourceSet samples(String... samples) {
        samples_.addAll(List.of(samples));
        return this;
    }

    /**
     * Sets whether to skip deprecated declarations.
     * <p>
     * Whether to document declarations annotated with {@code @Deprecated}.
     * <p>
     * This can be configured on per-package basis.
     *
     * @param skipDeprecated {@code true} or {@code false}
     * @return this operation instance
     */
    public SourceSet skipDeprecated(boolean skipDeprecated) {
        skipDeprecated_ = skipDeprecated;
        return this;
    }

    /**
     * Sets the name of the source set. Default is {@code main}.
     *
     * @param sourceSetName the source set name.
     * @return this operation instance
     */
    public SourceSet sourceSetName(String sourceSetName) {
        sourceSetName_ = sourceSetName;
        return this;
    }

    /**
     * Sets the source code roots to be analyzed and documented.
     * <p>
     * The source code roots to be analyzed and documented. Acceptable inputs are directories and individual
     * {@code .kt} / {@code .java} files.
     *
     * @param src the list of source code roots
     * @return this operation instance
     */
    public SourceSet src(Collection<String> src) {
        src_.addAll(src);
        return this;
    }

    /**
     * Sets the source code roots to be analyzed and documented.
     * <p>
     * The source code roots to be analyzed and documented. Acceptable inputs are directories and individual
     * {@code .kt} / {@code .java} files.
     *
     * @param src pne ore moe source code roots
     * @return this operation instance
     */
    public SourceSet src(String... src) {
        src_.addAll(List.of(src));
        return this;
    }

    /**
     * Sets the mapping between a source directory and a Web service for browsing the code.
     *
     * @param srcPath    the source path
     * @param remotePath the remote path
     * @param lineSuffix the line suffix
     * @return this operation instance
     */
    public SourceSet srcLink(String srcPath, String remotePath, String lineSuffix) {
        srcLinks_.put(srcPath, remotePath + lineSuffix);
        return this;
    }

    /**
     * Sets the paths to files to be suppressed.
     * <p>
     * The files to be suppressed when generating documentation.
     *
     * @param suppressedFiles the list of suppressed files
     * @return this operation instance
     */
    public SourceSet suppressedFiles(Collection<String> suppressedFiles) {
        suppressedFiles_.addAll(suppressedFiles);
        return this;
    }

    /**
     * Sets the paths to files to be suppressed.
     * <p>
     * The files to be suppressed when generating documentation.
     *
     * @param suppressedFiles one or moe suppressed files
     * @return this operation instance
     */
    public SourceSet suppressedFiles(String... suppressedFiles) {
        suppressedFiles_.addAll(Arrays.asList(suppressedFiles));
        return this;
    }
}
