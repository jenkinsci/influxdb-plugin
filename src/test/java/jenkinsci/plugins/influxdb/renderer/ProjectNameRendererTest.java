package jenkinsci.plugins.influxdb.renderer;

import hudson.model.Job;
import hudson.model.Run;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectNameRendererTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";
    private static final String CUSTOM_PROJECT_NAME = "test_projectname";

    private Run<?,?> build;
    private Job job;

    @BeforeEach
    void before() {
        build = Mockito.mock(Run.class);
        job = Mockito.mock(Job.class);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.doReturn(job).when(build).getParent();
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
    }

    @Test
    void customProjectNameWithCustomPrefix() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, CUSTOM_PROJECT_NAME);
        String renderedProjectName = projectNameRenderer.render(build);
        assertTrue(renderedProjectName.startsWith(CUSTOM_PREFIX + "_" + CUSTOM_PROJECT_NAME));
    }

    @Test
    void customProjectNameWithNullPrefix() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(null, CUSTOM_PROJECT_NAME);
        String renderedProjectName = projectNameRenderer.render(build);
        assertTrue(renderedProjectName.startsWith(CUSTOM_PROJECT_NAME));
    }

    @Test
    void nullProjectNameWithCustomPrefix() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);
        String renderedProjectName = projectNameRenderer.render(build);
        assertTrue(renderedProjectName.startsWith(CUSTOM_PREFIX + "_" + JOB_NAME));
    }

    @Test
    void nullProjectNameWithNullPrefix() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(null, null);
        String renderedProjectName = projectNameRenderer.render(build);
        assertTrue(renderedProjectName.startsWith(JOB_NAME));
    }

    @Test
    void nullProjectNameWithNullPrefix_NoSideEffects() {
        ProjectNameRenderer projectNameRenderer = new ProjectNameRenderer(null, null);

        Mockito.when(job.getName()).thenReturn("job 1");
        String renderedProjectName1 = projectNameRenderer.render(build);
        assertTrue(renderedProjectName1.startsWith("job 1"));

        Mockito.when(job.getName()).thenReturn("job 2");
        String renderedProjectName2 = projectNameRenderer.render(build);
        assertTrue(renderedProjectName2.startsWith("job 2"));
    }
}
