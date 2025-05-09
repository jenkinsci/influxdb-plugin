package jenkinsci.plugins.influxdb.generators;

import hudson.EnvVars;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.test.AbstractTestResultAction;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.models.AbstractPoint;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JUnitPointGeneratorTest extends PointGeneratorBaseTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

    private Run build;
    private TaskListener listener;
    private ProjectNameRenderer measurementRenderer;

    private CaseResult caseResult;
    private long currTime;

    @BeforeEach
    void before() {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        listener = Mockito.mock(TaskListener.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);
        caseResult = Mockito.mock(CaseResult.class);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("");

        currTime = System.currentTimeMillis();
    }

    @Test
    void hasReport_tests_exist_and_flag_is_true() {
        EnvVars envVars = new EnvVars();
        envVars.put("LOG_JUNIT_RESULTS", "true");

        Mockito.when(build.getAction(AbstractTestResultAction.class)).thenReturn(Mockito.mock(AbstractTestResultAction.class));

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars);
        assertTrue(junitGen.hasReport());
    }

    @Test
    void hasReport_tests_exist_and_flag_is_false() {
        EnvVars envVars = new EnvVars();
        envVars.put("LOG_JUNIT_RESULTS", "false");

        Mockito.when(build.getAction(AbstractTestResultAction.class)).thenReturn(Mockito.mock(AbstractTestResultAction.class));

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars);
        assertFalse(junitGen.hasReport());
    }

    @Test
    void hasReport_tests_exist_and_flag_is_missing() {
        EnvVars envVars = new EnvVars();

        Mockito.when(build.getAction(AbstractTestResultAction.class)).thenReturn(Mockito.mock(AbstractTestResultAction.class));

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars);
        assertFalse(junitGen.hasReport());
    }

    @Test
    void hasReport_no_tests_and_flag_is_true() {
        EnvVars envVars = new EnvVars();
        envVars.put("LOG_JUNIT_RESULTS", "true");

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars);
        assertFalse(junitGen.hasReport());
    }

    @Test
    void hasReport_no_tests_and_flag_is_false() {
        EnvVars envVars = new EnvVars();
        envVars.put("LOG_JUNIT_RESULTS", "false");

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars);
        assertFalse(junitGen.hasReport());
    }

    @Test
    void hasReport_no_tests_and_flag_is_missing() {
        EnvVars envVars = new EnvVars();

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars);
        assertFalse(junitGen.hasReport());
    }

    @Test
    void measurement_successfully_generated() {
        CaseResult.Status status = Mockito.mock(CaseResult.Status.class);
        SuiteResult suiteResult = Mockito.mock(SuiteResult.class);
        Mockito.when(caseResult.getStatus()).thenReturn(status);
        List<CaseResult> passedTests = new ArrayList<>();
        passedTests.add(caseResult);
        AbstractTestResultAction testResultAction = Mockito.mock(AbstractTestResultAction.class);
        Mockito.when(testResultAction.getFailedTests()).thenReturn(Collections.emptyList());
        Mockito.when(testResultAction.getSkippedTests()).thenReturn(Collections.emptyList());
        Mockito.when(testResultAction.getPassedTests()).thenReturn(passedTests);
        Mockito.when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResultAction);
        Mockito.when(caseResult.getSuiteResult()).thenReturn(suiteResult);

        Mockito.when(caseResult.getSuiteResult().getName()).thenReturn("my_suite");
        Mockito.when(caseResult.getName()).thenReturn("my_test");
        Mockito.when(caseResult.getClassName()).thenReturn("my_class_name");
        Mockito.when(caseResult.getStatus().toString()).thenReturn("PASSED");
        Mockito.when(caseResult.getStatus().ordinal()).thenReturn(0);
        Mockito.when(caseResult.getDuration()).thenReturn(10.0f);

        JUnitPointGenerator generator = new JUnitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, new EnvVars());
        AbstractPoint[] points = generator.generate();

        assertTrue(allLineProtocolsContain(points[0], "suite_name=\"my_suite\""));
        assertTrue(allLineProtocolsContain(points[0], "test_name=\"my_test\""));
        assertTrue(allLineProtocolsContain(points[0], "test_class_full_name=\"my_class_name\""));
        assertTrue(allLineProtocolsContain(points[0], "test_status=\"PASSED\""));
        assertTrue(allLineProtocolsContain(points[0], "test_status_ordinal=0"));
        assertTrue(allLineProtocolsContain(points[0], "test_duration=10.0"));
    }
}
