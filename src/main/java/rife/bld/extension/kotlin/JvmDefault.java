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

/**
 * JVM default methods options for interface declarations with bodies.
 *
 * @since 1.1.0
 */
public enum JvmDefault {
    /**
     * Generate default methods for non-abstract interface declarations, as well as {@code DefaultImpls} classes with
     * static methods for compatibility with code compiled in the {@code disable} mode.
     * <p>
     * This is the default behavior since language version 2.2.
     */
    ENABLE("enable"),
    /**
     * Generate default methods for non-abstract interface declarations. Do not generate {@code DefaultImpls} classes.
     */
    NO_COMPATIBILITY("no-compatibility"),
    /**
     * Do not generate JVM default methods.
     * <p>
     * This is the default behavior up to language version 2.1.
     */
    DISABLE("disable");

    public final String value;

    JvmDefault(String value) {
        this.value = value;
    }
}