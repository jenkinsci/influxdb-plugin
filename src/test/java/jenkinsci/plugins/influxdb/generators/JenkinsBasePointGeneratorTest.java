package jenkinsci.plugins.influxdb.generators;

import hudson.EnvVars;
import hudson.model.*;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.models.AbstractPoint;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.Reader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Damien Coraboeuf <damien.coraboeuf@gmail.com>
 */
class JenkinsBasePointGeneratorTest extends PointGeneratorBaseTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

    private static final String JENKINS_ENV_PARAMETER_FIELD =
            """
                    testKey1=testValueField
                    testKey2=${incompleteEnvValueField
                    testEnvKeyField1=${testEnvValueField}
                    testEnvKeyField2=PREFIX_${testEnvValueField}_${testEnvValueField}_SUFFIX""";
    private static final String JENKINS_ENV_VALUE_FIELD = "testEnvValueField";
    private static final String JENKINS_ENV_RESOLVED_VALUE_FIELD = "resolvedEnvValueField";

    private static final String JENKINS_ENV_PARAMETER_TAG =
            """
                    testKey1=testValueTag
                    testKey2=${incompleteEnvValueTag
                    testEnvKeyTag1=${testEnvValueTag}
                    testEnvKeyTag2=PREFIX_${testEnvValueTag}_${testEnvValueTag}_SUFFIX""";
    private static final String JENKINS_ENV_VALUE_TAG = "testEnvValueTag";
    private static final String JENKINS_ENV_RESOLVED_VALUE_TAG = "resolvedEnvValueTag";
    private static final String MEASUREMENT_NAME = "jenkins_data";

    private static final String PROJECT_PATH = "project_path=\"folder/master\"";
    private static final String NODE_NAME = "slave-1";

    private Run<?, ?> build;
    private ProjectNameRenderer measurementRenderer;
    private Executor executor;
    private TaskListener listener;
    private long currTime;
    private EnvVars mockedEnvVars;

    @BeforeEach
    void before() throws Exception {
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
        Mockito.when(computer.getName()).thenReturn(NODE_NAME);
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
    void agent_present() {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        Mockito.when(mockedEnvVars.get("NODE_NAME")).thenReturn(NODE_NAME);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = generator.generate();
        assertTrue(allLineProtocolsContain(points[0], "build_agent_name=\"" + NODE_NAME + "\""));
        assertTrue(allLineProtocolsContain(points[0], PROJECT_PATH));
    }

    @Test
    void agent_present_in_log() throws Exception {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        Mockito.when(mockedEnvVars.get("NODE_NAME")).thenReturn("");
        Reader reader = new StringReader(JenkinsBasePointGenerator.AGENT_LOG_PATTERN + NODE_NAME);
        Mockito.when(build.getLogReader()).thenReturn(reader);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = generator.generate();

        assertTrue(allLineProtocolsContain(points[0], "build_agent_name=\"" + NODE_NAME + "\""));
        assertTrue(allLineProtocolsContain(points[0], PROJECT_PATH));
    }

    @Test
    void agent_not_present() {
        Mockito.when(build.getExecutor()).thenReturn(null);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = generator.generate();

        assertTrue(allLineProtocolsContain(points[0], "build_agent_name=\"\""));
        assertTrue(allLineProtocolsContain(points[0], PROJECT_PATH));
    }

    @Test
    void branch_present() {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        Mockito.when(mockedEnvVars.get("BRANCH_NAME")).thenReturn("develop");
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = generator.generate();

        assertTrue(allLineProtocolsContain(points[0], "build_branch_name=\"develop\""));
        assertTrue(allLineProtocolsContain(points[0], PROJECT_PATH));
    }

    @Test
    void branch_not_present() {
        Mockito.when(build.getExecutor()).thenReturn(null);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = generator.generate();

        assertTrue(allLineProtocolsContain(points[0], "build_branch_name=\"\""));
        assertTrue(allLineProtocolsContain(points[0], PROJECT_PATH));
    }


    @Test
    void scheduled_and_start_and_end_time_present() {
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = generator.generate();

        assertTrue(allLineProtocolsContain(points[0], String.format("%s=", JenkinsBasePointGenerator.BUILD_SCHEDULED_TIME)));
        assertTrue(allLineProtocolsContain(points[0], String.format("%s=", JenkinsBasePointGenerator.BUILD_EXEC_TIME)));
        assertTrue(allLineProtocolsContain(points[0], String.format("%s=", JenkinsBasePointGenerator.BUILD_MEASURED_TIME)));
    }

    @Test
    void valid_jenkins_env_parameter_for_fields_present() {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, JENKINS_ENV_PARAMETER_FIELD, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = jenkinsBasePointGenerator.generate();

        assertTrue(allLineProtocolsContain(points[0], "testKey1=\"testValueField\""));
        assertTrue(allLineProtocolsContain(points[0], "testKey2=\"${incompleteEnvValueField\""));
        assertTrue(allLineProtocolsContain(points[0], "testEnvKeyField1=\"" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "\""));
        assertTrue(allLineProtocolsContain(points[0], "testEnvKeyField2=\"PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_SUFFIX\""));
        assertFalse(allLineProtocolsContain(points[0], "testValueTag"));
        assertFalse(allLineProtocolsContain(points[0], "${incompleteEnvValueTag"));
        assertFalse(allLineProtocolsContain(points[0], "testEnvKeyTag1"));
        assertFalse(allLineProtocolsContain(points[0], JENKINS_ENV_RESOLVED_VALUE_TAG));
        assertFalse(allLineProtocolsContain(points[0], "testEnvKeyTag2"));
        assertFalse(allLineProtocolsContain(points[0], "PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_SUFFIX"));
    }

    @Test
    void valid_jenkins_env_parameter_for_tags_present() {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, JENKINS_ENV_PARAMETER_TAG, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = jenkinsBasePointGenerator.generate();

        assertTrue(allLineProtocolsContain(points[0], "testKey1=testValueTag"));
        assertTrue(allLineProtocolsContain(points[0], "testKey2=${incompleteEnvValueTag"));
        assertTrue(allLineProtocolsContain(points[0], "testEnvKeyTag1=" + JENKINS_ENV_RESOLVED_VALUE_TAG));
        assertTrue(allLineProtocolsContain(points[0], "testEnvKeyTag2=PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_SUFFIX"));
        assertFalse(allLineProtocolsContain(points[0], "testValueField"));
        assertFalse(allLineProtocolsContain(points[0], "${incompleteEnvValueField"));
        assertFalse(allLineProtocolsContain(points[0], "testEnvKeyField1"));
        assertFalse(allLineProtocolsContain(points[0], JENKINS_ENV_RESOLVED_VALUE_FIELD));
        assertFalse(allLineProtocolsContain(points[0], "testEnvKeyField2"));
        assertFalse(allLineProtocolsContain(points[0], "PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_SUFFIX"));
    }

    @Test
    void valid_jenkins_env_parameter_for_fields_and_tags_present() {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, JENKINS_ENV_PARAMETER_TAG, JENKINS_ENV_PARAMETER_FIELD, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = jenkinsBasePointGenerator.generate();

        assertTrue(allLineProtocolsContain(points[0], "testKey1=\"testValueField\""));
        assertTrue(allLineProtocolsContain(points[0], "testKey2=\"${incompleteEnvValueField\""));

        assertTrue(allLineProtocolsContain(points[0], "testEnvKeyField1=\"" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "\""));
        assertTrue(allLineProtocolsContain(points[0], "testEnvKeyField2=\"PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_SUFFIX\""));

        assertTrue(allLineProtocolsContain(points[0], "testKey1=testValueTag"));
        assertTrue(allLineProtocolsContain(points[0], "testKey2=${incompleteEnvValueTag"));

        assertTrue(allLineProtocolsContain(points[0], "testEnvKeyTag1=" + JENKINS_ENV_RESOLVED_VALUE_TAG));
        assertTrue(allLineProtocolsContain(points[0], "testEnvKeyTag2=PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_SUFFIX"));
    }

    @Test
    void custom_measurement_included() {
        String customMeasurement = "custom_measurement";
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, JENKINS_ENV_PARAMETER_TAG, JENKINS_ENV_PARAMETER_FIELD, CUSTOM_PREFIX, customMeasurement, mockedEnvVars);
        AbstractPoint[] points = jenkinsBasePointGenerator.generate();
        assertTrue(allLineProtocolsStartWith(points[0], customMeasurement));
    }
}
