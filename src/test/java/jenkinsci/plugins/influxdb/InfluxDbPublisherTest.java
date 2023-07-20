package jenkinsci.plugins.influxdb;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.models.Target;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class InfluxDbPublisherTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private FilePath workspace = new FilePath(new File("."));
    @Mock
    private Run build;
    @Mock
    private Launcher launcher;
    @Mock
    private TaskListener listener;
    @Mock
    private EnvVars envVars;

    @Test
    public void testEmptyTargetShouldThrowException() {

        InfluxDbPublisher.DescriptorImpl descriptorMock = Mockito.mock(InfluxDbPublisher.DescriptorImpl.class);
        Jenkins jenkinsMock = Mockito.mock(Jenkins.class);
        Mockito.when(descriptorMock.getTargets()).thenReturn(Collections.emptyList());
        Mockito.when(jenkinsMock.getDescriptorByType(InfluxDbPublisher.DescriptorImpl.class)).thenReturn(descriptorMock);

        assertThrows("Target was null!", RuntimeException.class, () -> new InfluxDbPublisher("").perform(build, workspace, envVars, launcher, listener));
    }

    @Test
    @Issue("JENKINS-61305")
    public void testConfigRoundTripShouldPreserveSelectedTarget() throws Exception {
        InfluxDbGlobalConfig globalConfig = InfluxDbGlobalConfig.getInstance();
        Target target1 = new Target();
        target1.setDescription("Target1");
        Target target2 = new Target();
        target2.setDescription("Target2");
        globalConfig.addTarget(target1);
        globalConfig.addTarget(target2);

        InfluxDbPublisher before = new InfluxDbPublisher("Target2");
        assertEquals(before.getSelectedTarget(), "Target2");
        assertEquals(before.getTarget(), target2);
        FreeStyleProject project = j.createFreeStyleProject();
        project.getPublishersList().add(before);
        j.configRoundtrip(project);

        InfluxDbPublisher after = project.getPublishersList().get(InfluxDbPublisher.class);
        j.assertEqualBeans(before, after, "selectedTarget");
    }
}
