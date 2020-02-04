package jenkinsci.plugins.influxdb.generators.serenity;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SerenityCannedJsonSummaryFile implements ISerenityJsonSummaryFile {

    public SerenityCannedJsonSummaryFile() {}

    public boolean exists() {
        return Files.exists(getPath());
    }

    public Path getPath() {
        try {
            return Paths.get(ClassLoader.getSystemResource("serenity/serenity-summary.json").toURI());
        } catch (URISyntaxException e) {
            //
        }
        return null;
    }

    public String getContents() throws IOException {
        return new String(Files.readAllBytes(getPath()), StandardCharsets.UTF_8);
    }
}
