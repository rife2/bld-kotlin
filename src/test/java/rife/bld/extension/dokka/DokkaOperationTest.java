/*
 * Copyright 2023-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;
import rife.bld.blueprints.BaseProjectBlueprint;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class DokkaOperationTest {
    @Test
    @SuppressWarnings({"ExtractMethodRecommender", "PMD.AvoidDuplicateLiterals"})
    void executeConstructProcessCommandListTest() {
        var examples = new File("examples");
        var args = new DokkaOperation()
                .fromProject(new BaseProjectBlueprint(examples, "com.example", "Example"))
                .globalLinks("s", "link")
                .globalLinks(Map.of("s2", "link2"))
                .globalPackageOptions("option1", "option2")
                .globalPackageOptions(List.of("option3", "option4"))
                .globalSrcLink("link1", "link2")
                .globalSrcLink(List.of("link3", "link4"))
                .includes("file1", "file2")
                .pluginConfiguration("name", "\"json\"")
                .pluginConfiguration(Map.of("\"name2\"", "json2"))
                .pluginsClasspath("path1", "path2")
                .pluginsClasspath(List.of("path3", "path4"))
                .delayTemplateSubstitution(true)
                .failOnWarning(true)
                .loggingLevel(LoggingLevel.DEBUG)
                .moduleName("name")
                .moduleVersion("1.0")
                .noSuppressObviousFunctions(true)
                .offlineMode(true)
                .outputDir(new File(examples, "build"))
                .outputFormat(OutputFormat.JAVADOC)
                .suppressInheritedMembers(true)
                .executeConstructProcessCommandList();

        var path = examples.getAbsolutePath();
        var dokkaJar = "1.9.20.jar";
        var matches = List.of("java",
                "-jar", path + "/lib/bld/dokka-cli-" + dokkaJar,
                "-pluginsClasspath", path + "/lib/bld/dokka-base-" + dokkaJar + ';' +
                        path + "/lib/bld/analysis-kotlin-descriptors-" + dokkaJar + ';' +
                        path + "/lib/bld/javadoc-plugin-" + dokkaJar + ';' +
                        path + "/lib/bld/korte-jvm-4.0.10.jar;" +
                        path + "/lib/bld/kotlin-as-java-plugin-" + dokkaJar + ";path1;path2;path3;path4",
                "-sourceSet", "-src " + path + "/src/main/kotlin",
                "-outputDir", path + "/build",
                "-delayTemplateSubstitution", "true",
                "-failOnWarning", "true",
                "-globalLinks", "s^link^^s2^link2",
                "-globalPackageOptions", "option1;option2;option3;option4",
                "-globalSrcLinks_", "link1;link2;link3;link4",
                "-includes", "file1;file2",
                "-loggingLevel", "debug",
                "-moduleName", "name",
                "-moduleVersion", "1.0",
                "-noSuppressObviousFunctions", "true",
                "-offlineMode", "true",
                "-pluginConfiguration", "{name}={\\\"json\\\"}^^{\\\"name2\\\"}={json2}",
                "-suppressInheritedMembers", "true");

        assertThat(args).hasSize(matches.size());

        IntStream.range(0, args.size()).forEach(i -> {
            if (args.get(i).contains(".jar;")) {
                var jars = args.get(i).split(";");
                Arrays.stream(jars).forEach(jar -> assertThat(matches.get(i)).contains(jar));
            } else {
                assertThat(args.get(i)).isEqualTo(matches.get(i));
            }
        });
    }
}
