package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.write.Point;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CustomDataPointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";
    private static final String MEASUREMENT_NAME = "jenkins_data";

    private Run<?,?> build;
    private TaskListener listener;

    private ProjectNameRenderer measurementRenderer;

    private long currTime;

    @Before
    public void before() {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        listener = Mockito.mock(TaskListener.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.doReturn(job).when(build).getParent();
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("folder/" + JOB_NAME);

        currTime = System.currentTimeMillis();
    }

    @Test
    public void hasReport() {
        //check with customDataMap = null
        CustomDataPointGenerator cdGen1 = new CustomDataPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, null, null, MEASUREMENT_NAME);
        assertFalse(cdGen1.hasReport());

        //check with empty customDataMap
        CustomDataPointGenerator cdGen2 = new CustomDataPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, Collections.emptyMap(), null, MEASUREMENT_NAME);
        assertFalse(cdGen2.hasReport());
    }

    @Test
    public void generate() {
        Map<String, Object> customData = new HashMap<>();
        customData.put("test1", 11);
        customData.put("test2", 22);

        Map<String, String> customDataTags = new HashMap<>();
        customDataTags.put("tag1", "myTag");
        CustomDataPointGenerator cdGen = new CustomDataPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, customData, customDataTags, MEASUREMENT_NAME);
        Point[] pointsToWrite = cdGen.generate();

        String lineProtocol = pointsToWrite[0].toLineProtocol();
        assertTrue(lineProtocol.startsWith("jenkins_custom_data,prefix=test_prefix,project_name=test_prefix_master,project_namespace=folder,project_path=folder/master,tag1=myTag build_number=11i,build_time="));
        assertTrue(lineProtocol.contains("jenkins_custom_data,prefix=test_prefix,project_name=test_prefix_master,project_namespace=folder,project_path=folder/master,tag1=myTag build_number=11i"));
    }

    @Test
    public void custom_measurement_included() {
        String customMeasurement = "custom_measurement";
        Map<String, Object> customData = new HashMap<>();
        customData.put("test1", 11);

        Map<String, String> customDataTags = new HashMap<>();
        customDataTags.put("tag1", "myTag");

        CustomDataPointGenerator cdGen = new CustomDataPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, customData, customDataTags, customMeasurement);
        Point[] pointsToWrite = cdGen.generate();

        String lineProtocol = pointsToWrite[0].toLineProtocol();
        assertTrue(lineProtocol.startsWith("custom_" + customMeasurement));
    }
}
