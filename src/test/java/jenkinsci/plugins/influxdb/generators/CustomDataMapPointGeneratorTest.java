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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class CustomDataMapPointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

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
        CustomDataMapPointGenerator cdmGen1 = new CustomDataMapPointGenerator(measurementRenderer, CUSTOM_PREFIX, build,
                currTime, null, null);
        assertThat(cdmGen1.hasReport(), is(false));

        //check with empty customDataMap
        CustomDataMapPointGenerator cdmGen2 = new CustomDataMapPointGenerator(measurementRenderer, CUSTOM_PREFIX, build,
                currTime, Collections.emptyMap(), Collections.emptyMap());
        assertThat(cdmGen2.hasReport(), is(false));
    }

    @Test
    public void generate() {
        Map<String, Object> customData1 = new HashMap<>();
        customData1.put("test1", 11);
        customData1.put("test2", 22);

        Map<String, Object> customData2 = new HashMap<>();
        customData2.put("test3", 33);
        customData2.put("test4", 44);

        Map<String, Map<String, Object>> customDataMap = new HashMap<>();
        customDataMap.put("series1", customData1);
        customDataMap.put("series2", customData2);

        Map<String, Map<String, String>> customDataMapTags = new HashMap<>();
        Map<String, String> customTags = new HashMap<>();
        customTags.put("build_result", "SUCCESS");
        customDataMapTags.put("series1", customTags);

        CustomDataMapPointGenerator cdmGen = new CustomDataMapPointGenerator(measurementRenderer, CUSTOM_PREFIX, build,
                currTime, customDataMap, customDataMapTags);
        Point[] pointsToWrite = cdmGen.generate();

        String lineProtocol1;
        String lineProtocol2;
        if (pointsToWrite[0].lineProtocol().startsWith("series1")) {
            lineProtocol1 = pointsToWrite[0].lineProtocol();
            lineProtocol2 = pointsToWrite[1].lineProtocol();
        } else {
            lineProtocol1 = pointsToWrite[1].lineProtocol();
            lineProtocol2 = pointsToWrite[0].lineProtocol();
        }
        assertThat(lineProtocol1, startsWith("series1,build_result=SUCCESS,prefix=test_prefix,project_name=test_prefix_master,project_path=folder/master build_number=11i,project_name=\"test_prefix_master\",project_path=\"folder/master\",test1=11i,test2=22i"));
        assertThat(lineProtocol2, startsWith("series2,prefix=test_prefix,project_name=test_prefix_master,project_path=folder/master build_number=11i,project_name=\"test_prefix_master\",project_path=\"folder/master\",test3=33i,test4=44i"));
    }
}
