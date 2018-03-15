package jenkinsci.plugins.influxdb.generators;

import hudson.model.Job;
import hudson.model.Run;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.influxdb.dto.Point;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

public class CustomDataMapPointGeneratorTest {

    public static final String JOB_NAME = "master";
    public static final int BUILD_NUMBER = 11;
    public static final String CUSTOM_PREFIX = "test_prefix";

    private Run<?,?> build;
    private Job job;

    private MeasurementRenderer<Run<?, ?>> measurementRenderer;

    @Before
    public void before() {
        build = Mockito.mock(Run.class);
        job = Mockito.mock(Job.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);

    }

    @Test
    public void hasReportTest() {
        //check with customDataMap = null
        CustomDataMapPointGenerator cdmGen1 = new CustomDataMapPointGenerator(measurementRenderer, CUSTOM_PREFIX, build,
                null, null);
        Assert.assertFalse(cdmGen1.hasReport());

        //check with empty customDataMap
        CustomDataMapPointGenerator cdmGen2 = new CustomDataMapPointGenerator(measurementRenderer, CUSTOM_PREFIX, build,
                Collections.<String, Map<String, Object>>emptyMap(), Collections.<String, String>emptyMap());
        Assert.assertFalse(cdmGen2.hasReport());
    }

    @Test
    public void generateTest() {

        Map<String, String> customDataMapTags = new HashMap<>();
        customDataMapTags.put("test1Tag", "testValue");

        Map<String, Object> customData1 = new HashMap<String, Object>();
        customData1.put("test1", 11);
        customData1.put("test2", 22);


        Map<String, Object> customData2 = new HashMap<String, Object>();
        customData2.put("test3", 33);
        customData2.put("test4", 44);


        Map<String, Map<String, Object>> customDataMap = new HashMap<String, Map<String, Object>>();
        customDataMap.put("series1", customData1);
        customDataMap.put("series2", customData2);


        List<Point> pointsToWrite = new ArrayList<Point>();

        CustomDataMapPointGenerator cdmGen = new CustomDataMapPointGenerator(measurementRenderer, CUSTOM_PREFIX, build,
                customDataMap, customDataMapTags);
        pointsToWrite.addAll(Arrays.asList(cdmGen.generate()));

        String lineProtocol1;
        String lineProtocol2;
        if (pointsToWrite.get(0).lineProtocol().startsWith("series1"))  {
            lineProtocol1 = pointsToWrite.get(0).lineProtocol();
            lineProtocol2 = pointsToWrite.get(1).lineProtocol();
        } else {
            lineProtocol1 = pointsToWrite.get(1).lineProtocol();
            lineProtocol2 = pointsToWrite.get(0).lineProtocol();
        }

        Assert.assertTrue(lineProtocol1.startsWith("series1,prefix=test_prefix,project_name=test_prefix_master,test1Tag=testValue build_number=11i,project_name=\"test_prefix_master\",test1=11i,test2=22i"));
        Assert.assertTrue(lineProtocol2.startsWith("series2,prefix=test_prefix,project_name=test_prefix_master,test1Tag=testValue build_number=11i,project_name=\"test_prefix_master\",test3=33i,test4=44i"));
    }
}
