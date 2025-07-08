package jenkinsci.plugins.influxdb;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
@Testcontainers
public class IntegrationBaseTest {
    protected static final Map<String, String> testEnv = new HashMap<>(Map.of(
            "INFLUXDB_USERNAME", "test_user",
            "INFLUXDB_PASSWORD", "test_password",
            "INFLUXDB_V2_TOKEN", "test_token",
            "INFLUXDB_DATABASE_OR_BUCKET", "test_db_or_bucket",
            "INFLUXDB_ORGANIZATION", "test_org"
    ));

    protected static final Map<String, GenericContainer<?>> influxContainers = Map.of(
            "V1",
            new InfluxDBContainer<>(DockerImageName.parse("influxdb:1.11"))
                    .withDatabase(testEnv.get("INFLUXDB_DATABASE_OR_BUCKET"))
                    .withUsername(testEnv.get("INFLUXDB_USERNAME"))
                    .withEnv(
                            Map.of("INFLUXDB_HTTP_FLUX_ENABLED", "true")
                    )
                    .withPassword(testEnv.get("INFLUXDB_PASSWORD")),
            "V2",
            new InfluxDBContainer<>(DockerImageName.parse("influxdb:2.7"))
                    .withBucket(testEnv.get("INFLUXDB_DATABASE_OR_BUCKET"))
                    .withUsername(testEnv.get("INFLUXDB_USERNAME"))
                    .withPassword(testEnv.get("INFLUXDB_PASSWORD"))
                    .withAdminToken(testEnv.get("INFLUXDB_V2_TOKEN"))
                    .withOrganization(testEnv.get("INFLUXDB_ORGANIZATION"))
    );
    protected static JenkinsRule jenkinsRule;

    @AfterAll
    public static void tearDown() throws Exception {
        for (Map.Entry<String, GenericContainer<?>> entry : influxContainers.entrySet()) {
            System.out.printf("Stopping InfluxDB %s container%n", entry.getKey());
            entry.getValue().stop();
        }
    }

    @BeforeAll
    public static void setUp(JenkinsRule rule) throws Exception {
        jenkinsRule = Objects.requireNonNull(rule);
        setupInfluxDBInstances();
        runConfigurationAsCode();
    }

    private static void setupInfluxDBInstances() {
        influxContainers.forEach((version, container) -> {
            System.out.printf("Starting InfluxDB %s container%n", version);
            container.start();
            assertTrue(container.isRunning());
            testEnv.put(
                    "INFLUXDB_%s_URL".formatted(version),
                    "http://localhost:" + container.getMappedPort(8086).toString()
            );
        });
    }

    private static void runConfigurationAsCode() throws Exception {
        testEnv.forEach(System::setProperty);
        String yamlUrl = Objects.requireNonNull(IntegrationBaseTest.class.getResource(
                IntegrationBaseTest.class.getSimpleName() + "/configuration-as-code.yml"
        )).toString();
        ConfigurationAsCode.get().configure(yamlUrl);
    }

    public static Map<String, Object> queryAndCompareAllInfluxDBInstances() {
        try (
                InfluxDBClient v1Client = InfluxDBClientFactory.createV1(
                        testEnv.get("INFLUXDB_V1_URL"),
                        testEnv.get("INFLUXDB_USERNAME"),
                        testEnv.get("INFLUXDB_PASSWORD").toCharArray(),
                        testEnv.get("INFLUXDB_DATABASE_OR_BUCKET"),
                        null
                );
                InfluxDBClient v2Client = InfluxDBClientFactory.create(
                        InfluxDBClientOptions.builder()
                                .url(testEnv.get("INFLUXDB_V2_URL"))
                                .org(testEnv.get("INFLUXDB_ORGANIZATION"))
                                .bucket(testEnv.get("INFLUXDB_DATABASE_OR_BUCKET"))
                                .authenticateToken(testEnv.get("INFLUXDB_V2_TOKEN").toCharArray()).build()
                );
        ) {
            String flux = "from(bucket:\"%s\") |> range(start: 0)".formatted(testEnv.get("INFLUXDB_DATABASE_OR_BUCKET"));
            ArrayList<Map<String, Object>> valueList = new ArrayList<>();
            for (InfluxDBClient client : Arrays.asList(v1Client, v2Client)) {
                Map<String, Object> values = new HashMap<>();
                QueryApi queryApi = client.getQueryApi();
                List<FluxTable> tables = queryApi.query(flux);
                for (FluxTable fluxTable : tables) {
                    for (FluxRecord record : fluxTable.getRecords()) {
                        values.put(record.getField(), record.getValue());
                    }
                }
                valueList.add(values);
            }

            // The data reported to InfluxDB v1.X and v2.X must be identical.
            assertEquals(valueList.get(0), valueList.get(1));
            return valueList.get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
