package jenkinsci.plugins.influxdb.models;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InfluxDBClientWrapperTest {

    @Test
    void testV2ConnectionWithDevVersion() {
        InfluxDBClient mockClient = mock(InfluxDBClient.class);
        when(mockClient.ping()).thenReturn(true);
        when(mockClient.version()).thenReturn("dev");

        StringCredentials mockToken = mock(StringCredentials.class);
        when(mockToken.getSecret()).thenReturn(Secret.fromString("test-token"));

        try (MockedStatic<InfluxDBClientFactory> mockedFactory = Mockito.mockStatic(InfluxDBClientFactory.class)) {
            mockedFactory.when(() -> InfluxDBClientFactory.create(any(InfluxDBClientOptions.class)))
                .thenReturn(mockClient);

            InfluxDBClientWrapper wrapper = new InfluxDBClientWrapper(
                "https://example.com:8086",
                "test-org",
                "test-db",
                null,
                null,
                mockToken,
                false
            );

            assertNotNull(wrapper);
            assertEquals("v2", wrapper.getConnectedApiVersion());
            assertEquals("dev", wrapper.getAPIVersion());
        }
    }

    @Test
    void testV2ConnectionWithStandardVersion() {
        InfluxDBClient mockClient = mock(InfluxDBClient.class);
        when(mockClient.ping()).thenReturn(true);
        when(mockClient.version()).thenReturn("v2.7.1");

        StringCredentials mockToken = mock(StringCredentials.class);
        when(mockToken.getSecret()).thenReturn(Secret.fromString("test-token"));

        try (MockedStatic<InfluxDBClientFactory> mockedFactory = Mockito.mockStatic(InfluxDBClientFactory.class)) {
            mockedFactory.when(() -> InfluxDBClientFactory.create(any(InfluxDBClientOptions.class)))
                .thenReturn(mockClient);

            InfluxDBClientWrapper wrapper = new InfluxDBClientWrapper(
                "https://example.com:8086",
                "test-org",
                "test-db",
                null,
                null,
                mockToken,
                false
            );

            assertNotNull(wrapper);
            assertEquals("v2", wrapper.getConnectedApiVersion());
            assertEquals("v2.7.1", wrapper.getAPIVersion());
        }
    }

    @Test
    void testV1ConnectionWithDevVersion() {
        InfluxDBClient mockClient = mock(InfluxDBClient.class);
        when(mockClient.ping()).thenReturn(true);
        when(mockClient.version()).thenReturn("dev");

        StringCredentials mockToken = mock(StringCredentials.class);
        when(mockToken.getSecret()).thenReturn(Secret.fromString("test-token"));

        try (MockedStatic<InfluxDBClientFactory> mockedFactory = Mockito.mockStatic(InfluxDBClientFactory.class)) {
            mockedFactory.when(() -> InfluxDBClientFactory.createV1(
                any(String.class),
                any(String.class),
                any(char[].class),
                any(String.class),
                any()
            )).thenReturn(mockClient);

            InfluxDBClientWrapper wrapper = new InfluxDBClientWrapper(
                "https://example.com:8086",
                null,
                "test-db",
                "autogen",
                null,
                mockToken,
                false
            );

            assertNotNull(wrapper);
            assertEquals("v1", wrapper.getConnectedApiVersion());
            assertEquals("dev", wrapper.getAPIVersion());
        }
    }
}
