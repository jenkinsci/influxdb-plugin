package jenkinsci.plugins.influxdb.generators;

import hudson.model.*;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.influxdb.dto.Point;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * @author Damien Coraboeuf <damien.coraboeuf@gmail.com>
 */
public class JenkinsBasePointGeneratorTest {
    public static final String JOB_NAME = "master";
    public static final int BUILD_NUMBER = 11;
    public static final String CUSTOM_PREFIX = "test_prefix";

    private Run<?, ?> build;
    private MeasurementRenderer<Run<?, ?>> measurementRenderer;
    private Executor executor;

    @Before
    public void before() {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        executor = Mockito.mock(Executor.class);
        Computer computer = Mockito.mock(Computer.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getBuildStatusSummary()).thenReturn(new Run.Summary(false, "OK"));
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(executor.getOwner()).thenReturn(computer);
        Mockito.when(computer.getName()).thenReturn("slave-1");
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(job.getRelativeNameFrom(Mockito.any(Jenkins.class))).thenReturn("folder/" + JOB_NAME);
        Mockito.when(job.getBuildHealth()).thenReturn(new HealthReport());
    }

    @Test
    public void agent_present() {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(measurementRenderer, AbstractPointGenerator.CUSTOM_PREFIX, build);
        Point[] points = generator.generate();

        Assert.assertTrue(points[0].lineProtocol().contains("build_agent_name=\"slave-1\""));
        Assert.assertTrue(points[0].lineProtocol().contains("project_path=\"folder/master\""));
    }

    @Test
    public void agent_not_present() {
        Mockito.when(build.getExecutor()).thenReturn(null);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(measurementRenderer, AbstractPointGenerator.CUSTOM_PREFIX, build);
        Point[] points = generator.generate();

        Assert.assertTrue(points[0].lineProtocol().contains("build_agent_name=\"\""));
        Assert.assertTrue(points[0].lineProtocol().contains("project_path=\"folder/master\""));
    }
}
