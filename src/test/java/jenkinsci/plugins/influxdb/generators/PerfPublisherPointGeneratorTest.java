package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.write.Point;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.PerfPublisher.PerfPublisherBuildAction;
import hudson.plugins.PerfPublisher.Report.Metric;
import hudson.plugins.PerfPublisher.Report.Report;
import hudson.plugins.PerfPublisher.Report.ReportContainer;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Eugene Schava <eschava@gmail.com>
 */
public class PerfPublisherPointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

    private Run<?,?> build;
    private TaskListener listener;
    private ProjectNameRenderer measurementRenderer;
    private ReportContainer reports;

    private long currTime;

    @Before
    public void before() {
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

        Mockito.when(buildAction.getReport()).thenAnswer(new Answer<Report>() {
            @Override
            public Report answer(InvocationOnMock invocationOnMock) {
                return reports.getReports().isEmpty() ? null : reports.getReports().get(0);
            }
        });
        Mockito.when(buildAction.getReports()).thenReturn(reports);

        currTime = System.currentTimeMillis();
    }

    @Test
    public void hasReport() {
        PerfPublisherPointGenerator generator = new PerfPublisherPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX);
        assertFalse(generator.hasReport());

        reports.addReport(new Report());
        generator = new PerfPublisherPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX);
        assertTrue(generator.hasReport());
    }

    @Test
    public void generate() {
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
        Point[] points = generator.generate();

        assertTrue(points[0].toLineProtocol().startsWith("perfpublisher_summary,prefix=test_prefix,project_name=test_prefix_master,project_namespace=folder,project_path=folder/master build_number=11i,number_of_executed_tests=1i"));
        assertTrue(points[1].toLineProtocol().startsWith("perfpublisher_metric,prefix=test_prefix,project_name=test_prefix_master,project_namespace=folder,project_path=folder/master average=50.0,best=50.0,build_number=11i,metric_name=\"metric1\",project_name=\"test_prefix_master\",project_path=\"folder/master\",worst=50.0"));
        assertTrue(points[2].toLineProtocol().startsWith("perfpublisher_test,prefix=test_prefix,project_name=test_prefix_master,project_namespace=folder,project_path=folder/master,test_name=test.txt build_number=11i,executed=true,project_name=\"test_prefix_master\",project_path=\"folder/master\",successful=false,test_name=\"test.txt\""));
        assertTrue(points[3].toLineProtocol().startsWith("perfpublisher_test_metric,prefix=test_prefix,project_name=test_prefix_master,project_namespace=folder,project_path=folder/master,test_name=test.txt build_number=11i,metric_name=\"metric1\",project_name=\"test_prefix_master\",project_path=\"folder/master\",relevant=true,test_name=\"test.txt\",unit=\"ms\",value=50.0"));
    }
}
