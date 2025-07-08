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
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
    protected static final DontCare DONT_CARE = DontCare.INSTANCE;
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

    public static Predicate<Object> greaterThan(double threshold) {
        return v -> {
            if (v instanceof Number) {
                return ((Number) v).doubleValue() > threshold;
            }
            return false;
        };
    }

    public static Predicate<Object> greaterThanOrEqualTo(double threshold) {
        return v -> {
            if (v instanceof Number) {
                return ((Number) v).doubleValue() >= threshold;
            }
            return false;
        };
    }

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

    private static void setupInfluxDBInstances() throws java.io.IOException, java.lang.InterruptedException, UnsupportedOperationException {
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

    public static List<Map<String, List<Map<String, Object>>>> queryAllInfluxInstances() {
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
            List<String> tablesToQuery = Arrays.asList("jenkins_data", "metrics_data", "agent_data");
            List<Map<String, List<Map<String, Object>>>> clientValues = new ArrayList<>();
            for (InfluxDBClient client : Arrays.asList(v1Client, v2Client)) {
                clientValues.add(queryV1V2API(client, tablesToQuery));
            }

            clientValues.add(queryV3API(v3Client, tablesToQuery));
            return clientValues;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, List<Map<String, Object>>> queryV1V2API(
            InfluxDBClient v1v2Client,
            List<String> tables
    ) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        QueryApi queryApi = v1v2Client.getQueryApi();

        for (String tableName : tables) {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: 0) " +
                            "|> filter(fn: (r) => r._measurement == \"%s\")" +
                            "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
                    testEnv.get("INFLUXDB_DATABASE_OR_BUCKET"),
                    tableName
            );
            ArrayList<Map<String, Object>> valueList = new ArrayList<>();

            List<FluxTable> fluxTables = queryApi.query(flux);
            assertEquals(1, fluxTables.size());
            FluxTable fluxTable = fluxTables.get(0);

            for (FluxRecord record : fluxTable.getRecords()) {
                Map<String, Object> recordValues = record.getValues();
                recordValues.remove("_stop");
                valueList.add(recordValues);
            }
            result.put(tableName, valueList);
        }
        return result;
    }

    protected void assertInfluxRecordsAreIdentical(
            Map<String, List<Map<String, Object>>> influxData,
            Map<String, List<Map<String, Object>>> expectedTablesAndRecords,
            Set<String> ignoreKeys
    ) {
        if (influxData.size() < 2) {
            throw new IllegalArgumentException("Need at least two InfluxDB results to compare");
        }

        // Compare table names
        Set<String> actualTables = influxData.keySet();
        Set<String> expectedTables = expectedTablesAndRecords.keySet();
        if (!actualTables.equals(expectedTables)) {
            throw new RuntimeException(
                    String.format(
                            "Mismatch in tables:\nExpected tables: %s\nActual tables: %s",
                            expectedTables,
                            actualTables
                    )
            );
        }

        // Compare each table
        for (String tableName : actualTables) {
            List<Map<String, Object>> actualRecords = influxData.get(tableName);
            List<Map<String, Object>> expectedRecords = expectedTablesAndRecords.get(tableName);

            // Canonicalize records (remove ignored keys and sort keys)
            List<Map<String, Object>> actualCanonical = canonicalizeRecords(actualRecords, ignoreKeys);
            List<Map<String, Object>> expectedCanonical = canonicalizeRecords(expectedRecords, ignoreKeys);

            assertEquals(expectedCanonical.size(), actualCanonical.size());

            for (int recordIndex = 0; recordIndex < expectedRecords.size(); recordIndex++) {
                Map<String, Object> expectedRecord = expectedCanonical.get(recordIndex);
                Map<String, Object> actualRecord = actualCanonical.get(recordIndex);
                assertEquals(expectedRecord.keySet(), actualRecord.keySet());
                for (Map.Entry<String, Object> entry : expectedRecord.entrySet()) {
                    String expectedKey = entry.getKey();
                    Object expectedValue = entry.getValue();
                    Object actualValue = actualRecord.get(expectedKey);
                    if (expectedValue.equals(DONT_CARE)) {
                        continue;
                    } else if (expectedValue instanceof Predicate<?>) {
                        @SuppressWarnings("unchecked")
                        Predicate<Object> predicate = (Predicate<Object>) expectedValue;
                        assertTrue(predicate.test(actualValue), "Entry %s=%s did not match predicate".formatted(
                                expectedKey, actualValue)
                        );
                    } else {
                        assertEquals(entry.getValue(), actualValue);
                    }
                }
            }
        }
    }

    private List<Map<String, Object>> canonicalizeRecords(List<Map<String, Object>> records, Set<String> ignoreKeys) {
        List<Map<String, Object>> canonical = new ArrayList<>();
        for (Map<String, Object> record : records) {
            Map<String, Object> cleaned = new TreeMap<>();
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                if (!ignoreKeys.contains(entry.getKey())) {
                    cleaned.put(entry.getKey(), entry.getValue());
                }
            }
            canonical.add(cleaned);
        }
        return canonical;
    }

    public static final class DontCare {
        public static final DontCare INSTANCE = new DontCare();

        private DontCare() {
        }

        @Override
        public String toString() {
            return "DONT_CARE";
        }
    }

}
