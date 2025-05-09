package jenkinsci.plugins.influxdb.generators;

import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.metrics.impl.TimeInQueueAction;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.models.AbstractPoint;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsPointGeneratorTest extends PointGeneratorBaseTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

    private Run build;
    private TaskListener listener;
    private ProjectNameRenderer measurementRenderer;
    private TimeInQueueAction timeInQueueAction;

    private long currTime;

    @BeforeEach
    void before() {
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
    void measurement_successfully_generated() {
        Mockito.when(timeInQueueAction.getBlockedDurationMillis()).thenReturn((long) 10);
        Mockito.when(timeInQueueAction.getBuildableDurationMillis()).thenReturn((long) 20);
        Mockito.when(timeInQueueAction.getBuildingDurationMillis()).thenReturn((long) 30);
        Mockito.when(timeInQueueAction.getExecutingTimeMillis()).thenReturn((long) 40);
        Mockito.when(timeInQueueAction.getExecutorUtilization()).thenReturn(0.5);
        Mockito.when(timeInQueueAction.getQueuingDurationMillis()).thenReturn((long) 50);
        Mockito.when(timeInQueueAction.getSubTaskCount()).thenReturn(2);
        Mockito.when(timeInQueueAction.getTotalDurationMillis()).thenReturn((long) 60);
        Mockito.when(timeInQueueAction.getWaitingDurationMillis()).thenReturn((long) 70);

        MetricsPointGenerator generator = new MetricsPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX);
        AbstractPoint[] points = generator.generate();

        assertTrue(allLineProtocolsContain(points[0], "blocked_time=10"));
        assertTrue(allLineProtocolsContain(points[0], "buildable_time=20"));
        assertTrue(allLineProtocolsContain(points[0], "building_time=30"));
        assertTrue(allLineProtocolsContain(points[0], "executing_time=40"));
        assertTrue(allLineProtocolsContain(points[0], "executor_utilization=0.5"));
        assertTrue(allLineProtocolsContain(points[0], "queue_time=50"));
        assertTrue(allLineProtocolsContain(points[0], "subtask_count=2"));
        assertTrue(allLineProtocolsContain(points[0], "total_duration=60"));
        assertTrue(allLineProtocolsContain(points[0], "waiting_time=70"));
    }

}
