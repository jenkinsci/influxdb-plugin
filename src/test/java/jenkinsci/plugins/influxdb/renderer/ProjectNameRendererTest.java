package jenkinsci.plugins.influxdb.renderer;

import hudson.model.Job;
import hudson.model.Run;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class ProjectNameRendererTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";
    private static final String CUSTOM_PROJECT_NAME = "test_projectname";

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
    public void customProjectNameWithCustomPrefix() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, CUSTOM_PROJECT_NAME);
        String renderedProjectName = projectNameRenderer.render(build);
        assertThat(renderedProjectName, startsWith("test_prefix_test_projectname"));
    }

    @Test
    public void customProjectNameWithNullPrefix() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(null, CUSTOM_PROJECT_NAME);
        String renderedProjectName = projectNameRenderer.render(build);
        assertThat(renderedProjectName, startsWith("test_projectname"));
    }

    @Test
    public void nullProjectNameWithCustomPrefix() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);
        String renderedProjectName = projectNameRenderer.render(build);
        assertThat(renderedProjectName, startsWith("test_prefix_master"));
    }

    @Test
    public void nullProjectNameWithNullPrefix() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(null, null);
        String renderedProjectName = projectNameRenderer.render(build);
        assertThat(renderedProjectName, startsWith("master"));
    }

    @Test
    public void nullProjectNameWithNullPrefix_NoSideEffects() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(null, null);

        Mockito.when(job.getName()).thenReturn("job 1");
        String renderedProjectName1 = projectNameRenderer.render(build);
        assertThat(renderedProjectName1, startsWith("job 1"));

        Mockito.when(job.getName()).thenReturn("job 2");
        String renderedProjectName2 = projectNameRenderer.render(build);
        assertThat(renderedProjectName2, startsWith("job 2"));
    }
}
