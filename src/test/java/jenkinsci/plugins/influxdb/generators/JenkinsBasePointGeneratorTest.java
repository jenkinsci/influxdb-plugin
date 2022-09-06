package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.write.Point;
import hudson.EnvVars;
import hudson.model.*;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

import java.io.Reader;
import java.io.StringReader;

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
        Mockito.when(build.getLogReader()).thenReturn(new StringReader(""));

        currTime = System.currentTimeMillis();
    }

    @Test
    public void agent_present() {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        Mockito.when(mockedEnvVars.get("NODE_NAME")).thenReturn("slave-1");
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        Point[] points = generator.generate();
        String lineProtocol = points[0].toLineProtocol();

        assertTrue(lineProtocol.contains("build_agent_name=\"slave-1\""));
        assertTrue(lineProtocol.contains("project_path=\"folder/master\""));
    }

    public void agent_present_in_log() throws Exception {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        Mockito.when(mockedEnvVars.get("NODE_NAME")).thenReturn("");
        Reader reader = new StringReader(JenkinsBasePointGenerator.AGENT_LOG_PATTERN + "slave-1");
        Mockito.when(build.getLogReader()).thenReturn(reader);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        Point[] points = generator.generate();
        String lineProtocol = points[0].toLineProtocol();

        assertTrue(lineProtocol.contains("build_agent_name=\"slave-1\""));
        assertTrue(lineProtocol.contains("project_path=\"folder/master\""));
    }

    @Test
    public void agent_not_present() {
        Mockito.when(build.getExecutor()).thenReturn(null);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        Point[] points = generator.generate();
        String lineProtocol = points[0].toLineProtocol();

        assertTrue(lineProtocol.contains("build_agent_name=\"\""));
        assertTrue(lineProtocol.contains("project_path=\"folder/master\""));
    }

    @Test
    public void branch_present() {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        Mockito.when(mockedEnvVars.get("BRANCH_NAME")).thenReturn("develop");
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        Point[] points = generator.generate();
        String lineProtocol = points[0].toLineProtocol();

        assertTrue(lineProtocol.contains("build_branch_name=\"develop\""));
        assertTrue(lineProtocol.contains("project_path=\"folder/master\""));
    }

    @Test
    public void brach_not_present() {
        Mockito.when(build.getExecutor()).thenReturn(null);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        Point[] points = generator.generate();
        String lineProtocol = points[0].toLineProtocol();

        assertTrue(lineProtocol.contains("build_branch_name=\"\""));
        assertTrue(lineProtocol.contains("project_path=\"folder/master\""));
    }


    @Test
    public void scheduled_and_start_and_end_time_present() {
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, MEASUREMENT_NAME, mockedEnvVars);
        Point[] generatedPoints = generator.generate();
        String lineProtocol = generatedPoints[0].toLineProtocol();

        assertTrue(lineProtocol.contains(String.format("%s=", JenkinsBasePointGenerator.BUILD_SCHEDULED_TIME)));
        assertTrue(lineProtocol.contains(String.format("%s=", JenkinsBasePointGenerator.BUILD_EXEC_TIME)));
        assertTrue(lineProtocol.contains(String.format("%s=", JenkinsBasePointGenerator.BUILD_MEASURED_TIME)));
    }

    @Test
    public void valid_jenkins_env_parameter_for_fields_present() {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, JENKINS_ENV_PARAMETER_FIELD, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        Point[] generatedPoints = jenkinsBasePointGenerator.generate();
        String lineProtocol = generatedPoints[0].toLineProtocol();

        assertTrue(lineProtocol.contains("testKey1=\"testValueField\""));
        assertTrue(lineProtocol.contains("testKey2=\"${incompleteEnvValueField\""));
        assertTrue(lineProtocol.contains("testEnvKeyField1=\"" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "\""));
        assertTrue(lineProtocol.contains("testEnvKeyField2=\"PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_SUFFIX\""));
        assertFalse(lineProtocol.contains("testValueTag"));
        assertFalse(lineProtocol.contains("${incompleteEnvValueTag"));
        assertFalse(lineProtocol.contains("testEnvKeyTag1"));
        assertFalse(lineProtocol.contains(JENKINS_ENV_RESOLVED_VALUE_TAG));
        assertFalse(lineProtocol.contains("testEnvKeyTag2"));
        assertFalse(lineProtocol.contains("PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_SUFFIX"));
    }

    @Test
    public void valid_jenkins_env_parameter_for_tags_present() {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, JENKINS_ENV_PARAMETER_TAG, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        Point[] generatedPoints = jenkinsBasePointGenerator.generate();
        String lineProtocol = generatedPoints[0].toLineProtocol();

        assertTrue(lineProtocol.contains("testKey1=testValueTag"));
        assertTrue(lineProtocol.contains("testKey2=${incompleteEnvValueTag"));
        assertTrue(lineProtocol.contains("testEnvKeyTag1=" + JENKINS_ENV_RESOLVED_VALUE_TAG));
        assertTrue(lineProtocol.contains("testEnvKeyTag2=PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_" + JENKINS_ENV_RESOLVED_VALUE_TAG +"_SUFFIX"));

        assertFalse(lineProtocol.contains("testValueField"));
        assertFalse(lineProtocol.contains("${incompleteEnvValueField"));
        assertFalse(lineProtocol.contains("testEnvKeyField1"));
        assertFalse(lineProtocol.contains(JENKINS_ENV_RESOLVED_VALUE_FIELD));
        assertFalse(lineProtocol.contains("testEnvKeyField2"));
        assertFalse(lineProtocol.contains("PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_SUFFIX"));
    }

    @Test
    public void valid_jenkins_env_parameter_for_fields_and_tags_present() {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, JENKINS_ENV_PARAMETER_TAG, JENKINS_ENV_PARAMETER_FIELD, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        Point[] generatedPoints = jenkinsBasePointGenerator.generate();
        String lineProtocol = generatedPoints[0].toLineProtocol();

        assertTrue(lineProtocol.contains("testKey1=\"testValueField\""));
        assertTrue(lineProtocol.contains("testKey2=\"${incompleteEnvValueField\""));

        assertTrue(lineProtocol.contains("testEnvKeyField1=\"" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "\""));
        assertTrue(lineProtocol.contains("testEnvKeyField2=\"PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_SUFFIX\""));

        assertTrue(lineProtocol.contains("testKey1=testValueTag"));
        assertTrue(lineProtocol.contains("testKey2=${incompleteEnvValueTag"));

        assertTrue(lineProtocol.contains("testEnvKeyTag1=" + JENKINS_ENV_RESOLVED_VALUE_TAG));
        assertTrue(lineProtocol.contains("testEnvKeyTag2=PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_SUFFIX"));
    }

    @Test
    public void custom_measurement_included() {
        String customMeasurement = "custom_measurement";
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, JENKINS_ENV_PARAMETER_TAG, JENKINS_ENV_PARAMETER_FIELD, CUSTOM_PREFIX, customMeasurement, mockedEnvVars);
        Point[] generatedPoints = jenkinsBasePointGenerator.generate();
        String lineProtocol = generatedPoints[0].toLineProtocol();

        assertTrue(lineProtocol.startsWith(customMeasurement));
    }
}
