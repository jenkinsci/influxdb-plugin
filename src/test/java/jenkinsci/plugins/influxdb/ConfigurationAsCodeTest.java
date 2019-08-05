package jenkinsci.plugins.influxdb;

import hudson.util.Secret;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import jenkinsci.plugins.influxdb.models.Target;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ConfigurationAsCodeTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void should_support_jcasc_from_yaml() throws Exception {
        DescriptorImpl globalConfig = j.jenkins.getDescriptorByType(DescriptorImpl.class);

        String yamlUrl = getClass().getResource(getClass().getSimpleName() + "/configuration-as-code.yml").toString();
        ConfigurationAsCode.get().configure(yamlUrl);

        assertThat(globalConfig.getTargets(), arrayWithSize(1));

        Target target = globalConfig.getTargets()[0];
        assertThat(target.getDescription(), equalTo("some description"));
        assertThat(target.getUrl(), equalTo("http://some/url"));
        assertThat(target.getUsername(), equalTo("some username"));
        assertThat(target.getPassword(), equalTo(Secret.fromString("some password")));
        assertThat(target.getDatabase(), equalTo("some_database"));
        assertThat(target.getRetentionPolicy(), equalTo("some_policy"));
        assertThat(target.isJobScheduledTimeAsPointsTimestamp(), equalTo(true));
        assertThat(target.isExposeExceptions(), equalTo(true));
        assertThat(target.isUsingJenkinsProxy(), equalTo(true));
        assertThat(target.isGlobalListener(), equalTo(true));
        assertThat(target.getGlobalListenerFilter(), equalTo("some filter"));
    }

    @Test
    public void should_support_jcasc_to_yaml() throws Exception {
        DescriptorImpl globalConfig = j.jenkins.getDescriptorByType(DescriptorImpl.class);

        Target target = new Target();
        target.setDescription("some description");
        target.setUrl("http://some/url");
        target.setUsername("some username");
        target.setPassword(Secret.fromString("some password"));
        target.setDatabase("some_database");
        target.setRetentionPolicy("some_policy");
        target.setJobScheduledTimeAsPointsTimestamp(true);
        target.setExposeExceptions(true);
        target.setUsingJenkinsProxy(true);
        target.setGlobalListener(true);
        target.setGlobalListenerFilter("some filter");

        globalConfig.setTargets(Collections.singletonList(target));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(outputStream);
        String exportedYaml = outputStream.toString("UTF-8");

        InputStream yamlStream = getClass().getResourceAsStream(getClass().getSimpleName() + "/configuration-as-code.yml");
        String expectedYaml = IOUtils.toString(yamlStream, "UTF-8")
                .replaceAll("\r\n?", "\n")
                .replace("unclassified:\n", "")
                .replace("some password", target.getPassword().getEncryptedValue());

        assertThat(exportedYaml, containsString(expectedYaml));
    }
}
