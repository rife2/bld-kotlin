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
import rife.bld.extension.tools.ObjectTools;
import rife.tools.StringUtils;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Java Virtual Machine options.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.1.0
 */
@SuppressWarnings("PMD.LooseCoupling")
public class JvmOptions extends ArrayList<String> {

    /**
     * Keyword to enable native access for all code on the class path.
     */
    public static final String ALL_UNNAMED = "ALL-UNNAMED";

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Modules that are permitted to perform restricted native operations.
     * The module name can also be {@link #ALL_UNNAMED}.
     * <p>
     * This replaces any previously set JVM options in this list to match CLI behavior
     * where the last occurrence of the flag takes effect.
     *
     * @param modules the module names
     *
     * @return this list of options
     * @throws NullPointerException     if modules is null or contains null
     * @throws IllegalArgumentException if modules is empty or contains empty strings
     */
    @NonNull
    public JvmOptions enableNativeAccess(@NonNull String... modules) {
        ObjectTools.requireAllNotEmpty(modules,
                "'enableNativeAccess' and its elements must not be null or empty");
        add("--enable-native-access=" + StringUtils.join(List.of(modules), ","));
        return this;
    }

    /**
     * Modules that are permitted to perform restricted native operations.
     * The module name can also be {@link #ALL_UNNAMED}.
     * <p>
     * This replaces any previously set JVM options in this list to match CLI behavior
     * where the last occurrence of the flag takes effect.
     *
     * @param modules the module names
     * @return this list of options
     * @throws NullPointerException     if modules is null or contains null
     * @throws IllegalArgumentException if modules is empty or contains empty strings
     */
    @NonNull
    public JvmOptions enableNativeAccess(@NonNull Collection<String> modules) {
        ObjectTools.requireAllNotEmpty(modules,
                "'enableNativeAccess' and its elements must not be null or empty");
        add("--enable-native-access=" + StringUtils.join(modules, ","));
        return this;
    }

    /**
     * Controls what action the Java runtime takes when native access is not enabled for a module.
     * <p>
     * This replaces any previously set JVM options in this list to match CLI behavior
     * where the last occurrence of the flag takes effect.
     * <p>
     * Note: This flag was introduced in JDK 17 and removed in JDK 23.
     *
     * @param access the access mode
     * @return this list of options
     * @throws NullPointerException if access is null
     * @deprecated Removed in JDK 23
     */
    @NonNull
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "23")
    public JvmOptions illegalNativeAccess(@NonNull NativeAccess access) {
        Objects.requireNonNull(access, "'illegalNativeAccess' must not be null");
        add("--illegal-native-access=" + access.getMode());
        return this;
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