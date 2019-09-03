package jenkinsci.plugins.influxdb.generators;

import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.influxdb.dto.Point;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class CustomDataPointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";
    private static final String MEASUREMENT_NAME = "jenkins_data";

    private Run<?,?> build;

    private MeasurementRenderer<Run<?, ?>> measurementRenderer;

    private long currTime;

    @Before
    public void before() {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("folder/" + JOB_NAME);

        currTime = System.currentTimeMillis();
    }

    @Test
    public void hasReport() {
        //check with customDataMap = null
        CustomDataPointGenerator cdGen1 = new CustomDataPointGenerator(measurementRenderer, CUSTOM_PREFIX, build, currTime, null, null, MEASUREMENT_NAME, true);
        assertThat(cdGen1.hasReport(), is(false));

        //check with empty customDataMap
        CustomDataPointGenerator cdGen2 = new CustomDataPointGenerator(measurementRenderer, CUSTOM_PREFIX, build, currTime, Collections.<String, Map<String, Object>>emptyMap(), null, MEASUREMENT_NAME, true);
        assertThat(cdGen2.hasReport(), is(false));
    }

    @Test
    public void generate() {
        Map<String, Object> customData = new HashMap<>();
        customData.put("test1", 11);
        customData.put("test2", 22);

        Map<String, String> customDataTags = new HashMap<>();
        customDataTags.put("tag1", "myTag");

        CustomDataPointGenerator cdGen = new CustomDataPointGenerator(measurementRenderer, CUSTOM_PREFIX, build, currTime, customData, customDataTags, MEASUREMENT_NAME, true);
        Point[] pointsToWrite = cdGen.generate();

        String lineProtocol = pointsToWrite[0].lineProtocol();
        assertThat(lineProtocol, startsWith("jenkins_custom_data,prefix=test_prefix,project_name=test_prefix_master,project_path=folder/master,tag1=myTag build_number=11i,build_time="));
        assertThat(lineProtocol, containsString("project_name=\"test_prefix_master\",project_path=\"folder/master\",test1=11i,test2=22i"));
    }

    @Test
    public void custom_measurement_included() {
        String customMeasurement = "custom_measurement";
        Map<String, Object> customData = new HashMap<>();
        customData.put("test1", 11);

        Map<String, String> customDataTags = new HashMap<>();
        customDataTags.put("tag1", "myTag");

        CustomDataPointGenerator cdGen = new CustomDataPointGenerator(measurementRenderer, CUSTOM_PREFIX, build, currTime, customData, customDataTags, customMeasurement, true);
        Point[] pointsToWrite = cdGen.generate();

        String lineProtocol = pointsToWrite[0].lineProtocol();
        assertThat(lineProtocol, startsWith("custom_" + customMeasurement));
    }
}
