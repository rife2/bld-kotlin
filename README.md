# [Kotlin](https://kotlinlang.org/) Extension for [b<span style="color:orange">l</span>d](https://rife2.com/bld)

[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21%2B-7f52ff.svg)](https://kotlinlang.org)
[![bld](https://img.shields.io/badge/3.0.1-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Freleases%2Fcom%2Fuwyn%2Frife2%2Fbld-kotlin%2Fmaven-metadata.xml&color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-kotlin)
[![Snapshot](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Fsnapshots%2Fcom%2Fuwyn%2Frife2%2Fbld-kotlin%2Fmaven-metadata.xml&label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-kotlin)
[![GitHub CI](https://github.com/rife2/bld-kotlin/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-kotlin/actions/workflows/bld.yml)

To install the latest version, add the following to the `lib/bld/bld-wrapper.properties` file:

```properties
bld.extension-kotlin=com.uwyn.rife2:bld-kotlin
```

For more information, please refer to the [extensions](https://github.com/rife2/bld/wiki/Extensions) and [support](https://github.com/rife2/bld/wiki/Kotlin-Support) documentation.

## Compile Kotlin Source Code

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

## Kotlin Compiler Resolution

The extension needs a Kotlin compiler to compile sources. It obtains one in one of two ways:

- **From the host**, if a Kotlin compiler is already installed.
- **By downloading one**, otherwise, or when the download is explicitly requested.

The full resolution order is:

1. an explicit `kotlinc(...)` or `kotlinHome(...)`,
2. a download when `version(...)` or `useDownloadedKotlin()` is set,
3. a host compiler discovered via `KOTLIN_HOME`, `PATH`, SDKMAN!, Homebrew, JetBrains Toolbox, or the
   common install locations,
4. a download of [`KOTLIN_VERSION`](https://rife2.github.io/bld-kotlin/rife/bld/extension/CompileKotlinOperation.html#KOTLIN_VERSION) as a last resort.


### Using an installed compiler

By default, the extension searches the host for a Kotlin compiler in the following locations:

- `KOTLIN_HOME`
- `PATH`
- [SDKMAN!](https://sdkman.io/)
- [Homebrew](https://brew.sh/)
- [JetBrains Toolbox](https://www.jetbrains.com/toolbox-app/) (IntelliJ IDEA, Android Studio)
- common install locations

If one is found, it is used. Please make sure it is
[installed](https://kotlinlang.org/docs/command-line.html#install-the-compiler) if you want to rely
on the host compiler.

You can also point at a specific installation explicitly. Either configure the Kotlin home directory:

```java

@BuildCommand(summary = "Compiles the Kotlin project")
public void compile() throws Exception {
    new CompileKotlinOperation()
            .fromProject(this)
            .kotlinHome("path/to/kotlin")
            .execute();
}
```

Or set the compiler executable directly:

```java

@BuildCommand(summary = "Compiles the Kotlin project")
public void compile() throws Exception {
    new CompileKotlinOperation()
            .fromProject(this)
            .kotlinc("/usr/bin/kotlinc")
            .execute();
}
```

### Downloading a compiler

The extension automatically downloads a Kotlin compiler from the official JetBrains release archive
and caches it under the project directory, either when no host compiler is found or when a specific
version is requested. This means the
extension works out of the box on a machine with no Kotlin installation.

The version that is downloaded defaults to
[`KOTLIN_VERSION`](https://rife2.github.io/bld-kotlin/rife/bld/extension/CompileKotlinOperation.html#KOTLIN_VERSION).

To pin a different version, set it explicitly:

```java

@BuildCommand(summary = "Compiles the Kotlin project")
public void compile() throws Exception {
    new CompileKotlinOperation()
            .fromProject(this)
            .version("2.4.20")
            .execute();
}
```

An explicit `version(...)` **also forces the download**, even when a Kotlin compiler is installed on
the host: it takes precedence over anything discovered via `KOTLIN_HOME`, `PATH`, or the common
install locations. This makes the build hermetic with respect to the host compiler — the same
version is used regardless of what is installed where the build runs.

The downloaded compiler is reused on subsequent builds, so a given version is only fetched once.

Downloading requires a project to be configured with `fromProject(...)`, since the compiler is placed
in the project directory.

### Forcing a download over the host compiler

There are two equivalent ways to skip the host search and use a bld-managed compiler:

- Pin a version with `version(...)`. The compiler is downloaded at that version, regardless of what
  the host has installed:

  ```java

  @BuildCommand(summary = "Compiles the Kotlin project")
  public void compile() throws Exception {
      new CompileKotlinOperation()
              .fromProject(this)
              .version("2.4.21")
              .execute();
  }
  ```

- Set `useDownloadedKotlin()`. This skips the `KOTLIN_HOME`, `PATH`, and common-install-location
  search and downloads [`KOTLIN_VERSION`](https://rife2.github.io/bld-kotlin/rife/bld/extension/CompileKotlinOperation.html#KOTLIN_VERSION) (or the configured `version(...)`, if one was set). It is
  the right choice when you want the default bld-managed compiler without pinning a specific
  version:

  ```java

  @BuildCommand(summary = "Compiles the Kotlin project")
  public void compile() throws Exception {
      new CompileKotlinOperation()
              .fromProject(this)
              .useDownloadedKotlin()
              .execute();
  }
  ```

Both can be combined; `useDownloadedKotlin()` then just makes the intent explicit. An explicit
`kotlinc(...)` or `kotlinHome(...)` still takes precedence over either, so an explicitly configured
compiler is never overridden by a download.

## Supported Kotlin Versions

While older versions of Kotlin are likely to work with the extension, only version 2.0.21 or higher is
officially supported.

## Template Project

There is also a [Template Project](https://github.com/rife2/kotlin-bld-example) with support for
the [Dokka](https://github.com/rife2/bld-dokka) and [Detekt](https://github.com/rife2/bld-detekt) extensions.

## API Reference

Please check
the [Compile Operation documentation](https://rife2.github.io/bld-kotlin/rife/bld/extension/CompileKotlinOperation.html#method-summary)
for all available configuration options.