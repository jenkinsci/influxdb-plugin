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

public class CustomDataMapPointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

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

        CustomDataMapPointGenerator cdmGen1 = new CustomDataMapPointGenerator(build, listener, measurementRenderer,
                currTime, StringUtils.EMPTY, CUSTOM_PREFIX, null, null);
        assertFalse(cdmGen1.hasReport());

        //check with empty customDataMap
        CustomDataMapPointGenerator cdmGen2 = new CustomDataMapPointGenerator(build, listener, measurementRenderer,
                currTime, StringUtils.EMPTY, CUSTOM_PREFIX, Collections.emptyMap(), Collections.emptyMap());
        assertFalse(cdmGen2.hasReport());
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

        CustomDataMapPointGenerator cdmGen = new CustomDataMapPointGenerator(build, listener, measurementRenderer,
                currTime, StringUtils.EMPTY, CUSTOM_PREFIX, customDataMap, customDataMapTags);
        Point[] pointsToWrite = cdmGen.generate();

        String lineProtocol1;
        String lineProtocol2;
        if (pointsToWrite[0].toLineProtocol().startsWith("series1")) {
            lineProtocol1 = pointsToWrite[0].toLineProtocol();
            lineProtocol2 = pointsToWrite[1].toLineProtocol();
        } else {
            lineProtocol1 = pointsToWrite[1].toLineProtocol();
            lineProtocol2 = pointsToWrite[0].toLineProtocol();
        }
        assertTrue(lineProtocol1.startsWith("series1,build_result=SUCCESS,prefix=test_prefix,project_name=test_prefix_master,project_namespace=folder,project_path=folder/master build_number=11i,project_name=\"test_prefix_master\",project_path=\"folder/master\",test1=11i,test2=22i"));
        assertTrue(lineProtocol2.startsWith("series2,prefix=test_prefix,project_name=test_prefix_master,project_namespace=folder,project_path=folder/master build_number=11i,project_name=\"test_prefix_master\",project_path=\"folder/master\",test3=33i,test4=44i"));
    }
}
