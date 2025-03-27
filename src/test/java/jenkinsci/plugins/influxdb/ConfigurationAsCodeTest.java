package jenkinsci.plugins.influxdb;

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

import static org.junit.Assert.*;

public class ConfigurationAsCodeTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void should_support_jcasc_from_yaml() throws Exception {
        InfluxDbGlobalConfig globalConfig = InfluxDbGlobalConfig.getInstance();

        String yamlUrl = getClass().getResource(getClass().getSimpleName() + "/configuration-as-code.yml").toString();
        ConfigurationAsCode.get().configure(yamlUrl);

        assertEquals(1, globalConfig.getTargets().size());

        Target target = globalConfig.getTargets().get(0);
        assertEquals("some description", target.getDescription());
        assertEquals("http://some/url", target.getUrl());

        assertEquals("some_id", target.getCredentialsId());
        assertEquals("some_database", target.getDatabase());
        assertEquals("some_policy", target.getRetentionPolicy());
        assertTrue(target.isJobScheduledTimeAsPointsTimestamp());
        assertTrue(target.isExposeExceptions());
        assertTrue(target.isUsingJenkinsProxy());
        assertTrue(target.isGlobalListener());
        assertEquals("some filter", target.getGlobalListenerFilter());
        assertEquals("some_organization", target.getOrganization());
    }

    @Test
    public void should_support_jcasc_to_yaml() throws Exception {
        InfluxDbGlobalConfig globalConfig = InfluxDbGlobalConfig.getInstance();

        Target target = new Target();
        target.setDescription("some description");
        target.setUrl("http://some/url");
        target.setCredentialsId("some_id");
        target.setDatabase("some_database");
        target.setRetentionPolicy("some_policy");
        target.setJobScheduledTimeAsPointsTimestamp(true);
        target.setExposeExceptions(true);
        target.setUsingJenkinsProxy(true);
        target.setGlobalListener(true);
        target.setGlobalListenerFilter("some filter");
        target.setOrganization("some_organization");

        globalConfig.setTargets(Collections.singletonList(target));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(outputStream);
        String exportedYaml = outputStream.toString(StandardCharsets.UTF_8);

        InputStream yamlStream = getClass().getResourceAsStream(getClass().getSimpleName() + "/configuration-as-code.yml");
        String expectedYaml = IOUtils.toString(yamlStream, StandardCharsets.UTF_8)
                .replaceAll("\r\n?", "\n")
                .replace("unclassified:\n", "");

        assertTrue(exportedYaml.contains(expectedYaml));
    }
}
