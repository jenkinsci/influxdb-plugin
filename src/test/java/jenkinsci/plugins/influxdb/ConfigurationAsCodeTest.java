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
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigurationAsCodeTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void should_support_jcasc_from_yaml() throws Exception {
        InfluxDbGlobalConfig globalConfig = InfluxDbGlobalConfig.getInstance();

        String yamlUrl = getClass().getResource(getClass().getSimpleName() + "/configuration-as-code.yml").toString();
        ConfigurationAsCode.get().configure(yamlUrl);

        assertEquals(globalConfig.getTargets().size(), 1);

        Target target = globalConfig.getTargets().get(0);
        assertEquals(target.getDescription(), "some description");
        assertEquals(target.getUrl(), "http://some/url");
        assertEquals(target.getUsername(), "some username");
        assertEquals(target.getPassword(), Secret.fromString("some password"));
        assertEquals(target.getDatabase(), "some_database");
        assertEquals(target.getRetentionPolicy(), "some_policy");
        assertTrue(target.isJobScheduledTimeAsPointsTimestamp());
        assertTrue(target.isExposeExceptions());
        assertTrue(target.isUsingJenkinsProxy());
        assertTrue(target.isGlobalListener());
        assertEquals(target.getGlobalListenerFilter(), "some filter");
    }

    @Test
    public void should_support_jcasc_to_yaml() throws Exception {
        InfluxDbGlobalConfig globalConfig = InfluxDbGlobalConfig.getInstance();

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
        String expectedYaml = IOUtils.toString(yamlStream, StandardCharsets.UTF_8)
                .replaceAll("\r\n?", "\n")
                .replace("unclassified:\n", "")
                .replace("some password", target.getPassword().getEncryptedValue());

        assertTrue(exportedYaml.contains(expectedYaml));
    }
}
