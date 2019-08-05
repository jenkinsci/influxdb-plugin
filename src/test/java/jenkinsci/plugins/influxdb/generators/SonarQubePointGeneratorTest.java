package jenkinsci.plugins.influxdb.generators;

import hudson.model.Job;
import hudson.model.Run;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SonarQubePointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

    private Run build;

    private MeasurementRenderer<Run<?, ?>> measurementRenderer;
    private String sonarUrl = "http://sonar.dashboard.com";

    private long currTime;

    @Before
    public void before() {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);

        currTime = System.currentTimeMillis();
    }

    @Test
    public void getSonarProjectNameFromNewSonarQube() throws Exception {
        String name = "org.namespace:feature%2Fmy-sub-project";
        String url = sonarUrl + "/dashboard?id=" + name;
        SonarQubePointGenerator gen = new SonarQubePointGenerator(measurementRenderer, CUSTOM_PREFIX, build, currTime, null, true);
        assertThat(gen.getSonarProjectName(url), is(name));
    }

    @Test
    public void getSonarProjectName() throws Exception {
        String name = "org.namespace:feature%2Fmy-sub-project";
        String url = sonarUrl + "/dashboard/index/" + name;
        SonarQubePointGenerator gen = new SonarQubePointGenerator(measurementRenderer, CUSTOM_PREFIX, build, currTime, null, true);
        assertThat(gen.getSonarProjectName(url), is(name));
    }
}
