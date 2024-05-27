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

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SourceSetTest {
    @Test
    void sourceSetCollectionsTest() {
        var args = new SourceSet()
                .classpath(List.of("path1", "path2"))
                .dependentSourceSets(Map.of("set1", "set2", "set3", "set4"))
                .externalDocumentationLinks(Map.of("link1", "link2", "link3", "link4"))
                .perPackageOptions(List.of("option1", "option2"))
                .samples(List.of("samples1", "samples1"))
                .suppressedFiles(List.of("sup1", "sup2"))
                .args();

        var matches = List.of(
                "-classpath", "path1;path2",
                "-dependentSourceSets", "set1/set2;set3/set4",
                "-externalDocumentationLinks", "link3^link4^^link1^link2",
                "-perPackageOptions", "option1;option2",
                "-samples", "samples1;samples1",
                "-suppressedFiles", "sup1;sup2"
        );

        assertThat(args).hasSize(matches.size());

        IntStream.range(0, args.size()).forEach(i -> assertThat(args.get(i)).isEqualTo(matches.get(i)));
    }

    @Test
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    void sourceSetTest() {
        var params = List.of(
                "-analysisPlatform",
                "-apiVersion",
                "-classpath",
                "-dependentSourceSets",
                "-displayName",
                "-documentedVisibilities",
                "-externalDocumentationLinks",
                "-includes",
                "-jdkVersion",
                "-languageVersion",
                "-noJdkLink",
                "-noSkipEmptyPackages",
                "-noStdlibLink",
                "-perPackageOptions",
                "-reportUndocumented",
                "-samples",
                "-skipDeprecated",
                "-sourceSetName",
                "-src",
                "-srcLink",
                "-suppressedFiles"
        );
        var sourceSet = new SourceSet()
                .analysisPlatform(AnalysisPlatform.JVM)
                .apiVersion("1.0")
                .classpath("classpath1", "classpath2")
                .dependentSourceSets("moduleName", "sourceSetName")
                .displayName("name")
                .documentedVisibilities(DocumentedVisibility.PACKAGE, DocumentedVisibility.PRIVATE)
                .externalDocumentationLinks("url1", "packageListUrl1")
                .externalDocumentationLinks("url2", "packageListUrl2")
                .includes("includes1", "includes2")
                .jdkVersion(18)
                .languageVersion("2.0")
                .noJdkLink(true)
                .noSkipEmptyPackages(true)
                .noStdlibLink(true)
                .perPackageOptions("options1", "options2")
                .reportUndocumented(true)
                .samples("samples1", "sample2")
                .skipDeprecated(true)
                .sourceSetName("setName")
                .src("src1", "src2")
                .srcLink("path1", "remote1", "#suffix1")
                .srcLink("path2", "remote2", "#suffix2")
                .suppressedFiles("sup1", "sup2");

        var args = sourceSet.args();

        for (var p : params) {
            var found = false;
            for (var a : args) {
                if (a.startsWith(p)) {
                    found = true;
                    break;
                }
            }
            assertThat(found).as(p + " not found.").isTrue();
        }

        var matches = List.of(
                "-analysisPlatform", "jvm",
                "-apiVersion", "1.0",
                "-classpath", "classpath1;classpath2",
                "-dependentSourceSets", "moduleName/sourceSetName",
                "-displayName", "name",
                "-documentedVisibilities", "package;private",
                "-externalDocumentationLinks", "url1^packageListUrl1^^url2^packageListUrl2",
                "-jdkVersion", "18",
                "-includes", "includes1;includes2",
                "-languageVersion", "2.0",
                "-noJdkLink", "true",
                "-noSkipEmptyPackages", "true",
                "-noStdlibLink", "true",
                "-reportUndocumented", "true",
                "-perPackageOptions", "options1;options2",
                "-samples", "samples1;sample2",
                "-skipDeprecated", "true",
                "-src", "src1;src2",
                "-srcLink", "path1=remote1#suffix1;path2=remote2#suffix2",
                "-sourceSetName", "setName",
                "-suppressedFiles", "sup1;sup2");

        assertThat(args).hasSize(matches.size());

        IntStream.range(0, args.size()).forEach(i -> assertThat(args.get(i)).isEqualTo(matches.get(i)));

        sourceSet.classpath(List.of("classpath1", "classpath2"));

        IntStream.range(0, args.size()).forEach(i -> assertThat(args.get(i)).isEqualTo(matches.get(i)));
    }
}
