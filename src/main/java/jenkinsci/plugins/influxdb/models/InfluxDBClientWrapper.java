package jenkinsci.plugins.influxdb.models;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.v3.client.config.ClientConfig;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.springframework.security.access.AccessDeniedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class InfluxDBClientWrapper {
    /**
     * The logger.
     **/
    private static final Logger logger = Logger.getLogger(InfluxDBClientWrapper.class.getName());

    /**
     * Shared HTTP client which can make use of connection and thread pooling.
     */
    private static final OkHttpClient httpClient = new OkHttpClient();

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
            @Nullable StringCredentials tokenCredentials,
            boolean usingJenkinsProxy
    ) {
        // InfluxDB v2.X
        if (organization != null && !organization.trim().isEmpty()) {
            if (tryConnectV2(url, organization, database, basicAuthCredentials, tokenCredentials, usingJenkinsProxy)) {
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

    private boolean tryConnectV2(
            String url,
            String organization,
            String bucket,
            @Nullable StandardUsernamePasswordCredentials basicAuthCredentials,
            @Nullable StringCredentials tokenCredentials,
            boolean usingJenkinsProxy
    ) {
        logger.fine("Attempting connection to InfluxDB v2.X API at " + url);
        boolean success = false;
        try {
            InfluxDBClientOptions.Builder options = InfluxDBClientOptions.builder()
                    .url(url)
                    .org(organization)
                    .bucket(bucket)
                    .okHttpClient(createHttpClient(new URL(url), usingJenkinsProxy));
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
        } catch (InfluxException | AccessDeniedException | MalformedURLException e) {
            logger.warning("Connection failed: " + e.getMessage());
        } finally {
            if (!success) {
                this.closeAndResetAllClients();
            }
        }
        return success;
    }

    private boolean tryConnectV3(
            String url,
            String database,
            @Nullable StringCredentials tokenCredentials
    ) {
        logger.fine("Attempting connection to InfluxDB v3.X API at " + url);
        boolean success = false;
        try {
            if (tokenCredentials != null) {
                logger.fine("Attempting token authentication");
                com.influxdb.v3.client.config.ClientConfig config = new ClientConfig.Builder()
                        .host(url)
                        .token(tokenCredentials.getSecret().getPlainText().toCharArray())
                        .database(database)
                        .build();
                this.v3client = com.influxdb.v3.client.InfluxDBClient.getInstance(config);
                if (this.getAPIVersion().startsWith("3")) {
                    logger.fine("Connection success");
                    success = true;
                } else {
                    logger.fine("Connection failed");
                    this.v3client.close();
                }
            } else {
                logger.fine("Token credentials not found, skipping InfluxDB v3.X API");
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
            String apiVersion = this.getAPIVersion();
            // InfluxDB v1.11 returns v1.X instead of 1.X
            if (this.v1v2client.ping() && (apiVersion.startsWith("1") || apiVersion.startsWith("v1"))) {
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

    private OkHttpClient.Builder createHttpClient(URL url, boolean useProxy) {
        OkHttpClient.Builder builder = httpClient.newBuilder();
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        ProxyConfiguration proxyConfig = jenkins == null ? null : jenkins.proxy;
        if (useProxy && proxyConfig != null) {
            builder.proxy(proxyConfig.createProxy(url.getHost()));
            if (proxyConfig.getUserName() != null) {
                builder.proxyAuthenticator((route, response) -> {
                    if (response.request().header("Proxy-Authorization") != null) {
                        return null; // Give up, we've already failed to authenticate.
                    }

                    String credential = Credentials.basic(proxyConfig.getUserName(), proxyConfig.getSecretPassword().getPlainText());
                    return response.request().newBuilder().header("Proxy-Authorization", credential).build();
                });
            }
        }
        return builder;
    }

    public String getAPIVersion() {
        String version;
        if (this.v1v2client != null) {
            version = this.v1v2client.version();
        } else if (this.v3client != null) {
            version = this.v3client.getServerVersion();
        } else {
            throw new RuntimeException("Unable to query InfluxDB server version, all servers are NULL.");
        }
        logger.fine("API version: " + version);
        return version;
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
            }
        }
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

    public void close() {
        this.closeAndResetAllClients();
    }
}
