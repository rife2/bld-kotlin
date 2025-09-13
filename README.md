# [Kotlin](https://kotlinlang.org/) Extension for [b<span style="color:orange">l</span>d](https://rife2.com/bld)

[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.25%2B-7f52ff.svg)](https://kotlinlang.org)
[![bld](https://img.shields.io/badge/2.3.0-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/releases/com/uwyn/rife2/bld-kotlin/maven-metadata.xml?color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-kotlin)
[![Snapshot](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/snapshots/com/uwyn/rife2/bld-kotlin/maven-metadata.xml?label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-kotlin)
[![GitHub CI](https://github.com/rife2/bld-kotlin/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-kotlin/actions/workflows/bld.yml)

To install the latest version, add the following to the `lib/bld/bld-wrapper.properties` file:

```properties
bld.extension-kotlin=com.uwyn.rife2:bld-kotlin
```

For more information, please refer to the [extensions](https://github.com/rife2/bld/wiki/Extensions) and [support](https://github.com/rife2/bld/wiki/Kotlin-Support) documentation.

## Compile Kotlin Source Code

To compile the source code located in `src/main/kotlin` and `src/test/kotlin` from the current project add the following to the build file:

```java

@BuildCommand(summary = "Compiles the Kotlin project")
public void compile() throws Exception {
    new CompileKotlinOperation()
            .fromProject(this)
            .execute();
}
```

```console
./bld compile
```

- [View Examples Project](https://github.com/rife2/bld-kotlin/tree/main/examples/)
- [View Template Project](https://github.com/rife2/kotlin-bld-example)

Please check
the [Compile Operation documentation](https://rife2.github.io/bld-kotlin/rife/bld/extension/CompileKotlinOperation.html#method-summary)
for all available configuration options.

## Kotlin Compiler Requirement

Please make sure the Kotlin compiler is [installed](https://kotlinlang.org/docs/command-line.html#install-the-compiler).

The extension will look in common locations such as:

 - `KOTLIN_HOME`
 - `PATH`
 - [SDKMAN!](https://sdkman.io/)
 - [Homebrew](https://brew.sh/)
 - [JetBrains Toolbox](https://www.jetbrains.com/toolbox-app/) (IntelliJ IDEA, Android Studio)
 - etc.

You can also manually configure the Kotlin home location as follows:

```java

@BuildCommand(summary = "Compiles the Kotlin project")
public void compile() throws Exception {
    new CompileKotlinOperation()
            .fromProject(this)
            .kotlinHome("path/to/kotlin")
            .execute();
}
```

The Kotlin compiler executable can also be specified directly:

```java

@BuildCommand(summary = "Compiles the Kotlin project")
public void compile() throws Exception {
    new CompileKotlinOperation()
            .fromProject(this)
            .kotlinc("/usr/bin/kotlinc")
            .execute();
}
```

While older version of Kotlin are likely working with the extension, only version 1.9.25 or higher are officially
supported.

## Template Project

There is also a [Template Project](https://github.com/rife2/kotlin-bld-example) with support for
the [Dokka](https://github.com/rife2/bld-dokka) and [Detekt](https://github.com/rife2/bld-detekt) extensions.
