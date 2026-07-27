package jenkinsci.plugins.influxdb;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxTable;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Shell;
import jenkinsci.plugins.influxdb.models.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledOnOs(OS.WINDOWS) // Docker is not available on Jenkins CI windows build agents
public class IntegrationTest extends IntegrationBaseTest {
    @Test
    public void testInfluxDBTargetsAreAvailable() {
        InfluxDbGlobalConfig globalConfig = InfluxDbGlobalConfig.getInstance();
        List<Target> targets = globalConfig.getTargets();
        assertEquals(3, targets.size());
        assertTrue(targets.get(0).isGlobalListener());
        assertTrue(targets.get(1).isGlobalListener());
        assertTrue(targets.get(2).isGlobalListener());
        assertEquals(testEnv.get("INFLUXDB_V1_URL"), targets.get(0).getUrl());
        assertEquals(testEnv.get("INFLUXDB_V2_URL"), targets.get(1).getUrl());
        assertEquals(testEnv.get("INFLUXDB_V3_URL"), targets.get(2).getUrl());
    }

    @Test
    public void testInfluxDBReporting() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("influxdb-test-job");
        project.getBuildersList().add(new Shell("echo 'Hello from Integration Tests!'"));
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkinsRule.assertBuildStatus(Result.SUCCESS, build);
        List<Map<String, List<Map<String, Object>>>> influxData = queryAllInfluxInstances();
        Set<String> ignoreKeys = new HashSet<>(Arrays.asList("_time", "_start", "_stop", "result", "_measurement", "table"));
        Map<String, List<Map<String, Object>>> expectedValues = Map.ofEntries(
                Map.entry("jenkins_data", List.of(Map.ofEntries(
                        Map.entry("build_result", "SUCCESS"),
                        Map.entry("instance", DONT_CARE),
                        Map.entry("project_name", "influxdb-test-job"),
                        Map.entry("project_namespace", "influxdb-test-job"),
                        Map.entry("project_path", "influxdb-test-job"),
                        Map.entry("build_agent_name", "built-in"),
                        Map.entry("build_branch_name", DONT_CARE),
                        Map.entry("build_cause", "LegacyCodeCause"),
                        Map.entry("build_causer", "Legacy code started this job.  No cause information is available"),
                        Map.entry("build_exec_time", greaterThan(0)),
                        Map.entry("build_measured_time", greaterThan(0)),
                        Map.entry("build_number", 1L),
                        Map.entry("build_result_ordinal", 0L),
                        Map.entry("build_scheduled_time", greaterThanOrEqualTo(0)),
                        Map.entry("build_status_message", "stable"),
                        Map.entry("build_successful", true),
                        Map.entry("build_time", greaterThan(0)),
                        Map.entry("build_user", DONT_CARE),
                        Map.entry("last_stable_build", 1L),
                        Map.entry("last_successful_build", 1L),
                        Map.entry("project_build_health", 100L),
                        Map.entry("time_in_queue", greaterThanOrEqualTo(0))
                ))),
                Map.entry("agent_data", List.of(Map.ofEntries(
                        Map.entry("instance", DONT_CARE),
                        Map.entry("project_name", "influxdb-test-job"),
                        Map.entry("project_namespace", "influxdb-test-job"),
                        Map.entry("project_path", "influxdb-test-job"),
                        Map.entry("unique_id", "1"),
                        Map.entry("agent_label", DONT_CARE),
                        Map.entry("agent_name", "Jenkins"),
                        Map.entry("build_number", 1L)
                ))),
                Map.entry("metrics_data", List.of(Map.ofEntries(
                        Map.entry("instance", DONT_CARE),
                        Map.entry("project_name", "influxdb-test-job"),
                        Map.entry("project_namespace", "influxdb-test-job"),
                        Map.entry("project_path", "influxdb-test-job"),
                        Map.entry("blocked_time", greaterThanOrEqualTo(0)),
                        Map.entry("build_number", 1L),
                        Map.entry("buildable_time", greaterThan(0)),
                        Map.entry("building_time", greaterThan(0)),
                        Map.entry("executing_time", greaterThan(0)),
                        Map.entry("executor_utilization", greaterThan(0)),
                        Map.entry("queue_time", greaterThanOrEqualTo(0)),
                        Map.entry("subtask_count", greaterThanOrEqualTo(0)),
                        Map.entry("total_duration", greaterThan(0)),
                        Map.entry("waiting_time", greaterThanOrEqualTo(0))
                )))
        );

        this.assertInfluxRecordsAreIdentical(influxData.get(0), expectedValues, ignoreKeys);
        this.assertInfluxRecordsAreIdentical(influxData.get(1), expectedValues, ignoreKeys);
        this.assertInfluxRecordsAreIdentical(influxData.get(2), expectedValues, ignoreKeys);
    }

        @Test
        public void testMeasurementRegexAppliesToPublisherCall() throws Exception {
                String projectName = "influxdb-regex-test-job-" + System.currentTimeMillis();
                InfluxDbGlobalConfig globalConfig = InfluxDbGlobalConfig.getInstance();
                Map<String, Boolean> originalGlobalListenerFlags = new HashMap<>();

                for (Target target : globalConfig.getTargets()) {
                        originalGlobalListenerFlags.put(target.getDescription(), target.isGlobalListener());
                        target.setGlobalListener(false);
                }

                FreeStyleProject project = jenkinsRule.createFreeStyleProject(projectName);
                project.getBuildersList().add(new Shell("echo 'Measurement regex integration test'"));

                InfluxDbPublisher publisher = new InfluxDbPublisher("InfluxDB v1");
                publisher.setMeasurementRegex("^jenkins_data$");
                project.getPublishersList().add(publisher);

                try {
                        FreeStyleBuild build = project.scheduleBuild2(0).get();
                        jenkinsRule.assertBuildStatus(Result.SUCCESS, build);

                        try (InfluxDBClient v1Client = InfluxDBClientFactory.createV1(
                                        testEnv.get("INFLUXDB_V1_URL"),
                                        testEnv.get("INFLUXDB_USERNAME"),
                                        testEnv.get("INFLUXDB_PASSWORD").toCharArray(),
                                        testEnv.get("INFLUXDB_DATABASE_OR_BUCKET"),
                                        null
                        )) {
                                assertEquals(1L, countRecordsForProject(v1Client, "jenkins_data", projectName));
                                assertEquals(0L, countRecordsForProject(v1Client, "metrics_data", projectName));
                                assertEquals(0L, countRecordsForProject(v1Client, "agent_data", projectName));
                        }
                } finally {
                        for (Target target : globalConfig.getTargets()) {
                                Boolean original = originalGlobalListenerFlags.get(target.getDescription());
                                if (original != null) {
                                        target.setGlobalListener(original);
                                }
                        }
                }
        }

        private long countRecordsForProject(InfluxDBClient client, String measurementName, String projectName) {
                QueryApi queryApi = client.getQueryApi();
                String flux = String.format(
                                "from(bucket: \"%s\") " +
                                                "|> range(start: 0) " +
                                                "|> filter(fn: (r) => r._measurement == \"%s\")" +
                                                "|> filter(fn: (r) => r.project_name == \"%s\")" +
                                                "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
                                testEnv.get("INFLUXDB_DATABASE_OR_BUCKET"),
                                measurementName,
                                projectName
                );
                List<FluxTable> tables = queryApi.query(flux);
                return tables.stream().mapToLong(table -> table.getRecords().size()).sum();
        }
}
