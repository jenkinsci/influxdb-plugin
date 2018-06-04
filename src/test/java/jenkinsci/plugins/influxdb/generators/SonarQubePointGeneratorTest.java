package jenkinsci.plugins.influxdb.generators;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import hudson.model.Job;
import hudson.model.Run;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

public class SonarQubePointGeneratorTest {

	  public static final String JOB_NAME = "master";
	    public static final int BUILD_NUMBER = 11;
	    public static final String CUSTOM_PREFIX = "test_prefix";

	    private Run build;
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
	public void getSonarProjectNameTest() throws URISyntaxException {
		String name = "org.namespace:feature%2Fmy-sub-project";
		String url = "http://sonar.dashboard.com/dashboard/index/" + name;
		SonarQubePointGenerator gen = new SonarQubePointGenerator(measurementRenderer, CUSTOM_PREFIX, build, null);
		assertEquals(name, gen.getSonarProjectName(url));
	}
}
