package jenkinsci.plugins.influxdb.generators.serenity;

import java.io.IOException;
import java.nio.file.Path;

public interface ISerenityJsonSummaryFile {
    boolean exists();
    Path getPath();
    String getContents() throws IOException;
}