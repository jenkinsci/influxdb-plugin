package jenkinsci.plugins.influxdb.generators.serenity;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SerenityJsonSummaryFileTest {
    @TempDir
    private File temporaryFolder;

    @Test
    void testAvailableSummary() throws Exception {
        writeToTemporaryPath("target/site/serenity/serenity-summary.json", "expected summary content");

        SerenityJsonSummaryFile serenityJsonSummaryFile = new SerenityJsonSummaryFile(pathOfTemporaryFolder());

        assertTrue(serenityJsonSummaryFile.exists());
        assertEquals("expected summary content", serenityJsonSummaryFile.getContents());
    }

    @Test
    void testUnavailableSummary() {
        SerenityJsonSummaryFile serenityJsonSummaryFile = new SerenityJsonSummaryFile(pathOfTemporaryFolder());

        assertFalse(serenityJsonSummaryFile.exists());
    }

    @Test
    void testUnavailableWorkspace() {
        SerenityJsonSummaryFile serenityJsonSummaryFile = new SerenityJsonSummaryFile(null);

        assertFalse(serenityJsonSummaryFile.exists());
    }

    private void writeToTemporaryPath(String path, String content) throws IOException {
        Path temporaryPath = temporaryFolder.toPath();
        Path summaryJson = temporaryPath.resolve(path);
        Files.createDirectories(summaryJson.getParent());
        Files.writeString(summaryJson, content, StandardCharsets.UTF_8);
    }

    private String pathOfTemporaryFolder() {
        return temporaryFolder.getAbsolutePath();
    }
}