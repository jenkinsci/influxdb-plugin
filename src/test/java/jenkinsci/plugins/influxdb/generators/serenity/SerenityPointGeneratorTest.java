package jenkinsci.plugins.influxdb.generators.serenity;

import hudson.model.TaskListener;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.dto.Point;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

import static org.junit.Assert.assertTrue;

public class SerenityPointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

    private Run build;

    private MeasurementRenderer<Run<?, ?>> measurementRenderer;

    private long currTime;
    private TaskListener listener;

    String points = null;

    @Before
    public void before() {
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
            List<Point> pointsToWrite = new ArrayList<>();
            pointsToWrite.addAll(Arrays.asList(serenityGen.generate()));
            // points.fields is private so just get all fields as a single string
            points = pointsToWrite.get(0).toString();
        }
    }

    @Test
    public void verifyResultsCounts() {
        assertTrue(points.contains("serenity_results_counts_total=99"));
        assertTrue(points.contains("serenity_results_counts_success=91"));
        assertTrue(points.contains("serenity_results_counts_pending=8"));
        assertTrue(points.contains("serenity_results_counts_error=0"));
    }

    @Test
    public void verifyResultsPercentages() {
        assertTrue(points.contains("serenity_results_percentages_success=92"));
        assertTrue(points.contains("serenity_results_percentages_pending=8"));
        assertTrue(points.contains("serenity_results_percentages_ignored=0"));
    }

    @Test
    public void verifyResultsTimings() {
        assertTrue(points.contains("serenity_results_max_test_duration=199957"));
        assertTrue(points.contains("serenity_results_total_clock_duration=489836"));
        assertTrue(points.contains("serenity_results_min_test_duration=1714"));
    }

    @Test
    public void verifyTags() {
        assertTrue(points.contains("serenity_tags_:branding=14"));
        assertTrue(points.contains("serenity_tags_context:API=73"));
        assertTrue(points.contains("serenity_tags_context:UI=26"));
    }
}
