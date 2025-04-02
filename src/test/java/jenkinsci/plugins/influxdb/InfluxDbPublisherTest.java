package jenkinsci.plugins.influxdb;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.models.Target;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WithJenkins
class InfluxDbPublisherTest {

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
    void testEmptyTargetShouldThrowException(JenkinsRule j) {

        InfluxDbPublisher.DescriptorImpl descriptorMock = Mockito.mock(InfluxDbPublisher.DescriptorImpl.class);
        Jenkins jenkinsMock = Mockito.mock(Jenkins.class);
        Mockito.when(descriptorMock.getTargets()).thenReturn(Collections.emptyList());
        Mockito.when(jenkinsMock.getDescriptorByType(InfluxDbPublisher.DescriptorImpl.class)).thenReturn(descriptorMock);

        assertThrows(RuntimeException.class, () -> new InfluxDbPublisher("").perform(build, workspace, envVars, launcher, listener), "Target was null!");
    }

    @Test
    @Issue("JENKINS-61305")
    void testConfigRoundTripShouldPreserveSelectedTarget(JenkinsRule j) throws Exception {
        InfluxDbGlobalConfig globalConfig = InfluxDbGlobalConfig.getInstance();
        Target target1 = new Target();
        target1.setDescription("Target1");
        Target target2 = new Target();
        target2.setDescription("Target2");
        globalConfig.addTarget(target1);
        globalConfig.addTarget(target2);

        InfluxDbPublisher before = new InfluxDbPublisher("Target2");
        assertEquals("Target2", before.getSelectedTarget());
        assertEquals(before.getTarget(), target2);
        FreeStyleProject project = j.createFreeStyleProject();
        project.getPublishersList().add(before);
        j.configRoundtrip(project);

        InfluxDbPublisher after = project.getPublishersList().get(InfluxDbPublisher.class);
        j.assertEqualBeans(before, after, "selectedTarget");
    }

    @Test
    void testGetTargetShouldReturnFirstTargetWithNull(JenkinsRule j) {
        InfluxDbGlobalConfig globalConfig = InfluxDbGlobalConfig.getInstance();

        Target target1 = new Target();
        target1.setDescription("Target1");
        globalConfig.addTarget(target1);

        InfluxDbPublisher publisher = new InfluxDbPublisher(null);
        assertEquals(target1, publisher.getTarget());
    }
}
