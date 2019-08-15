package jenkinsci.plugins.influxdb;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkinsci.plugins.influxdb.models.Target;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

@RunWith(PowerMockRunner.class)
@PrepareForTest(InfluxDbPublisher.class)
public class InfluxDbPublisherTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private FilePath workspace = new FilePath(new File("."));
    @Mock
    private Run build;
    @Mock
    private Launcher launcher;
    @Mock
    private TaskListener listener;

    @Test
    public void emptyTarget() throws Exception {
        exception.expect(RuntimeException.class);
        exception.expectMessage("Target was null!");

        DescriptorImpl descriptorMock = Mockito.mock(DescriptorImpl.class);
        Mockito.when(descriptorMock.getTargets()).thenReturn(new Target[0]);
        PowerMockito.whenNew(DescriptorImpl.class).withNoArguments().thenReturn(descriptorMock);

        try {
            new InfluxDbPublisher("").perform(build, workspace, launcher, listener);
        } catch (NullPointerException e) {
            Assert.fail("NullPointerException raised");
        }
    }
}
