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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import rife.bld.extension.tools.ObjectTools;
import rife.tools.StringUtils;

import java.util.*;

/**
 * Java Virtual Machine options.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.1.0
 */
public class JvmOptions {

    /**
     * Keyword to enable native access for all code on the class path.
     */
    public static final String ALL_UNNAMED = "ALL-UNNAMED";

    private final Set<String> nativeAccessModules_ = new LinkedHashSet<>();
    private NativeAccess illegalAccessMode_;

    /**
     * Returns the formatted arguments.
     *
     * @return the arguments
     */
    public List<String> args() {
        var args = new ArrayList<String>();

        if (!nativeAccessModules_.isEmpty()) {
            args.add("--enable-native-access=" + StringUtils.join(nativeAccessModules_, ","));
        }

        if (illegalAccessMode_ != null) {
            args.add("--illegal-native-access=" + illegalAccessMode_.getMode());
        }

        return args;
    }

    /**
     * Returns the action the Java runtime takes when native access is not enabled for a module.
     *
     * @return the access mode or {@code null} if unspecified
     * @since 1.2
     */
    public NativeAccess illegalNativeAccess() {
        return illegalAccessMode_;
    }

    /**
     * Controls what action the Java runtime takes when native access is not enabled for a module.
     * <p>
     * Note: This flag was introduced in JDK 17 and removed in JDK 23.
     *
     * @param access the access mode
     * @return this list of options
     * @throws NullPointerException if access is {@code null}
     * @deprecated Removed in JDK 23
     */
    @NonNull
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "23")
    public JvmOptions illegalNativeAccess(@NonNull NativeAccess access) {
        ObjectTools.requireNonNull(access, "illegalNativeAccess");
        illegalAccessMode_ = access;
        return this;
    }

    /**
     * Modules that are permitted to perform restricted native operations.
     * <p>
     * The module name can also be {@link #ALL_UNNAMED}.
     *
     * @param modules the module names
     * @return this list of options
     * @throws NullPointerException     if {@code modules} is {@code null}
     * @throws IllegalArgumentException if {@code modules} is empty, or contains {@code null} or empty elements
     * @since 1.2
     */
    @NonNull
    public JvmOptions nativeAccessModules(@NonNull Collection<String> modules) {
        ObjectTools.requireNotEmpty(modules, "nativeAccessModules");
        nativeAccessModules_.addAll(modules);
        return this;
    }

    /**
     * Modules that are permitted to perform restricted native operations.
     * <p>
     * The module name can also be {@link #ALL_UNNAMED}.
     *
     * @param modules the module names
     * @return this list of options
     * @throws NullPointerException     if {@code modules} is {@code null}
     * @throws IllegalArgumentException if {@code modules} is empty, or contains {@code null} or empty elements
     * @since 1.2
     */
    @NonNull
    public JvmOptions nativeAccessModules(@NonNull String... modules) {
        ObjectTools.requireNotEmpty(modules, "nativeAccessModules");
        nativeAccessModules_.addAll(List.of(modules));
        return this;
    }

    /**
     * Returns the modules that are permitted to perform restricted native operations.
     *
     * @return the modules list
     * @since 1.2
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Returns the live list. Mutate at your own risk.")
    public Set<String> nativeAccessModules() {
        return nativeAccessModules_;
    }

    /**
     * Illegal native access modes.
     */
    public enum NativeAccess {
        /**
         * Represents the {@code allow} mode for enabling specific native access.
         * <p>
         * This mode permits the operation or access that would otherwise be restricted
         * or managed by native access policies.
         */
        ALLOW,
        /**
         * Represents the {@code deny} mode which signifies complete prevention of specific
         * native access.
         * <p>
         * This mode ensures that the operation or access is entirely prohibited according to
         * the native access policies.
         */
        DENY,
        /**
         * Represents the {@code warn} mode, which logs a warning when specific native access
         * is attempted.
         * <p>
         * This mode allows the operation to proceed while notifying the user about the
         * potential risks or restrictions associated with native access policies.
         */
        WARN;

        /**
         * Return the native access mode.
         *
         * @return the native access mode
         */
        @NonNull
        public String getMode() {
            return name().toLowerCase();
        }
    }
}