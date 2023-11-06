# [Kotlin](https://kotlinlang.org/) Extension for [b<span style="color:orange">l</span>d](https://rife2.com/bld) 

[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.20-7f52ff.svg)](https://kotlinlang.org)
[![bld](https://img.shields.io/badge/1.7.3-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/releases/com/uwyn/rife2/bld-kotlin/maven-metadata.xml?color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-spring-boot)
[![Snapshot](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/snapshots/com/uwyn/rife2/bld-kotlin/maven-metadata.xml?label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-spring-boot)
[![GitHub CI](https://github.com/rife2/bld-kotlin/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-spring-boot/actions/workflows/bld.yml)

To install, please refer to the [extensions documentation](https://github.com/rife2/bld/wiki/Extensions).

## Compile Kotlin Source Code

To compile the source code located in `src/main/kotlin` and `src/test/kotlin` from the current project:

```java
@BuildCommand(summary = "Compile the Kotlin project")
public void compile() throws IOException {
    new CompileKotlinOperation()
            .fromProject(this)
            .compileOptions("-verbose")
            .execute();
}
```

```text
./bld compile
```

- [View Examples Project](https://github.com/rife2/bld-kotlin/tree/main/examples/)

Please check the [Compile Operation documentation](https://rife2.github.io/bld-kotlin/rife/bld/extension/CompileKotlinOperation.html#method-summary)
for all available configuration options.

## Generate Javadoc

To generate the Javadoc using [Dokka](https://github.com/Kotlin/dokka):

```java
@BuildCommand(summary = "Generates Javadoc for the project")
public void javadoc() throws ExitStatusException, IOException, InterruptedException {
    new DokkaOperation()
            .fromProject(this)
            .execute();
}
```

```text
./bld javadoc
```

- [View Examples Project](https://github.com/rife2/bld-kotlin/tree/main/examples/)

Please check the [Dokka Operation documentation](https://rife2.github.io/bld-kotlin/rife/bld/extension/dokka/DokkaOperation.html#method-summary)
for all available configuration options.
