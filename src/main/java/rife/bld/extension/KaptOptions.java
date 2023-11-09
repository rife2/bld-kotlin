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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The (@{code kapt}) compiler plugin options,
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class KaptOptions {
    private final static String PLUGIN_ID = "plugin:org.jetbrains.kotlin.kapt3:";
    private final Logger LOGGER = Logger.getLogger(KaptOptions.class.getName());
    private final List<String> apClasspath_ = new ArrayList<>();
    private final Collection<Map<String, String>> apOptions_ = new ArrayList<>();
    private final AptMode aptMode_;
    private final Collection<String> classes_;
    private final Collection<Map<String, String>> javacArguments_ = new ArrayList<>();
    private final List<String> processors_ = new ArrayList<>();
    private final Collection<String> sources_;
    private final File stubs_;
    private boolean correctErrorTypes_;
    private File dumpFileReadHistory_;
    private File incrementalData_;
    private boolean verbose_;

    /**
     * Creates a new instance.
     * <p>
     * The application processor mode can be one of the following:
     * <ul>
     * <li>{@link AptMode#STUBS stubs} - only generate stubs needed for annotation processing</li>
     * <li>{@link AptMode#APT apt} - only run annotation processing</li>
     * <li>{@link AptMode#STUBS_AND_APT stubsAndApt} - generate stubs and run annotation processing.</li>
     * </ul>
     *
     * @param sources an output path for the generated files
     * @param classes an output path for the generated class files and resources
     * @param stubs   an output path for the stub files. In other words, some temporary directory
     * @param mode    the mode
     */
    KaptOptions(Collection<String> sources, Collection<String> classes, File stubs, AptMode mode) {
        sources_ = sources;
        classes_ = classes;
        stubs_ = stubs;
        aptMode_ = mode;
    }

    /**
     * Creates a new instance.
     * <p>
     * The application processor mode can be one of the following:
     * <ul>
     * <li>{@link AptMode#STUBS stubs} - only generate stubs needed for annotation processing</li>
     * <li>{@link AptMode#APT apt} - only run annotation processing</li>
     * <li>{@link AptMode#STUBS_AND_APT stubsAndApt} - generate stubs and run annotation processing.</li>
     * </ul>
     *
     * @param sources an output path for the generated files
     * @param classes an output path for the generated class files and resources
     * @param stubs   an output path for the stub files. In other words, some temporary directory
     * @param mode    the mode
     */
    KaptOptions(Collection<String> sources, Collection<String> classes, String stubs, AptMode mode) {
        this(sources, classes, new File(stubs), mode);
    }

    /**
     * A path to the annotation processor JAR. Pass as many classpath as the number of JARs that you have.
     *
     * @param apClasspath the list of classpath
     * @return this class instance
     */
    public KaptOptions apClasspath(Collection<String> apClasspath) {
        apClasspath_.addAll(apClasspath);
        return this;
    }

    /**
     * A path to the annotation processor JAR. Pass as many classpath as the number of JARs that you have.
     *
     * @param apClasspath one or more classpath
     * @return this class instance
     */
    public KaptOptions apClasspath(String... apClasspath) {
        apClasspath_.addAll(List.of(apClasspath));
        return this;
    }

    /**
     * A list of the annotation processor options.
     *
     * @param apOptions the list of options
     * @return this class instance
     */
    public KaptOptions apOptions(Map<String, String> apOptions) {
        apOptions_.add(apOptions);
        return this;
    }

    /**
     * Returns the formatted arguments.
     *
     * @return the arguments
     */
    public List<String> args() {
        var args = new ArrayList<String>();

        // sources
        if (!sources_.isEmpty()) {
            sources_.forEach(s -> args.add(PLUGIN_ID + "sources=" + s));
        }

        // classes
        if (!classes_.isEmpty()) {
            classes_.forEach(c -> args.add(PLUGIN_ID + "classes=" + c));
        }

        // stubs
        args.add(PLUGIN_ID + "stubs=" + stubs_.getAbsolutePath());

        // apMode
        args.add(PLUGIN_ID + "aptMode=" + aptMode_.name().toLowerCase());

        // apclasspath
        if (!apClasspath_.isEmpty()) {
            apClasspath_.forEach(c -> args.add(PLUGIN_ID + "apclasspath=" + c));
        }

        // apoptions
        if (!apOptions_.isEmpty()) {
            apOptions_.forEach(o -> {
                try {
                    args.add(PLUGIN_ID + "apoptions=" + encodeList(o));
                } catch (IOException e) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, "Could not encode application processor option: " + o, e);
                    }
                }
            });
        }

        // correctErrorTypes
        if (correctErrorTypes_) {
            args.add(PLUGIN_ID + "correctErrorTypes=true");
        }

        // dumpFileReadHistory
        if (dumpFileReadHistory_ != null) {
            args.add(PLUGIN_ID + "dumpFileReadHistory=" + dumpFileReadHistory_.getAbsolutePath());
        }

        // incrementalData
        if (incrementalData_ != null) {
            args.add(PLUGIN_ID + "incrementalData=" + incrementalData_.getAbsolutePath());
        }

        // javacArguments
        if (!javacArguments_.isEmpty()) {
            javacArguments_.forEach(a -> {
                try {
                    args.add(PLUGIN_ID + "javacArguments=" + encodeList(a));
                } catch (IOException e) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, "Could not encode javac argument: " + a, e);
                    }
                }
            });
        }

        // processors
        if (!processors_.isEmpty()) {
            args.add(PLUGIN_ID + "processors=" + String.join(",", processors_));
        }

        // verbose
        if (verbose_) {
            args.add(PLUGIN_ID + "verbose=true");
        }

        return args;
    }

    /**
     * To enable error type inferring in stubs.
     * <p>
     * Some annotation processors (such as {@code AutoFactory}) rely on precise types in declaration signatures. By
     * default, {@code kapt} replaces every unknown type (including types for the generated classes) to
     * {@code NonExistentClass}, but you use this option to change this behavior.
     *
     * @param correctErrorTypes {@code true} or {@code false}
     * @return this class instance
     */
    public KaptOptions correctErrorTypes(boolean correctErrorTypes) {
        correctErrorTypes_ = correctErrorTypes;
        return this;
    }

    /**
     * An output path to dump for each file a list of classes used during annotation processing.
     *
     * @param dupFileReadHistory the output path
     * @return this class instance
     */
    public KaptOptions dupFileReadHistory(File dupFileReadHistory) {
        dumpFileReadHistory_ = dupFileReadHistory;
        return this;
    }

    /**
     * An output path to dump for each file a list of classes used during annotation processing.
     *
     * @param dupFileReadHistory the output path
     * @return this class instance
     */
    public KaptOptions dupFileReadHistory(String dupFileReadHistory) {
        dumpFileReadHistory_ = new File(dupFileReadHistory);
        return this;
    }

    // Base-64 encodes the options list
    private String encodeList(Map<String, String> options) throws IOException {
        var os = new ByteArrayOutputStream();
        var oos = new ObjectOutputStream(os);

        oos.writeInt(options.size());
        for (var entry : options.entrySet()) {
            oos.writeUTF(entry.getKey());
            oos.writeUTF(entry.getValue());
        }

        oos.flush();
        return Base64.getEncoder().encodeToString(os.toByteArray());
    }

    /**
     * An output path for the binary stubs.
     *
     * @param incrementalData the output path
     * @return this class instance
     */
    public KaptOptions incrementalData(File incrementalData) {
        incrementalData_ = incrementalData;
        return this;
    }

    /**
     * An output path for the binary stubs.
     *
     * @param incrementalData the output path
     * @return this class instance
     */
    public KaptOptions incrementalData(String incrementalData) {
        incrementalData_ = new File(incrementalData);
        return this;
    }

    /**
     * A list of the options passed to {@code javac}.
     *
     * @param javacArguments the {@code javac} arguments
     * @return this class instance
     */
    public KaptOptions javacArguments(Map<String, String> javacArguments) {
        javacArguments_.add(javacArguments);
        return this;
    }

    /**
     * A list of annotation processor qualified class names. If specified, {@code kapt} does not try to find annotation
     * processors in {@link #apClasspath}.
     *
     * @param processors the list of qualified class names
     * @return this class instance
     */
    public KaptOptions processors(Collection<String> processors) {
        processors_.addAll(processors);
        return this;
    }

    /**
     * A list of annotation processor qualified class names. If specified, {@code kapt} does not try to find annotation
     * processors in {@link #apClasspath}.
     *
     * @param processors one or moe qualified class names
     * @return this class instance
     */
    public KaptOptions processors(String... processors) {
        processors_.addAll(List.of(processors));
        return this;
    }

    /**
     * Enables verbose output.
     *
     * @param verbose {@code true} or {@code false}
     * @return this class instance
     */
    public KaptOptions verbose(boolean verbose) {
        verbose_ = verbose;
        return this;
    }
}
