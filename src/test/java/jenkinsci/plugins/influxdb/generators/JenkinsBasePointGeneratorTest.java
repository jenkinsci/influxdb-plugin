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
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

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
    void agent_present() throws Exception {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        Mockito.when(mockedEnvVars.get("NODE_NAME")).thenReturn(NODE_NAME);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = generator.generate();

        assertEquals("jenkins_data", points[0].getName());

        TreeMap<String, Object> fields = getPointFields(points[0]);
        TreeMap<String, Object> tags = getPointTags(points[0]);
        assertEquals(NODE_NAME, fields.get("build_agent_name"));
        assertEquals("folder/master", tags.get("project_path"));
    }

    @Test
    void agent_present_in_log() throws Exception {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        Mockito.when(mockedEnvVars.get("NODE_NAME")).thenReturn("");
        Reader reader = new StringReader(JenkinsBasePointGenerator.AGENT_LOG_PATTERN + NODE_NAME);
        Mockito.when(build.getLogReader()).thenReturn(reader);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = generator.generate();

        TreeMap<String, Object> fields = getPointFields(points[0]);
        TreeMap<String, Object> tags = getPointTags(points[0]);
        assertEquals(NODE_NAME, fields.get("build_agent_name"));
        assertEquals("folder/master", tags.get("project_path"));
    }

    @Test
    void agent_not_present() throws Exception {
        Mockito.when(build.getExecutor()).thenReturn(null);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = generator.generate();

        TreeMap<String, Object> fields = getPointFields(points[0]);
        assertEquals("", fields.get("build_agent_name"));
    }

    @Test
    void branch_present() throws Exception {
        Mockito.when(build.getExecutor()).thenReturn(executor);
        Mockito.when(mockedEnvVars.get("BRANCH_NAME")).thenReturn("develop");
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = generator.generate();

        TreeMap<String, Object> fields = getPointFields(points[0]);
        assertEquals("develop", fields.get("build_branch_name"));
    }

    @Test
    void branch_not_present() throws Exception {
        Mockito.when(build.getExecutor()).thenReturn(null);
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = generator.generate();

        TreeMap<String, Object> fields = getPointFields(points[0]);
        assertEquals("", fields.get("build_branch_name"));
    }


    @Test
    void scheduled_and_start_and_end_time_present() throws Exception {
        JenkinsBasePointGenerator generator = new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = generator.generate();

        TreeMap<String, Object> fields = getPointFields(points[0]);
        assertTrue(fields.containsKey(JenkinsBasePointGenerator.BUILD_SCHEDULED_TIME));
        assertTrue(fields.containsKey(JenkinsBasePointGenerator.BUILD_EXEC_TIME));
        assertTrue(fields.containsKey(JenkinsBasePointGenerator.BUILD_MEASURED_TIME));
    }

    @Test
    void valid_jenkins_env_parameter_for_fields_present() throws Exception {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, JENKINS_ENV_PARAMETER_FIELD, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = jenkinsBasePointGenerator.generate();

        TreeMap<String, Object> fields = getPointFields(points[0]);
        assertEquals("testValueField", fields.get("testKey1"));
        assertEquals("${incompleteEnvValueField", fields.get("testKey2"));
        assertEquals(JENKINS_ENV_RESOLVED_VALUE_FIELD, fields.get("testEnvKeyField1"));
        assertEquals("PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_SUFFIX", fields.get("testEnvKeyField2"));
        assertEquals("resolvedEnvValueField", fields.get("testEnvKeyField1"));
    }

    @Test
    void valid_jenkins_env_parameter_for_tags_present() throws Exception {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, JENKINS_ENV_PARAMETER_TAG, StringUtils.EMPTY, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = jenkinsBasePointGenerator.generate();

        TreeMap<String, Object> tags = getPointTags(points[0]);
        assertEquals("testValueTag", tags.get("testKey1"));
        assertEquals("${incompleteEnvValueTag", tags.get("testKey2"));
        assertEquals(JENKINS_ENV_RESOLVED_VALUE_TAG, tags.get("testEnvKeyTag1"));
        assertEquals("PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_SUFFIX", tags.get("testEnvKeyTag2"));
        assertFalse(tags.containsKey("testValueField"));
        assertFalse(tags.containsKey("testEnvKeyField1"));
        assertFalse(tags.containsKey("testEnvKeyField2"));
    }

    @Test
    void valid_jenkins_env_parameter_for_fields_and_tags_present() throws Exception {
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, JENKINS_ENV_PARAMETER_TAG, JENKINS_ENV_PARAMETER_FIELD, CUSTOM_PREFIX, MEASUREMENT_NAME, mockedEnvVars);
        AbstractPoint[] points = jenkinsBasePointGenerator.generate();

        TreeMap<String, Object> fields = getPointFields(points[0]);
        TreeMap<String, Object> tags = getPointTags(points[0]);

        // Check fields
        assertEquals("testValueField", fields.get("testKey1"));
        assertEquals("${incompleteEnvValueField", fields.get("testKey2"));
        assertEquals(JENKINS_ENV_RESOLVED_VALUE_FIELD, fields.get("testEnvKeyField1"));
        assertEquals("PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_" + JENKINS_ENV_RESOLVED_VALUE_FIELD + "_SUFFIX", fields.get("testEnvKeyField2"));
        // Check tags
        assertEquals("testValueTag", tags.get("testKey1"));
        assertEquals("${incompleteEnvValueTag", tags.get("testKey2"));
        assertEquals(JENKINS_ENV_RESOLVED_VALUE_TAG, tags.get("testEnvKeyTag1"));
        assertEquals("PREFIX_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_" + JENKINS_ENV_RESOLVED_VALUE_TAG + "_SUFFIX", tags.get("testEnvKeyTag2"));
    }

    @Test
    void custom_measurement_included() throws Exception {
        String customMeasurement = "custom_measurement";
        JenkinsBasePointGenerator jenkinsBasePointGenerator =
                new JenkinsBasePointGenerator(build, listener, measurementRenderer, currTime, JENKINS_ENV_PARAMETER_TAG, JENKINS_ENV_PARAMETER_FIELD, CUSTOM_PREFIX, customMeasurement, mockedEnvVars);
        AbstractPoint[] points = jenkinsBasePointGenerator.generate();
        assertTrue(allLineProtocolsStartWith(points[0], customMeasurement));
    }
}
