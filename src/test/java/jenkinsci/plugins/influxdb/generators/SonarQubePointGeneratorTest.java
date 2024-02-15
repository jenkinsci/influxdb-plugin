package jenkinsci.plugins.influxdb.generators;

import hudson.EnvVars;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.ClassRule;
import org.mockito.Mockito;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class SonarQubePointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

            
    private Run build;
    private TaskListener listener;
    private ProjectNameRenderer measurementRenderer;
    private String sonarUrl = "http://sonar.dashboard.com";

    private long currTime;

    // temp file system settings for testing find report file 
    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();
    
    private static final String[] mvnReportPath = {"maven-basic", "target", "sonar"};
    private static final String defaultReportName = "report-task.txt";
    private static final String[] customReportPath = {"custom", "report", "path"};
    private static final String customReportName  = "custom-report.txt";

    private File resourceDirectory;

    @BeforeClass
    public static void beforeClass() {
        
        // create a dummy file system with the scanner report file
        // temp-dir/
        //  - maven-basic/target/sonar/report-task.txt
        //  - custom/report/path/custom-report.txt
        //  - temp{1..5}/
        //      - temfiles...
        String wsRoot = folder.getRoot().toString();
        Path wsSonarDir = Paths.get(wsRoot, mvnReportPath);
        Path sonarReport = wsSonarDir.resolve(defaultReportName); 
        Path customSonarReportDir = Paths.get(wsRoot, customReportPath);
        Path sonarCustomReport = customSonarReportDir.resolve(customReportName); 

        try {
            Files.createDirectories(wsSonarDir);  
            Files.createFile(sonarReport);
            Files.createDirectories(customSonarReportDir); 
            Files.createFile(sonarCustomReport);
            
            // create temp dirs and files
            for (int i = 0; i < 5; i++) {
                String tmpDirName = "temp" + i;
                Path tmpPath = Paths.get(wsRoot, tmpDirName);
                Files.createDirectories(tmpPath);
                for (int j = 0; j < 10; j++) {
                    Files.createTempFile(tmpPath, null, ".class");
                }
            }
        }catch(Exception e) {
            System.err.println("[InfluxDB Plugin Test] ERROR: Failed to create a temp file system - " + e.getMessage());
        }
    }

    @Before
    public void before() {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        listener = Mockito.mock(TaskListener.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);

        try {
            resourceDirectory = new File(SonarQubePointGeneratorTest.class.getResource(".").toURI());
        } catch (Exception ignored) {
            resourceDirectory = null;
        }

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);

        currTime = System.currentTimeMillis();
    }

    @Test
    public void getSonarProjectMetric() throws Exception {
        EnvVars envVars = new EnvVars();
        String name = "org.namespace:feature%2Fmy-sub-project";
        String metric_key = "code_smells";
        String metric_value = "59";
        String responseJson = "{\"component\":{\"id\":\"AWZS_ynA7tIj5HosrIjz\",\"key\":\"" + name + "\",\"name\":\"Fake Statistics\",\"qualifier\":\"TRK\",\"measures\":[{\"metric\":\"" + metric_key + "\",\"value\":\"" + metric_value +"\",\"bestValue\":false}]}}";
        String url = sonarUrl + "/api/measures/component?componentKey=" + name + "&metricKeys=" + metric_key;
        SonarQubePointGenerator gen = Mockito.spy(new SonarQubePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars));

        Mockito.doReturn(responseJson).when(gen).getResult(any(String.class));
        assertEquals(java.util.Optional.of(gen.getSonarMetric(url, metric_key)), java.util.Optional.of(Float.parseFloat(metric_value)));
    }

    @Test
    public void getSonarProjectMetric_NoMetric() throws Exception {
        EnvVars envVars = new EnvVars();
        String name = "org.namespace:feature%2Fmy-sub-project";
        String metric_key = "branch_coverage";
        String responseJson = "{\"component\":{\"id\":\"AWZS_ynA7tIj5HosrIjz\",\"key\":\"" + name + "\",\"name\":\"Fake Statistics\",\"qualifier\":\"TRK\",\"measures\":[]}}";
        String url = sonarUrl + "/api/measures/component?componentKey=" + name + "&metricKeys=" + metric_key;
        SonarQubePointGenerator gen = Mockito.spy(new SonarQubePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, CUSTOM_PREFIX, envVars));

        Mockito.doReturn(responseJson).when(gen).getResult(any(String.class));
        assertNull(gen.getSonarMetric(url, metric_key));
    }

   
    @Test
    public void getSonarDefaultReportFilePath() {
        // Find default sonar report-task.txt file 
        EnvVars envVars = new EnvVars();

        SonarQubePointGenerator gen = Mockito.spy(
                                    new SonarQubePointGenerator(build, 
                                                                listener, 
                                                                measurementRenderer, 
                                                                currTime, 
                                                                StringUtils.EMPTY, 
                                                                CUSTOM_PREFIX, 
                                                                envVars));

        String wsRoot = folder.getRoot().toString();
        Path wsSonarDir = Paths.get(wsRoot, mvnReportPath);
        Path sonarReport = wsSonarDir.resolve(defaultReportName); 

        try {
            List<Path> result = gen.findReportByFileName(wsRoot);    
            assertEquals(1, result.size());

            Files.deleteIfExists(sonarReport);
            result = gen.findReportByFileName(wsRoot);
            assertEquals(0, result.size());
        } catch(Exception e){
            System.err.println("[InfluxDB Plugin Test] ERROR: Failed to find default report file - " + e.getMessage());
        }
    }

    @Test
    public void getSonarCustomReportFileName() {
        // Find a custom report file defined by SONARQUBE_BUILD_REPORT_NAME env var 
        // Example: custom-report.txt
        EnvVars envVars = Mockito.spy(new EnvVars());

        SonarQubePointGenerator gen = Mockito.spy(
                                    new SonarQubePointGenerator(build, 
                                                                listener, 
                                                                measurementRenderer, 
                                                                currTime, 
                                                                StringUtils.EMPTY, 
                                                                CUSTOM_PREFIX, 
                                                                envVars));

        String wsRoot = folder.getRoot().toString();

        try {
            
            Mockito.doReturn(customReportName)
                .when(envVars)
                .get(any(String.class));

            List<Path> result = gen.findReportByFileName(wsRoot);    
            assertEquals(1, result.size());

        } catch(Exception e){
            System.err.println("[InfluxDB Plugin Test] ERROR: Failed to find custom report file - " + e.getMessage());
        }
    }

    @Test
    public void getSonarCustomReportFilePath() {
        // Find a custom report path defined by SONARQUBE_BUILD_REPORT_NAME env var
        // Example: path/custom-report.txt
        EnvVars envVars = Mockito.spy(new EnvVars());

        SonarQubePointGenerator gen = Mockito.spy(
                                    new SonarQubePointGenerator(build, 
                                                                listener, 
                                                                measurementRenderer, 
                                                                currTime, 
                                                                StringUtils.EMPTY, 
                                                                CUSTOM_PREFIX, 
                                                                envVars));

        String wsRoot = folder.getRoot().toString();

        try {
            
            String parentCustomDir = customReportPath[customReportPath.length - 1];
            Path customReportPathPattern = Paths.get(parentCustomDir,
                                                    customReportName);
                                                    
            Mockito.doReturn(customReportPathPattern.toString())
                .when(envVars)
                .get(any(String.class));

            List<Path>  result = gen.findReportByFileName(wsRoot);    
            assertEquals(1, result.size());

        } catch(Exception e){
            System.err.println("[InfluxDB Plugin Test] ERROR: Failed to find custom report file path - " + e.getMessage());
        }
    }

    @Test
    public void hasReportFindsCorrectInformationFromReportFile() {
        EnvVars envVars = new EnvVars();
        envVars.put("SONARQUBE_BUILD_REPORT_NAME", "report-task.txt");
        envVars.put("WORKSPACE", resourceDirectory.getAbsolutePath());

        SonarQubePointGenerator generator = new SonarQubePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, envVars);
        boolean hasReport = generator.hasReport();

        String id = "123EXAMPLE";
        String url = "http://sonarqube:9000";
        assertTrue(hasReport);
        assertEquals("InfluxDBPlugin",generator.getProjectKey());
        assertEquals(url,generator.getSonarBuildURL());
        assertEquals(id,generator.getSonarBuildTaskId());
        assertEquals(url + "/api/ce/task?id=" + id,generator.getSonarBuildTaskIdUrl());
    }

    @Test
    public void hasReportFindsCorrectInformationFromBuildLogs() throws Exception {
        File directory = new File(resourceDirectory, "sonarqube");
        File file = new File(directory, "build-log.txt");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        Mockito.when(build.getLogReader()).thenReturn(reader);
        EnvVars envVars = new EnvVars();
        envVars.put("WORKSPACE", "mikki hiiri");

        SonarQubePointGenerator generator = new SonarQubePointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY, envVars);
        boolean hasReport = generator.hasReport();

        String id = "321EXAMPLE";
        String url = "http://sonarqube:9001";
        assertTrue(hasReport);
        assertEquals("InfluxDBPlugin-log",generator.getProjectKey());
        assertEquals(url,generator.getSonarBuildURL());
        assertEquals(id,generator.getSonarBuildTaskId());
        assertEquals(url + "/api/ce/task?id=" + id,generator.getSonarBuildTaskIdUrl());
    }
}
