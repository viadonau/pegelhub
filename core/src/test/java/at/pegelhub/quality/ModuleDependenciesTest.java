package at.pegelhub.quality;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleDependenciesTest {
    @Test
    void featureInternalsAreNotImportedAcrossModules() throws Exception {
        Path root = Path.of("src/main/java/at/pegelhub");

        try (var paths = Files.walk(root)) {
            for (var path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                String module = root.relativize(path).getName(0).toString();

                if (!module.equals("quality") && !module.equals("notifications")) {
                    assertThat(source).as(path.toString())
                            .doesNotContain("import at.pegelhub.quality.", "import at.pegelhub.notifications.");
                }
                if (module.equals("quality")) {
                    assertThat(source).as(path.toString()).doesNotContain(
                            "import at.pegelhub.notifications.persistence.",
                            "import at.pegelhub.notifications.transport.",
                            "import at.pegelhub.measurement.persistence.",
                            "import at.pegelhub.timeseries.persistence.");
                }
                if (module.equals("notifications")) {
                    assertThat(source).as(path.toString()).doesNotContain("import at.pegelhub.quality.");
                }
            }
        }
    }
}
