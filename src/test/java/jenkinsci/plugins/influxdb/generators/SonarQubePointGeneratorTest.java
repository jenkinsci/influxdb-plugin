package jenkinsci.plugins.influxdb.generators;

import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SonarQubePointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";
    public static final String projectName = "org.namespace:feature%2Fmy-sub-project";

    private Run build;
    private TaskListener listener;
    private MeasurementRenderer<Run<?, ?>> measurementRenderer;
    private String sonarUrl = "http://sonar.dashboard.com";

    private long currTime;
    private SonarQubePointGenerator gen;
    private String metricCodeSmellJson;
    private SonarQubePointGenerator spiedGen;

    @Before
    public void before() throws IOException {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        listener = Mockito.mock(TaskListener.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);

        currTime = System.currentTimeMillis();
        gen = new SonarQubePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX);

        InputStream metricCodeSmellIS = this.getClass().getResourceAsStream("/sonarqube/metric_codeSmell.json");
        BufferedReader metricCodeSmellBufReader = new BufferedReader(new InputStreamReader(metricCodeSmellIS));
        metricCodeSmellJson = metricCodeSmellBufReader.lines().collect(Collectors.joining());

        spiedGen = Mockito.spy(gen);
    }

    @Test
    public void getSonarProjectNameFromNewSonarQube() throws Exception {
        String url = sonarUrl + "/dashboard?id=" + projectName;
        assertThat(gen.getSonarProjectName(url), is(projectName));
    }

    @Test
    public void getSonarProjectName() throws Exception {
        String url = sonarUrl + "/dashboard/index/" + projectName;
        assertThat(gen.getSonarProjectName(url), is(projectName));
    }

    @Test
    public void getSonarProjectMetric() throws Exception {
        String metric_key = "code_smells";
        String baseUrl = sonarUrl + "/api/measures/component?componentKey=" + projectName;
        String fullUrl = baseUrl + "&metricKeys=" + metric_key;

        Float expectedMetricResult = 36f;

        Mockito.doReturn(metricCodeSmellJson).when(spiedGen).getResult(fullUrl);
        assertThat(spiedGen.getSonarMetric(baseUrl, metric_key), equalTo(expectedMetricResult));
        Mockito.verify(spiedGen, Mockito.times(1)).getResult(fullUrl);
    }
}
