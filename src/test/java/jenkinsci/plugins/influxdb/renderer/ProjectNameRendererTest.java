package jenkinsci.plugins.influxdb.renderer;

import hudson.model.Job;
import hudson.model.Run;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ProjectNameRendererTest {

    public static final String JOB_NAME = "master";
    public static final int BUILD_NUMBER = 11;
    public static final String CUSTOM_PREFIX = "test_prefix";
    public static final String CUSTOM_PROJECT_NAME = "test_projectname";

    private Run<?,?> build;
    private Job job;


    @Before
    public void before() {
        build = Mockito.mock(Run.class);
        job = Mockito.mock(Job.class);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);

    }

    @Test
    public void customProjectNameWithCustomPrefixTest() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, CUSTOM_PROJECT_NAME);
        String renderedProjectName = projectNameRenderer.render(build);
        Assert.assertTrue(renderedProjectName.startsWith("test_prefix_test_projectname"));
    }

    @Test
    public void customProjectNameWithNullPrefixTest() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(null, CUSTOM_PROJECT_NAME);
        String renderedProjectName = projectNameRenderer.render(build);
        Assert.assertTrue(renderedProjectName.startsWith("test_projectname"));
    }

    @Test
    public void nullProjectNameWithCustomPrefixTest() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);
        String renderedProjectName = projectNameRenderer.render(build);
        Assert.assertTrue(renderedProjectName.startsWith("test_prefix_master"));
    }

    @Test
    public void nullProjectNameWithNullPrefixTest() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(null, null);
        String renderedProjectName = projectNameRenderer.render(build);
        Assert.assertTrue(renderedProjectName.startsWith("master"));
    }
}
