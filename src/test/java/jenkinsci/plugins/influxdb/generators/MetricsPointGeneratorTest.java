package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.write.Point;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.metrics.impl.TimeInQueueAction;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;

public class MetricsPointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

    private Run build;
    private TaskListener listener;
    private ProjectNameRenderer measurementRenderer;
    private TimeInQueueAction timeInQueueAction;

    private long currTime;

    @Before
    public void before() {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        listener = Mockito.mock(TaskListener.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);
        timeInQueueAction = Mockito.mock(TimeInQueueAction.class);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(build.getAction(TimeInQueueAction.class)).thenReturn(timeInQueueAction);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("folder/" + JOB_NAME);

        currTime = System.currentTimeMillis();
    }

    @Test
    public void measurement_successfully_generated() {
        Mockito.when(timeInQueueAction.getBlockedDurationMillis()).thenReturn((long)10);
        Mockito.when(timeInQueueAction.getBuildableDurationMillis()).thenReturn((long)20);
        Mockito.when(timeInQueueAction.getBuildingDurationMillis()).thenReturn((long)30);
        Mockito.when(timeInQueueAction.getExecutingTimeMillis()).thenReturn((long)40);
        Mockito.when(timeInQueueAction.getExecutorUtilization()).thenReturn(0.5);
        Mockito.when(timeInQueueAction.getQueuingDurationMillis()).thenReturn((long)50);
        Mockito.when(timeInQueueAction.getSubTaskCount()).thenReturn(2);
        Mockito.when(timeInQueueAction.getTotalDurationMillis()).thenReturn((long)60);
        Mockito.when(timeInQueueAction.getWaitingDurationMillis()).thenReturn((long)70);

        MetricsPointGenerator generator = new MetricsPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX);
        Point[] points = generator.generate();
        String lineProtocol = points[0].toLineProtocol();

        assertTrue(lineProtocol.contains("blocked_time=10"));
        assertTrue(lineProtocol.contains("buildable_time=20"));
        assertTrue(lineProtocol.contains("building_time=30"));
        assertTrue(lineProtocol.contains("executing_time=40"));
        assertTrue(lineProtocol.contains("executor_utilization=0.5"));
        assertTrue(lineProtocol.contains("queue_time=50"));
        assertTrue(lineProtocol.contains("subtask_count=2"));
        assertTrue(lineProtocol.contains("total_duration=60"));
        assertTrue(lineProtocol.contains("waiting_time=70"));
    }

}
