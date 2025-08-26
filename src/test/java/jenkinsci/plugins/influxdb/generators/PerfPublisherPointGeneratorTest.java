package jenkinsci.plugins.influxdb.generators;

import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.PerfPublisher.PerfPublisherBuildAction;
import hudson.plugins.PerfPublisher.Report.Metric;
import hudson.plugins.PerfPublisher.Report.Report;
import hudson.plugins.PerfPublisher.Report.ReportContainer;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.models.AbstractPoint;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Eugene Schava <eschava@gmail.com>
 */
class PerfPublisherPointGeneratorTest extends PointGeneratorBaseTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

    private Run<?, ?> build;
    private TaskListener listener;
    private ProjectNameRenderer measurementRenderer;
    private ReportContainer reports;

    private long currTime;

    @BeforeEach
    void before() {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        listener = Mockito.mock(TaskListener.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);
        PerfPublisherBuildAction buildAction = Mockito.mock(PerfPublisherBuildAction.class);
        reports = new ReportContainer();

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.doReturn(job).when(build).getParent();
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("folder/" + JOB_NAME);
        Mockito.when(build.getAction(PerfPublisherBuildAction.class)).thenReturn(buildAction);

        Mockito.when(buildAction.getReport()).thenAnswer((Answer<Report>) invocationOnMock -> reports.getReports().isEmpty() ? null : reports.getReports().get(0));
        Mockito.when(buildAction.getReports()).thenReturn(reports);

        currTime = System.currentTimeMillis();
    }

    @Test
    void hasReport() {
        PerfPublisherPointGenerator generator = new PerfPublisherPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX);
        assertFalse(generator.hasReport());

        reports.addReport(new Report());
        generator = new PerfPublisherPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX);
        assertTrue(generator.hasReport());
    }

    @Test
    void generate() throws Exception {
        Report report = new Report();

        hudson.plugins.PerfPublisher.Report.Test test = new hudson.plugins.PerfPublisher.Report.Test();
        test.setName("test.txt");
        test.setExecuted(true);

        Map<String, Metric> metrics = new HashMap<>();
        Metric metric1 = new Metric();
        metric1.setMeasure(50);
        metric1.setRelevant(true);
        metric1.setUnit("ms");
        metrics.put("metric1", metric1);
        test.setMetrics(metrics);

        report.addTest(test);
        reports.addReport(report);
        PerfPublisherPointGenerator generator = new PerfPublisherPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX);
        AbstractPoint[] points = generator.generate();

        // Check point/table names
        assertEquals("perfpublisher_summary", points[0].getName());
        assertEquals("perfpublisher_metric", points[1].getName());
        assertEquals("perfpublisher_test", points[2].getName());
        assertEquals("perfpublisher_test_metric", points[3].getName());

        TreeMap<String, Object> p1Fields = getPointFields(points[0]);
        assertEquals(1L, p1Fields.get("number_of_executed_tests"));

        TreeMap<String, Object> p2Fields = getPointFields(points[1]);
        assertEquals(50.0, p2Fields.get("average"));
        assertEquals(50.0, p2Fields.get("best"));
        assertEquals(50.0, p2Fields.get("worst"));
        assertEquals("metric1", p2Fields.get("metric_name"));
        assertEquals(50.0, p2Fields.get("average"));

        TreeMap<String, Object> p3Fields = getPointFields(points[2]);
        assertEquals("test.txt", p3Fields.get("test_name"));
        assertEquals(true, p3Fields.get("executed"));

        TreeMap<String, Object> p4Fields = getPointFields(points[3]);
        assertEquals("test.txt", p4Fields.get("test_name"));
        assertEquals(true, p4Fields.get("relevant"));
        assertEquals("ms", p4Fields.get("unit"));
        assertEquals(50.0, p4Fields.get("value"));
    }
}
