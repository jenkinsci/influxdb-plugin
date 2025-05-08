package jenkinsci.plugins.influxdb.models;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.springframework.security.access.AccessDeniedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.logging.Logger;


public class InfluxDBClientWrapper {
    /**
     * The logger.
     **/
    private static final Logger logger = Logger.getLogger(InfluxDBClientWrapper.class.getName());

    /**
     * InfluxDB v1.X/v2.X client
     */
    private com.influxdb.client.InfluxDBClient v1v2client;

    public InfluxDBClientWrapper(
            @Nonnull String url,
            @Nullable String organization,
            @Nonnull String database,
            @Nullable String retentionPolicy,
            @Nullable StandardUsernamePasswordCredentials basicAuthCredentials,
            @Nullable StringCredentials tokenCredentials
    ) {
        // InfluxDB v2
        if (organization != null && !organization.trim().isEmpty()) {
            if (tryConnectV2(url, organization, database, basicAuthCredentials, tokenCredentials)) {
                return;
            }
        }
        // InfluxDB v1
        else if (tryConnectV1(url, database, retentionPolicy, basicAuthCredentials)) {
            return;
        }
        throw new RuntimeException("InfluxDB connection failed. Please check your connection parameters.");
    }

    public String getAPIVersion() {
        String version = this.v1v2client.version();
        logger.fine("API version: " + version);
        return version;
    }

    public void writePoints(List<Point> pointsToWrite) {
        this.v1v2client.getWriteApiBlocking().writePoints(pointsToWrite);
    }

    private boolean tryConnectV2(
            String url,
            String organization,
            String bucket,
            @Nullable StandardUsernamePasswordCredentials basicAuthCredentials,
            @Nullable StringCredentials tokenCredentials
    ) {
        logger.fine("Attempting connection to InfluxDB v2.X API at " + url);
        try {
            InfluxDBClientOptions.Builder options = InfluxDBClientOptions.builder().url(url).org(organization).bucket(bucket);
            if (basicAuthCredentials != null) {
                logger.fine("Attempting username/password authentication");
                options.authenticate(basicAuthCredentials.getUsername(), basicAuthCredentials.getPassword().getPlainText().toCharArray());
            } else {
                logger.fine("Attempting token authentication");
                options.authenticateToken(tokenCredentials.getSecret().getPlainText().toCharArray());
            }
            this.v1v2client = InfluxDBClientFactory.create(options.build());
            if (this.v1v2client.ping()) {
                logger.fine("Connection success");
                return true;
            } else {
                logger.fine("Connection failed");
            }
        } catch (InfluxException | AccessDeniedException e) {
            logger.warning("Connection failed: " + e.getMessage());
        }
        return false;
    }

    private boolean tryConnectV1(String url, String database, String retentionPolicy, @Nullable StandardUsernamePasswordCredentials credentials) {
        logger.fine("Attempting connection to InfluxDB v1.X API at " + url);
        try {
            if (credentials != null) {
                logger.fine("Attempting username/password authentication");
                this.v1v2client = InfluxDBClientFactory.createV1(url, credentials.getUsername(), credentials.getPassword().getPlainText().toCharArray(), database, retentionPolicy);
            } else {
                logger.fine("Attempting connection without credentials");
                this.v1v2client = InfluxDBClientFactory.createV1(url, "", "".toCharArray(), database, retentionPolicy);
            }
            if (this.v1v2client.ping()) {
                logger.fine("Connection success");
                return true;
            } else {
                logger.fine("Connection failed");
            }
        } catch (InfluxException | AccessDeniedException e) {
            logger.warning("Connection failed: " + e.getMessage());
        }
        return false;
    }

    public void close() {
        if (v1v2client != null) {
            v1v2client.close();
        }
    }
}
