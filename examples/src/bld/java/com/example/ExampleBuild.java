package com.example;

import rife.bld.BaseProject;
import rife.bld.BuildCommand;
import rife.bld.extension.CompileKotlinOperation;
import rife.bld.extension.dokka.DokkaOperation;
import rife.bld.extension.dokka.LoggingLevel;
import rife.bld.operations.exceptions.ExitStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.compile;

public class ExampleBuild extends BaseProject {
    public ExampleBuild() {
        pkg = "com.example";
        name = "Example";
        mainClass = "com.example.Example";
        version = version(0, 1, 0);

        javaRelease = 17;
        downloadSources = true;
        autoDownloadPurge = true;
        repositories = List.of(MAVEN_LOCAL, MAVEN_CENTRAL, RIFE2_RELEASES);

        scope(compile)
                .include(dependency("org.jetbrains.kotlin", "kotlin-stdlib", version(1, 9, 20)));

        testOperation().mainClass("com.example.ExampleTest");
    }

    public static void main(String[] args) {
        var level = Level.ALL;
        var logger = Logger.getLogger("rife.bld.extension");
        var consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);
        logger.addHandler(consoleHandler);
        logger.setLevel(level);
        logger.setUseParentHandlers(false);

        new ExampleBuild().start(args);
    }

    @BuildCommand(summary = "Compile the Kotlin project")
    public void compile() throws IOException {
        new CompileKotlinOperation()
                .fromProject(this)
                .compileOptions("-verbose")
                .execute();
    }

    @BuildCommand(value = "dokka-gfm", summary = "Generates documentation in GitHub flavored markdown format")
    public void dokkaGfm() throws ExitStatusException, IOException, InterruptedException {
        new DokkaOperation()
                .fromProject(this)
                .loggingLevel(LoggingLevel.INFO)
                .pluginsClasspath(true)
                .pluginsClasspath("lib/bld/dokka-base-1.9.10.jar",
                        "lib/bld/analysis-kotlin-descriptors-1.9.10.jar",
                        "lib/bld/gfm-plugin-1.9.10.jar",
                        "lib/bld/freemarker-2.3.31.jar")
                .outputDir(Path.of(buildDirectory().getAbsolutePath(), "dokka", "gfm").toFile())
                .execute();
    }

    @BuildCommand(value = "dokka-html", summary = "Generates documentation in HTML format")
    public void dokkaHtml() throws ExitStatusException, IOException, InterruptedException {
        new DokkaOperation()
                .fromProject(this)
                .loggingLevel(LoggingLevel.INFO)
                .pluginsClasspath(true)
                .pluginsClasspath("lib/bld/dokka-base-1.9.10.jar",
                        "lib/bld/analysis-kotlin-descriptors-1.9.10.jar",
                        "lib/bld/kotlinx-html-jvm-0.7.5.jar",
                        "lib/bld/freemarker-2.3.31.jar")
                .outputDir(Path.of(buildDirectory().getAbsolutePath(), "dokka", "html").toFile())
                .execute();
    }

    @BuildCommand(summary = "Generates Javadoc for the project")
    public void javadoc() throws ExitStatusException, IOException, InterruptedException {
        new DokkaOperation()
                .fromProject(this)
                .loggingLevel(LoggingLevel.INFO)
                .execute();
    }
}