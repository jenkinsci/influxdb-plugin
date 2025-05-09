package jenkinsci.plugins.influxdb.generators.serenity;

import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.generators.PointGeneratorBaseTest;
import jenkinsci.plugins.influxdb.models.AbstractPoint;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SerenityPointGeneratorTest extends PointGeneratorBaseTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";
    AbstractPoint point = null;
    private Run build;
    private ProjectNameRenderer measurementRenderer;
    private long currTime;
    private TaskListener listener;

    @BeforeEach
    void before() {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        listener = Mockito.mock(TaskListener.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("folder/" + JOB_NAME);

        currTime = System.currentTimeMillis();

        SerenityCannedJsonSummaryFile serenityCannedJsonSummaryFile = new SerenityCannedJsonSummaryFile();
        // build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix,
        SerenityPointGenerator serenityGen = new SerenityPointGenerator(build, listener, measurementRenderer, currTime,
                StringUtils.EMPTY, null, serenityCannedJsonSummaryFile);

        if (serenityGen.hasReport()) {
            List<AbstractPoint> pointsToWrite = new ArrayList<>(Arrays.asList(serenityGen.generate()));
            // points.fields is private so just get all fields as a single string
            point = pointsToWrite.get(0);
        }
    }

    @Test
    void verifyResultsCounts() {
        assertTrue(allLineProtocolsContain(point, "serenity_results_counts_total=99"));
        assertTrue(allLineProtocolsContain(point, "serenity_results_counts_success=91"));
        assertTrue(allLineProtocolsContain(point, "serenity_results_counts_pending=8"));
        assertTrue(allLineProtocolsContain(point, "serenity_results_counts_error=0"));
    }

    @Test
    void verifyResultsPercentages() {
        assertTrue(allLineProtocolsContain(point, "serenity_results_percentages_success=92"));
        assertTrue(allLineProtocolsContain(point, "serenity_results_percentages_pending=8"));
        assertTrue(allLineProtocolsContain(point, "serenity_results_percentages_ignored=0"));
    }

    @Test
    void verifyResultsTimings() {
        assertTrue(allLineProtocolsContain(point, "serenity_results_max_test_duration=199957"));
        assertTrue(allLineProtocolsContain(point, "serenity_results_total_clock_duration=489836"));
        assertTrue(allLineProtocolsContain(point, "serenity_results_min_test_duration=1714"));
    }

}
