package jenkinsci.plugins.influxdb.generators;

import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.models.AbstractPoint;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomDataPointGeneratorTest extends PointGeneratorBaseTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";
    private static final String MEASUREMENT_NAME = "jenkins_data";

    private Run<?, ?> build;
    private TaskListener listener;

    private ProjectNameRenderer measurementRenderer;

    private long currTime;

    @BeforeEach
    void before() {
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
    void hasReport() {
        //check with customDataMap = null
        CustomDataPointGenerator cdGen1 = new CustomDataPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, null, null, MEASUREMENT_NAME);
        assertFalse(cdGen1.hasReport());

        //check with empty customDataMap
        CustomDataPointGenerator cdGen2 = new CustomDataPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, Collections.emptyMap(), null, MEASUREMENT_NAME);
        assertFalse(cdGen2.hasReport());
    }

    @Test
    void generate() {
        Map<String, Object> customData = new HashMap<>();
        customData.put("test1", 11);
        customData.put("test2", 22);

        Map<String, String> customDataTags = new HashMap<>();
        customDataTags.put("tag1", "myTag");
        CustomDataPointGenerator cdGen = new CustomDataPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, customData, customDataTags, MEASUREMENT_NAME);
        AbstractPoint[] points = cdGen.generate();

        assertTrue(allLineProtocolsStartWith(points[0], "jenkins_custom_data,prefix=test_prefix,project_name=test_prefix_master,project_namespace=folder,project_path=folder/master,tag1=myTag build_number=11i,build_time="));
        assertTrue(allLineProtocolsContain(points[0], "jenkins_custom_data,prefix=test_prefix,project_name=test_prefix_master,project_namespace=folder,project_path=folder/master,tag1=myTag build_number=11i"));
    }

    @Test
    void custom_measurement_included() {
        String customMeasurement = "custom_measurement";
        Map<String, Object> customData = new HashMap<>();
        customData.put("test1", 11);

        Map<String, String> customDataTags = new HashMap<>();
        customDataTags.put("tag1", "myTag");

        CustomDataPointGenerator cdGen = new CustomDataPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, customData, customDataTags, customMeasurement);
        AbstractPoint[] pointsToWrite = cdGen.generate();

        assertTrue(allLineProtocolsStartWith(pointsToWrite[0], "custom_" + customMeasurement));
    }
}
