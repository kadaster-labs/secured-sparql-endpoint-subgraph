package nl.kadaster.labs.unlocked.securedsparqlendpoint.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GlobUtilTest {
    @Test
    void findAllReturnsMultipleFiles() throws IOException {
        var result = GlobUtil.findAll(
                Paths.get("./"),
                "**/*.java"
        );
        Assertions.assertFalse(result.isEmpty(), "There exist at least one Java class");
    }

    @Test
    void findFirstReturnsMatchOnExistingFile() throws IOException {
        var result = GlobUtil.findFirst(
                Paths.get("./"),
                "build.gradle"
        );
        Assertions.assertTrue(result.isPresent());
    }

    @Test
    void findFirstReturnsEmptyOnMissingFile() throws IOException {
        var result = GlobUtil.findFirst(
                Path.of("./src"),
                "non-existent-file.txt"
        );
        Assertions.assertTrue(result.isEmpty());
    }
}
