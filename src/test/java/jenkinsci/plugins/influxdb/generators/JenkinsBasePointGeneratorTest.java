package jenkinsci.plugins.influxdb.generators;

import hudson.EnvVars;
import hudson.model.*;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.influxdb.dto.Point;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Damien Coraboeuf <damien.coraboeuf@gmail.com>
 */
public class JenkinsBasePointGeneratorTest {
    public static final String JOB_NAME = "master";
    public static final int BUILD_NUMBER = 11;
    public static final String CUSTOM_PREFIX = "test_prefix";

    public static final String JENKINS_ENV_PARAMETER_FIELD =
                    "testKey1=testValueField\n" +
                    "testKey2=${incompleteEnvValueField\n" +
                    "testEnvKeyField1=${testEnvValueField}\n" +
                    "testEnvKeyField2=PREFIX_${testEnvValueField}_${testEnvValueField}_SUFFIX";
    private static final String JENKINS_ENV_VALUE_FIELD = "testEnvValueField";
    private static final String JENKINS_ENV_RESOLVED_VALUE_FIELD = "resolvedEnvValueField";

    public static final String JENKINS_ENV_PARAMETER_TAG =
                    "testKey1=testValueTag\n" +
                    "testKey2=${incompleteEnvValueTag\n" +
                    "testEnvKeyTag1=${testEnvValueTag}\n" +
                    "testEnvKeyTag2=PREFIX_${testEnvValueTag}_${testEnvValueTag}_SUFFIX";
    private static final String JENKINS_ENV_VALUE_TAG = "testEnvValueTag";
    private static final String JENKINS_ENV_RESOLVED_VALUE_TAG = "resolvedEnvValueTag";
    public static final String MEASUREMENT_NAME = "jenkins_data";

    private Run<?, ?> build;
    private MeasurementRenderer<Run<?, ?>> measurementRenderer;
    private Executor executor;
    private TaskListener listener;
    private EnvVars mockedEnvVars;

    @Before
    public void before() throws IOException, InterruptedException {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        executor = Mockito.mock(Executor.class);
        listener = Mockito.mock(TaskListener.class);
        mockedEnvVars = Mockito.mock(EnvVars.class);
        Computer computer = Mockito.mock(Computer.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getBuildStatusSummary()).thenReturn(new Run.Summary(false, "OK"));
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(build.getEnvironment(listener)).thenReturn(mockedEnvVars);
        Mockito.when(executor.getOwner()).thenReturn(computer);
        Mockito.when(computer.getName()).thenReturn("slave-1");
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(job.getRelativeNameFrom(Mockito.any(Jenkins.class))).thenReturn("folder/" + JOB_NAME);
        Mockito.when(job.getBuildHealth()).thenReturn(new HealthReport());
        Mockito.when(mockedEnvVars.get(JENKINS_ENV_VALUE_FIELD)).thenReturn(JENKINS_ENV_RESOLVED_VALUE_FIELD);
        Mockito.when(mockedEnvVars.get(JENKINS_ENV_VALUE_TAG)).thenReturn(JENKINS_ENV_RESOLVED_VALUE_TAG);
    }

    @Test
    public void agent_present() {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(measurementRenderer, AbstractPointGenerator.CUSTOM_PREFIX, build, listener, StringUtils.EMPTY, StringUtils.EMPTY, MEASUREMENT_NAME);
        Point[] points = generator.generate();

        Assert.assertTrue(points[0].lineProtocol().contains("build_agent_name=\"slave-1\""));
        Assert.assertTrue(points[0].lineProtocol().contains("project_path=\"folder/master\""));
    }

    @Test
    public void agent_not_present() {
        Mockito.when(build.getExecutor()).thenReturn(null);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(measurementRenderer, AbstractPointGenerator.CUSTOM_PREFIX, build, listener, StringUtils.EMPTY, StringUtils.EMPTY, MEASUREMENT_NAME);
        Point[] points = generator.generate();

        Assert.assertTrue(points[0].lineProtocol().contains("build_agent_name=\"\""));
        Assert.assertTrue(points[0].lineProtocol().contains("project_path=\"folder/master\""));
    }

    @Test
    public void sheduled_and_start_and_end_time_present() {
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(measurementRenderer, StringUtils.EMPTY, build, listener, StringUtils.EMPTY, StringUtils.EMPTY, MEASUREMENT_NAME);
        Point[] generatedPoints = generator.generate();

        Assert.assertThat(generatedPoints[0].lineProtocol(), Matchers.containsString(String.format("%s=", JenkinsBasePointGenerator.BUILD_SCHEDULED_TIME)));
        Assert.assertThat(generatedPoints[0].lineProtocol(), Matchers.containsString(String.format("%s=", JenkinsBasePointGenerator.BUILD_EXEC_TIME)));
        Assert.assertThat(generatedPoints[0].lineProtocol(), Matchers.containsString(String.format("%s=", JenkinsBasePointGenerator.BUILD_MEASURED_TIME)));
    }


    @Test
    public void valid_jenkins_env_parameter_for_fields_present() {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(measurementRenderer, CUSTOM_PREFIX, build, listener, JENKINS_ENV_PARAMETER_FIELD, StringUtils.EMPTY, MEASUREMENT_NAME);
        Point[] generatedPoints = jenkinsBasePointGenerator.generate();
        String lineProtocol = generatedPoints[0].lineProtocol();

        assertThat(lineProtocol, containsString("testKey1=\"testValueField\""));
        assertThat(lineProtocol, containsString("testKey2=\"${incompleteEnvValueField\""));
        assertThat(lineProtocol, containsString("testEnvKeyField1=\"" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "\""));
        assertThat(lineProtocol, containsString("testEnvKeyField2=\"PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_SUFFIX\""));

        assertThat(lineProtocol, not(containsString("testValueTag")));
        assertThat(lineProtocol, not(containsString("${incompleteEnvValueTag")));
        assertThat(lineProtocol, not(containsString("testEnvKeyTag1")));
        assertThat(lineProtocol, not(containsString(JENKINS_ENV_RESOLVED_VALUE_TAG)));
        assertThat(lineProtocol, not(containsString("testEnvKeyTag2")));
        assertThat(lineProtocol, not(containsString("PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_SUFFIX")));
    }

    @Test
    public void valid_jenkins_env_parameter_for_tags_present() {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(measurementRenderer, CUSTOM_PREFIX, build, listener, StringUtils.EMPTY, JENKINS_ENV_PARAMETER_TAG, MEASUREMENT_NAME);
        Point[] generatedPoints = jenkinsBasePointGenerator.generate();
        String lineProtocol = generatedPoints[0].lineProtocol();

        assertThat(lineProtocol, containsString("testKey1=testValueTag"));
        assertThat(lineProtocol, containsString("testKey2=${incompleteEnvValueTag"));
        assertThat(lineProtocol, containsString("testEnvKeyTag1=" + JENKINS_ENV_RESOLVED_VALUE_TAG));
        assertThat(lineProtocol, containsString("testEnvKeyTag2=PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_" + JENKINS_ENV_RESOLVED_VALUE_TAG +"_SUFFIX"));

        assertThat(lineProtocol, not(containsString("testValueField")));
        assertThat(lineProtocol, not(containsString("${incompleteEnvValueField")));
        assertThat(lineProtocol, not(containsString("testEnvKeyField1")));
        assertThat(lineProtocol, not(containsString(JENKINS_ENV_RESOLVED_VALUE_FIELD)));
        assertThat(lineProtocol, not(containsString("testEnvKeyField2")));
        assertThat(lineProtocol, not(containsString("PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_SUFFIX")));
    }

    @Test
    public void valid_jenkins_env_parameter_for_fields_and_tags_present() {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(measurementRenderer, CUSTOM_PREFIX, build, listener, JENKINS_ENV_PARAMETER_FIELD, JENKINS_ENV_PARAMETER_TAG, MEASUREMENT_NAME);
        Point[] generatedPoints = jenkinsBasePointGenerator.generate();
        String lineProtocol = generatedPoints[0].lineProtocol();

        assertThat(lineProtocol, containsString("testKey1=\"testValueField\""));
        assertThat(lineProtocol, containsString("testKey2=\"${incompleteEnvValueField\""));

        assertThat(lineProtocol, containsString("testEnvKeyField1=\"" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "\""));
        assertThat(lineProtocol, containsString("testEnvKeyField2=\"PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_SUFFIX\""));

        assertThat(lineProtocol, containsString("testKey1=testValueTag"));
        assertThat(lineProtocol, containsString("testKey2=${incompleteEnvValueTag"));

        assertThat(lineProtocol, containsString("testEnvKeyTag1=" + JENKINS_ENV_RESOLVED_VALUE_TAG));
        assertThat(lineProtocol, containsString("testEnvKeyTag2=PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_SUFFIX"));
    }
}
