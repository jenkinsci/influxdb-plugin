package jenkinsci.plugins.influxdb.generators;

import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.influxdb.dto.Point;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

public class CustomDataPointGeneratorTest {

    public static final String JOB_NAME = "master";
    public static final int BUILD_NUMBER = 11;
    public static final String CUSTOM_PREFIX = "test_prefix";
    public static final String MEASUREMENT_NAME = "jenkins_data";

    private Run<?,?> build;
    private Job job;

    private MeasurementRenderer<Run<?, ?>> measurementRenderer;

    private long currTime;

    @Before
    public void before() {
        build = Mockito.mock(Run.class);
        job = Mockito.mock(Job.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("folder/" + JOB_NAME);

        currTime = System.currentTimeMillis();
    }

    @Test
    public void hasReportTest() {
        //check with customDataMap = null
        CustomDataPointGenerator cdGen1 = new CustomDataPointGenerator(measurementRenderer, CUSTOM_PREFIX, build, currTime, null, null, MEASUREMENT_NAME, true);
        Assert.assertFalse(cdGen1.hasReport());

        //check with empty customDataMap
        CustomDataPointGenerator cdGen2 = new CustomDataPointGenerator(measurementRenderer, CUSTOM_PREFIX, build, currTime, Collections.<String, Map<String, Object>>emptyMap(), null, MEASUREMENT_NAME, true);
        Assert.assertFalse(cdGen2.hasReport());
    }

    @Test
    public void generateTest() {
        Map<String, Object> customData = new HashMap<>();
        customData.put("test1", 11);
        customData.put("test2", 22);

        Map<String, String> customDataTags = new HashMap<>();
        customDataTags.put("tag1", "myTag");

        CustomDataPointGenerator cdGen = new CustomDataPointGenerator(measurementRenderer, CUSTOM_PREFIX, build, currTime, customData, customDataTags, MEASUREMENT_NAME, true);
        Point[] pointsToWrite = cdGen.generate();

        String lineProtocol = pointsToWrite[0].lineProtocol();
        Assert.assertTrue(lineProtocol.startsWith("jenkins_custom_data,prefix=test_prefix,project_name=test_prefix_master,tag1=myTag build_number=11i,build_time="));
        Assert.assertTrue(lineProtocol.indexOf("project_name=\"test_prefix_master\",project_path=\"folder/master\",test1=11i,test2=22i")>0);
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
        Assert.assertTrue(lineProtocol.startsWith("custom_" + customMeasurement));
    }
}
