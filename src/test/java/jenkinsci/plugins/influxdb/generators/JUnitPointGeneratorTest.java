package jenkinsci.plugins.influxdb.generators;

import hudson.EnvVars;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.test.AbstractTestResultAction;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class JUnitPointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

    private Run build;
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
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);

        currTime = System.currentTimeMillis();
    }

    @Test
    public void hasReport_tests_exist_and_flag_is_true() {
        EnvVars envVars = new EnvVars();
        envVars.put("LOG_JUNIT_RESULTS", "true");

        Mockito.when(build.getAction(AbstractTestResultAction.class)).thenReturn(Mockito.mock(AbstractTestResultAction.class));

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars);
        Assert.assertTrue(junitGen.hasReport());
    }

    @Test
    public void hasReport_tests_exist_and_flag_is_false() {
        EnvVars envVars = new EnvVars();
        envVars.put("LOG_JUNIT_RESULTS", "false");

        Mockito.when(build.getAction(AbstractTestResultAction.class)).thenReturn(Mockito.mock(AbstractTestResultAction.class));

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars);
        Assert.assertFalse(junitGen.hasReport());
    }

    @Test
    public void hasReport_tests_exist_and_flag_is_missing() {
        EnvVars envVars = new EnvVars();

        Mockito.when(build.getAction(AbstractTestResultAction.class)).thenReturn(Mockito.mock(AbstractTestResultAction.class));

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars);
        Assert.assertFalse(junitGen.hasReport());
    }

    @Test
    public void hasReport_no_tests_and_flag_is_true() {
        EnvVars envVars = new EnvVars();
        envVars.put("LOG_JUNIT_RESULTS", "true");

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars);
        Assert.assertFalse(junitGen.hasReport());
    }

    @Test
    public void hasReport_no_tests_and_flag_is_false() {
        EnvVars envVars = new EnvVars();
        envVars.put("LOG_JUNIT_RESULTS", "false");

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars);
        Assert.assertFalse(junitGen.hasReport());
    }

    @Test
    public void hasReport_no_tests_and_flag_is_missing() {
        EnvVars envVars = new EnvVars();

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars);
        Assert.assertFalse(junitGen.hasReport());
    }
}
