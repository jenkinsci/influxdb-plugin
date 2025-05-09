package jenkinsci.plugins.influxdb.models;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.v3.client.config.ClientConfig;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.springframework.security.access.AccessDeniedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class InfluxDBClientWrapper {
    /**
     * The logger.
     **/
    private static final Logger logger = Logger.getLogger(InfluxDBClientWrapper.class.getName());

    /**
     * Store for the v3 API version. Can be removed as soon as the v3 client library offers a .version() method.
     */
    private String v3Version = null;

    /**
     * InfluxDB v1.X/v2.X client
     */
    private com.influxdb.client.InfluxDBClient v1v2client;
    /**
     * InfluxDB v3.X client
     */
    private com.influxdb.v3.client.InfluxDBClient v3client;

    public InfluxDBClientWrapper(
            @Nonnull String url,
            @Nullable String organization,
            @Nonnull String database,
            @Nullable String retentionPolicy,
            @Nullable StandardUsernamePasswordCredentials basicAuthCredentials,
            @Nullable StringCredentials tokenCredentials
    ) {
        // InfluxDB v2.X
        if (organization != null && !organization.trim().isEmpty()) {
            if (tryConnectV2(url, organization, database, basicAuthCredentials, tokenCredentials)) {
                return;
            }
        }
        // InfluxDB v3.X/v1.X
        else {
            if (tryConnectV3(url, database, tokenCredentials)) {
                return;
            } else if (tryConnectV1(url, database, retentionPolicy, basicAuthCredentials)) {
                return;
            }
        }
        throw new RuntimeException("InfluxDB connection failed. Please check your connection parameters.");
    }

    public String getAPIVersion() {
        if (this.v3client != null && this.v3Version != null) {
            // Can be replaced with .version() call as soon as the v3 client library offers a .version() method.
            return this.v3Version;
        }
        String version = this.v1v2client.version();
        logger.fine("API version: " + version);
        return version;
    }

    /**
     * Temporary helper function that queries the InfluxDB v3 API for the version.
     * Can be removed as soon as the v3 client library offers a .version() method.
     *
     * @param url   The InfluxDB v3 API URL, e.g. http://localhost:8181.
     * @param token The API token.
     * @return The version, if the request was successful, otherwise null.
     */
    public String getV3APIVersion(String url, String token) {
        HttpResponse<String> response;
        JsonObject jsonResponse;
        String version = null;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/ping"))
                    .header("Authorization", "Bearer " + token)
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assert response != null;
        } catch (Exception e) {
            logger.fine("Failed to get InfluxDB v3 API version: " + e.getMessage());
            return null;
        }
        if (response.statusCode() != 200) {
            logger.fine("Failed to get InfluxDB v3 API version: HTTP status code " + response.statusCode());
            return null;
        }
        try {
            jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            version = jsonResponse.get("version").getAsString();
        } catch (Exception e) {
            logger.fine("Failed to parse InfluxDB v3 API version: " + e.getMessage());
        }

        if (version != null && version.startsWith("3")) {
            return version;
        }
        logger.fine("Found invalid InfluxDB v3 API version: " + version);
        return null;
    }

    public void writePoints(List<AbstractPoint> pointsToWrite) {
        if (this.v3client != null) {
            List<com.influxdb.v3.client.Point> v3Points = pointsToWrite.stream()
                    .map(AbstractPoint::getV3Point)
                    .collect(Collectors.toList());
            this.v3client.writePoints(v3Points);

        } else if (this.v1v2client != null) {
            List<com.influxdb.client.write.Point> v1v2Points = pointsToWrite.stream()
                    .map(AbstractPoint::getV1v2Point)
                    .toList();
            this.v1v2client.getWriteApiBlocking().writePoints(v1v2Points);
        } else {
            throw new RuntimeException("InfluxDB client is not initialized.");
        }
    }

    private boolean tryConnectV3(
            String url,
            String database,
            @Nullable StringCredentials tokenCredentials
    ) {
        logger.fine("Attempting connection to InfluxDB v3.X API at " + url);
        boolean success = false;
        try {
            com.influxdb.v3.client.config.ClientConfig config = new ClientConfig.Builder()
                    .host(url)
                    .token(tokenCredentials.getSecret().getPlainText().toCharArray())
                    .database(database)
                    .build();
            this.v3client = com.influxdb.v3.client.InfluxDBClient.getInstance(config);
            // Can be replaced with getAPIVersion() call as soon as the v3 client library offers a .version() method.
            this.v3Version = this.getV3APIVersion(url, tokenCredentials.getSecret().getPlainText());
            if (this.v3Version != null && this.v3Version.startsWith("3")) {
                logger.fine("Connection success");
                success = true;
            } else {
                logger.fine("Connection failed");
                this.v3client.close();
            }
        } catch (Exception e) {
            logger.warning("Connection failed: " + e.getMessage());
        } finally {
            if (!success) {
                this.closeAndResetAllClients();
            }
        }
        return success;
    }

    private boolean tryConnectV2(
            String url,
            String organization,
            String bucket,
            @Nullable StandardUsernamePasswordCredentials basicAuthCredentials,
            @Nullable StringCredentials tokenCredentials
    ) {
        logger.fine("Attempting connection to InfluxDB v2.X API at " + url);
        boolean success = false;
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
            if (this.v1v2client.ping() && this.getAPIVersion().startsWith("v2")) {
                logger.fine("Connection success");
                success = true;
            } else {
                this.v1v2client.close();
                logger.fine("Connection failed");
            }
        } catch (InfluxException | AccessDeniedException e) {
            logger.warning("Connection failed: " + e.getMessage());
        } finally {
            if (!success) {
                this.closeAndResetAllClients();
            }
        }
        return success;
    }

    private boolean tryConnectV1(String url, String database, String retentionPolicy, @Nullable StandardUsernamePasswordCredentials credentials) {
        logger.fine("Attempting connection to InfluxDB v1.X API at " + url);
        boolean success = false;
        try {
            if (credentials != null) {
                logger.fine("Attempting username/password authentication");
                this.v1v2client = InfluxDBClientFactory.createV1(url, credentials.getUsername(), credentials.getPassword().getPlainText().toCharArray(), database, retentionPolicy);
            } else {
                logger.fine("Attempting connection without credentials");
                this.v1v2client = InfluxDBClientFactory.createV1(url, "", "".toCharArray(), database, retentionPolicy);
            }
            if (this.v1v2client.ping() && this.getAPIVersion().startsWith("v1")) {
                logger.fine("Connection success");
                success = true;
            } else {
                logger.fine("Connection failed");
            }
        } catch (InfluxException | AccessDeniedException e) {
            logger.warning("Connection failed: " + e.getMessage());
        } finally {
            if (!success) {
                this.closeAndResetAllClients();
            }
        }
        return success;
    }

    private void closeAndResetAllClients() {
        if (v1v2client != null) {
            try {
                v1v2client.close();
            } catch (Exception e) {
                logger.warning("Failed to close InfluxDB v1.X/v2.X client: " + e.getMessage());
            } finally {
                this.v1v2client = null;
            }
        }
        if (v3client != null) {
            try {
                v3client.close();
            } catch (Exception e) {
                logger.warning("Failed to close InfluxDB v3.X client: " + e.getMessage());
            } finally {
                this.v3client = null;
                this.v3Version = null;
            }
        }
    }

    public void close() {
        this.closeAndResetAllClients();
    }
}
