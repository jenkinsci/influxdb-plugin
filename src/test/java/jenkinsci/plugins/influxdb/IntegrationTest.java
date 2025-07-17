package jenkinsci.plugins.influxdb;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Shell;
import jenkinsci.plugins.influxdb.models.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledOnOs(OS.WINDOWS) // Docker is not available on Jenkins CI windows build agents
public class IntegrationTest extends IntegrationBaseTest {
    @Test
    public void testInfluxDBTargetsAreAvailable() {
        InfluxDbGlobalConfig globalConfig = InfluxDbGlobalConfig.getInstance();
        List<Target> targets = globalConfig.getTargets();
        assertEquals(2, targets.size());
        assertTrue(targets.get(0).isGlobalListener());
        assertTrue(targets.get(1).isGlobalListener());
        assertEquals(testEnv.get("INFLUXDB_V1_URL"), targets.get(0).getUrl());
        assertEquals(testEnv.get("INFLUXDB_V2_URL"), targets.get(1).getUrl());
    }

    @Test
    public void testInfluxDBReporting() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("influxdb-test-job");
        project.getBuildersList().add(new Shell("echo 'Hello from Integration Tests!'"));
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkinsRule.assertBuildStatus(Result.SUCCESS, build);
        Map<String, Object> values = queryAndCompareAllInfluxDBInstances();

        // Not all values are deterministic, such as build execution time. However, we want to test that all these
        // fields are included in the written values.
        Set<String> expectedKeys = Set.of(
                "build_cause",
                "agent_name",
                "build_time",
                "build_measured_time",
                "build_user",
                "project_name",
                "build_branch_name",
                "build_exec_time",
                "last_successful_build",
                "blocked_time",
                "build_agent_name",
                "build_status_message",
                "project_build_health",
                "build_scheduled_time",
                "build_result_ordinal",
                "building_time",
                "waiting_time",
                "time_in_queue",
                "subtask_count",
                "last_stable_build",
                "build_successful",
                "queue_time",
                "build_causer",
                "executor_utilization",
                "total_duration",
                "project_path",
                "build_result",
                "build_number",
                "executing_time",
                "buildable_time",
                "agent_label"
        );
        System.out.println("Missing from values: " + expectedKeys.stream().filter(k -> !values.containsKey(k)).toList());
        System.out.println("Unexpected in values: " + values.keySet().stream().filter(k -> !expectedKeys.contains(k)).toList());
        assertEquals(expectedKeys, values.keySet());

        // Compare all deterministic fields
        Map<String, Object> expectedValues = Map.ofEntries(
                Map.entry("build_cause", "LegacyCodeCause"),
                Map.entry("agent_name", "Jenkins"),
                Map.entry("project_name", "influxdb-test-job"),
                Map.entry("last_successful_build", 1L),
                Map.entry("build_agent_name", "built-in"),
                Map.entry("build_status_message", "stable"),
                Map.entry("project_build_health", 100L),
                Map.entry("build_result_ordinal", 0L),
                Map.entry("subtask_count", 0L),
                Map.entry("last_stable_build", 1L),
                Map.entry("build_successful", true),
                Map.entry("build_causer", "Legacy code started this job.  No cause information is available"),
                Map.entry("project_path", "influxdb-test-job"),
                Map.entry("build_result", "SUCCESS"),
                Map.entry("build_number", 1L)
        );
        for (Map.Entry<String, Object> entry : expectedValues.entrySet()) {
            assertEquals(entry.getValue(), values.get(entry.getKey()));
        }
    }
}
