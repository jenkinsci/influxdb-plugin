package jenkinsci.plugins.influxdb.generators.serenity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SerenityJsonSummaryFileTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testAvailableSummary() throws Exception {
        writeToTemporaryPath("target/site/serenity/serenity-summary.json", "expected summary content");

        SerenityJsonSummaryFile serenityJsonSummaryFile = new SerenityJsonSummaryFile(pathOfTemporaryFolder());

        assertTrue(serenityJsonSummaryFile.exists());
        assertEquals("expected summary content", serenityJsonSummaryFile.getContents());
    }

    @Test
    public void testUnavailableSummary() throws Exception {
        SerenityJsonSummaryFile serenityJsonSummaryFile = new SerenityJsonSummaryFile(pathOfTemporaryFolder());

        assertFalse(serenityJsonSummaryFile.exists());
    }

    @Test
    public void testUnavailableWorkspace() throws Exception {
        SerenityJsonSummaryFile serenityJsonSummaryFile = new SerenityJsonSummaryFile(null);

        assertFalse(serenityJsonSummaryFile.exists());
    }

    private void writeToTemporaryPath(String path, String content) throws IOException {
        Path temporaryPath = temporaryFolder.getRoot().toPath();
        Path summaryJson = temporaryPath.resolve(path);
        Files.createDirectories(summaryJson.getParent());
        Files.writeString(summaryJson, content, StandardCharsets.UTF_8);
    }

    private String pathOfTemporaryFolder() {
        return temporaryFolder.getRoot().getAbsolutePath();
    }
}