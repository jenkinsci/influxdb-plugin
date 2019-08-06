package jenkinsci.plugins.influxdb;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import jenkinsci.plugins.influxdb.models.Target;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import hudson.util.Secret;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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
        assertThat(target.getDescription(), is("some description"));
        assertThat(target.getUrl(), is("http://some/url"));
        assertThat(target.getUsername(), is("some username"));
        assertThat(target.getPassword(), is(Secret.fromString("some password")));
        assertThat(target.getDatabase(), is("some_database"));
        assertThat(target.getRetentionPolicy(), is("some_policy"));
        assertThat(target.isJobScheduledTimeAsPointsTimestamp(), is(true));
        assertThat(target.isExposeExceptions(), is(true));
        assertThat(target.isUsingJenkinsProxy(), is(true));
        assertThat(target.isGlobalListener(), is(true));
        assertThat(target.getGlobalListenerFilter(), is("some filter"));
    }

    @Test
    public void should_support_jcasc_to_yaml() throws Exception {
        DescriptorImpl globalConfig = j.jenkins.getDescriptorByType(DescriptorImpl.class);

        Target target = new Target();
        target.setDescription("some description");
        target.setUrl("http://some/url");
        target.setUsername("some username");
        target.setPassword("some password");
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
