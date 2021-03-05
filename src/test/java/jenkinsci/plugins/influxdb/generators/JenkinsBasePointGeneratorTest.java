package jenkinsci.plugins.influxdb.generators;

import hudson.EnvVars;
import hudson.model.*;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.dto.Point;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * @author Damien Coraboeuf <damien.coraboeuf@gmail.com>
 */
public class JenkinsBasePointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

    private static final String JENKINS_ENV_PARAMETER_FIELD =
                    "testKey1=testValueField\n" +
                    "testKey2=${incompleteEnvValueField\n" +
                    "testEnvKeyField1=${testEnvValueField}\n" +
                    "testEnvKeyField2=PREFIX_${testEnvValueField}_${testEnvValueField}_SUFFIX";
    private static final String JENKINS_ENV_VALUE_FIELD = "testEnvValueField";
    private static final String JENKINS_ENV_RESOLVED_VALUE_FIELD = "resolvedEnvValueField";

    private static final String JENKINS_ENV_PARAMETER_TAG =
                    "testKey1=testValueTag\n" +
                    "testKey2=${incompleteEnvValueTag\n" +
                    "testEnvKeyTag1=${testEnvValueTag}\n" +
                    "testEnvKeyTag2=PREFIX_${testEnvValueTag}_${testEnvValueTag}_SUFFIX";
    private static final String JENKINS_ENV_VALUE_TAG = "testEnvValueTag";
    private static final String JENKINS_ENV_RESOLVED_VALUE_TAG = "resolvedEnvValueTag";
    private static final String MEASUREMENT_NAME = "jenkins_data";

    private Run<?, ?> build;
    private ProjectNameRenderer measurementRenderer;
    private Executor executor;
    private TaskListener listener;
    private long currTime;
    private EnvVars mockedEnvVars;

    @Before
    public void before() throws Exception {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        executor = Mockito.mock(Executor.class);
        listener = Mockito.mock(TaskListener.class);
        mockedEnvVars = Mockito.mock(EnvVars.class);
        Computer computer = Mockito.mock(Computer.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getBuildStatusSummary()).thenReturn(new Run.Summary(false, "OK"));
        Mockito.doReturn(job).when(build).getParent();
        Mockito.when(build.getEnvironment(listener)).thenReturn(mockedEnvVars);
        Mockito.when(executor.getOwner()).thenReturn(computer);
        Mockito.when(computer.getName()).thenReturn("slave-1");
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("folder/" + JOB_NAME);
        Mockito.when(job.getBuildHealth()).thenReturn(new HealthReport());
        Mockito.when(mockedEnvVars.get(JENKINS_ENV_VALUE_FIELD)).thenReturn(JENKINS_ENV_RESOLVED_VALUE_FIELD);
        Mockito.when(mockedEnvVars.get(JENKINS_ENV_VALUE_TAG)).thenReturn(JENKINS_ENV_RESOLVED_VALUE_TAG);
        Mockito.when(mockedEnvVars.get("NODE_NAME")).thenReturn(null);
        Mockito.when(mockedEnvVars.get("BRANCH_NAME")).thenReturn(null);

        currTime = System.currentTimeMillis();
    }

    @Test
    public void agent_present() {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        Mockito.when(mockedEnvVars.get("NODE_NAME")).thenReturn("slave-1");
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        Point[] points = generator.generate();
        String lineProtocol = points[0].lineProtocol();

        assertThat(lineProtocol, containsString("build_agent_name=\"slave-1\""));
        assertThat(lineProtocol, containsString("project_path=\"folder/master\""));
    }

    @Test
    public void agent_not_present() {
        Mockito.when(build.getExecutor()).thenReturn(null);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        Point[] points = generator.generate();
        String lineProtocol = points[0].lineProtocol();

        assertThat(lineProtocol, containsString("build_agent_name=\"\""));
        assertThat(lineProtocol, containsString("project_path=\"folder/master\""));
    }

    @Test
    public void branch_present() {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        Mockito.when(mockedEnvVars.get("BRANCH_NAME")).thenReturn("develop");
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        Point[] points = generator.generate();
        String lineProtocol = points[0].lineProtocol();

        assertThat(lineProtocol, containsString("build_branch_name=\"develop\""));
        assertThat(lineProtocol, containsString("project_path=\"folder/master\""));
    }

    @Test
    public void brach_not_present() {
        Mockito.when(build.getExecutor()).thenReturn(null);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        Point[] points = generator.generate();
        String lineProtocol = points[0].lineProtocol();

        assertThat(lineProtocol, containsString("build_branch_name=\"\""));
        assertThat(lineProtocol, containsString("project_path=\"folder/master\""));
    }


    @Test
    public void scheduled_and_start_and_end_time_present() {
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, MEASUREMENT_NAME, mockedEnvVars);
        Point[] generatedPoints = generator.generate();
        String lineProtocol = generatedPoints[0].lineProtocol();

        assertThat(lineProtocol, containsString(String.format("%s=", JenkinsBasePointGenerator.BUILD_SCHEDULED_TIME)));
        assertThat(lineProtocol, containsString(String.format("%s=", JenkinsBasePointGenerator.BUILD_EXEC_TIME)));
        assertThat(lineProtocol, containsString(String.format("%s=", JenkinsBasePointGenerator.BUILD_MEASURED_TIME)));
    }

    @Test
    public void valid_jenkins_env_parameter_for_fields_present() {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, JENKINS_ENV_PARAMETER_FIELD, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
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
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, JENKINS_ENV_PARAMETER_TAG, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
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
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, JENKINS_ENV_PARAMETER_TAG, JENKINS_ENV_PARAMETER_FIELD, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
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

    @Test
    public void custom_measurement_included() {
        String customMeasurement = "custom_measurement";
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, JENKINS_ENV_PARAMETER_TAG, JENKINS_ENV_PARAMETER_FIELD, CUSTOM_PREFIX, customMeasurement, mockedEnvVars);
        Point[] generatedPoints = jenkinsBasePointGenerator.generate();
        String lineProtocol = generatedPoints[0].lineProtocol();

        assertThat(lineProtocol, startsWith(customMeasurement));
    }
}
