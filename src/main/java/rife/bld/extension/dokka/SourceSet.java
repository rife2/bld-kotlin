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

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for a Dokka source set.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class SourceSet {
    private final Collection<File> classpath_ = new ArrayList<>();
    private final Map<String, String> dependentSourceSets_ = new ConcurrentHashMap<>();
    private final Collection<DocumentedVisibility> documentedVisibilities_ = new ArrayList<>();
    private final Map<String, String> externalDocumentationLinks_ = new ConcurrentHashMap<>();
    private final Collection<File> includes_ = new ArrayList<>();
    private final Collection<String> perPackageOptions_ = new ArrayList<>();
    private final Collection<File> samples_ = new ArrayList<>();
    private final Map<String, String> srcLinks_ = new ConcurrentHashMap<>();
    private final Collection<File> src_ = new ArrayList<>();
    private final Collection<File> suppressedFiles_ = new ArrayList<>();
    private AnalysisPlatform analysisPlatform_;
    private String apiVersion_;
    private String displayName_;
    private int jdkVersion_;
    private String languageVersion_;
    private Boolean noJdkLink_;
    private boolean noSkipEmptyPackages_;
    private Boolean noStdlibLink_;
    private Boolean reportUndocumented_;
    private boolean skipDeprecated_;
    private String sourceSetName_;

    /**
     * Sets the platform used for setting up analysis. Default is {@link AnalysisPlatform#JVM}
     *
     * @param analysisPlatform the analysis platfrom
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

        // -src
        if (!src_.isEmpty()) {
            args.add("-src");
            src_.forEach(s -> args.add(s.getAbsolutePath() + ';'));
        }

        return args;
    }

    /**
     * Sets classpath for analysis and interactive samples.
     *
     * @param classpath one or more classpath
     * @return this operation instance
     */
    public SourceSet classpath(File... classpath) {
        classpath_.addAll(Arrays.asList(classpath));
        return this;
    }

    /**
     * Sets classpath for analysis and interactive samples.
     *
     * @param classpath the list of classpath
     * @return this operation instance
     */
    public SourceSet classpath(Collection<File> classpath) {
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
     *
     * @param externalDocumentationLinks the map of external documentation links
     * @return this operation instance
     * @see #dependentSourceSets(String, String)
     */
    public SourceSet externalDocumentationLinks(Map<String, String> externalDocumentationLinks) {
        externalDocumentationLinks_.putAll(externalDocumentationLinks);
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     *
     * @param files one or more files
     * @return this operation instance
     */
    public SourceSet includes(File... files) {
        includes_.addAll(Arrays.asList(files));
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     *
     * @param files the list of files
     * @return this operation instance
     */
    public SourceSet includss(Collection<File> files) {
        includes_.addAll(files);
        return this;
    }

    /**
     * Sets the version of JDK to use for linking to JDK Javadocs.
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
     *
     * @param noJdkLink the no JDK link toggle
     * @return this operation instance
     */
    public SourceSet noJdkLink(Boolean noJdkLink) {
        noJdkLink_ = noJdkLink;
        return this;
    }

    /**
     * Sets whether to create pages for empty packages.
     *
     * @param noSkipEmptyPackages the no skip empty packages toggle
     * @return this operation instance
     */
    public SourceSet noSkipEmptyPackages(boolean noSkipEmptyPackages) {
        noSkipEmptyPackages_ = noSkipEmptyPackages;
        return this;
    }

    /**
     * Sets whether to generate links to Standard library.
     *
     * @param noStdlibLink the no std lib link toggle
     * @return this operation instance
     */
    public SourceSet noStdlibLink(Boolean noStdlibLink) {
        noStdlibLink_ = noStdlibLink;
        return this;
    }

    /**
     * Set the list of package source set configuration in format:
     * <ul>
     * <li><matchingRegexp</li>
     * <li>-deprecated</li>
     * <li>-privateApi</li>
     * <li>+warnUndocumented</li>
     * <li>+suppress</li>
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
     * Set the list of package source set configuration in format:
     * <ul>
     * <li><matchingRegexp</li>
     * <li>-deprecated</li>
     * <li>-privateApi</li>
     * <li>+warnUndocumented</li>
     * <li>+suppress</li>
     * <li>...</li>
     * </ul>
     *
     * @param perPackageOptions one or more per package options
     * @return this operation instance
     */
    public SourceSet perPackageOptions(String... perPackageOptions) {
        perPackageOptions_.addAll(List.of(perPackageOptions));
        return this;
    }

    /**
     * Sets Wwether to report undocumented declarations.
     *
     * @param reportUndocumented the report undocumented toggle
     * @return this operation instance
     */
    public SourceSet reportUndocumented(Boolean reportUndocumented) {
        reportUndocumented_ = reportUndocumented;
        return this;
    }

    /**
     * Set the list of directories or files that contain sample functions.
     *
     * @param samples the list of samples
     * @return this operation instance
     */
    public SourceSet samples(Collection<File> samples) {
        samples_.addAll(samples);
        return this;
    }

    /**
     * Set the list of directories or files that contain sample functions.
     *
     * @param samples nne or more samples
     * @return this operation instance
     */
    public SourceSet samples(File... samples) {
        samples_.addAll(List.of(samples));
        return this;
    }

    /**
     * Sets whether to skip deprecated declarations.
     *
     * @param skipDeprecated the skip deprecated toggle
     * @return this operation instance
     */
    public SourceSet skipDeprecated(boolean skipDeprecated) {
        skipDeprecated_ = skipDeprecated;
        return this;
    }

    /**
     * Sets the name of the source set. Default is  {@code main}.
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
     *
     * @param src the list of source code roots
     * @return this operation instance
     */
    public SourceSet src(Collection<File> src) {
        src_.addAll(src);
        return this;
    }

    /**
     * Sets the source code roots to be analyzed and documented.
     *
     * @param src pne ore moe source code roots
     * @return this operation instance
     */
    public SourceSet src(File... src) {
        src_.addAll(List.of(src));
        return this;
    }

    /**
     * Sets the mpping between a source directory and a Web service for browsing the code.
     *
     * @param srcPath    the source path
     * @param remotePath the remote path
     * @param lineSuffix the line suffix
     * @return this operation instance
     */
    public SourceSet srcLinks(String srcPath, String remotePath, String lineSuffix) {
        srcLinks_.put(srcPath, remotePath + '#' + lineSuffix);
        return this;
    }

    /**
     * Sets the paths to files to be suppressed.
     *
     * @param suppressedFiles the list of suppressed files
     * @return this operation instance
     */
    public SourceSet suppressedFiles(Collection<File> suppressedFiles) {
        suppressedFiles_.addAll(suppressedFiles);
        return this;
    }

    /**
     * Sets the paths to files to be suppressed.
     *
     * @param suppressedFiles one or moe suppressed files
     * @return this operation instance
     */
    public SourceSet suppressedFiles(File... suppressedFiles) {
        suppressedFiles_.addAll(Arrays.asList(suppressedFiles));
        return this;
    }
}