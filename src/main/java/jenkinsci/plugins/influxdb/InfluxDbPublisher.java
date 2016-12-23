package jenkinsci.plugins.influxdb;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import jenkinsci.plugins.influxdb.generators.*;
import jenkinsci.plugins.influxdb.models.Target;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InfluxDbPublisher extends Notifier implements SimpleBuildStep{

    /** The logger. **/
    private static final Logger logger = Logger.getLogger(InfluxDbPublisher.class.getName());

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private String selectedTarget;

    /**
     * custom prefix, for example in multi branch pipelines, where every build is named
     * after the branch built and thus you have different builds called 'master' that report
     * different metrics.
     */
    private String customPrefix;


    /**
     * custom data, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB
     * This can easily be done by calling
     *
     *   def myDataMap = [:]
     *   myDataMap['myKey'] = 'myValue'
     *   step([$class: 'InfluxDbPublisher', target: myTarget, customPrefix: 'myPrefix', customData: myDataMap])
     *
     * inside a pipeline script
     */
    private Map<String, Object> customData;

    /**
     * custom data maps, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB.
     *
     * This goes beyond customData since it allows to define multiple customData measurements
     * where the name of the measurement is defined as the key of the customDataMap.
     *
     * Example for a pipeline script:
     *
     *   def myDataMap1 = [:]
     *   def myDataMap2 = [:]
     *   def myCustomDataMap = [:]
     *   myDataMap1["myMap1Key1"] = 11 //first value of first map
     *   myDataMap1["myMap1Key2"] = 12 //second value of first map
     *   myDataMap2["myMap2Key1"] = 21 //first value of second map
     *   myDataMap2["myMap2Key2"] = 22 //second value of second map
     *   myCustomDataMap["series1"] = myDataMap1
     *   myCustomDataMap["series2"] = myDataMap2
     *   step([$class: 'InfluxDbPublisher', target: myTarget, customPrefix: 'myPrefix', customDataMap: myCustomDataMap])
     *
     */
    private Map<String, Map<String, Object>> customDataMap;

    public InfluxDbPublisher() {
    }

    @DataBoundConstructor
    public InfluxDbPublisher(String target) {
        this.selectedTarget = target;
    }

    public String getSelectedTarget() {
        String ipTemp = selectedTarget;
        if (ipTemp == null) {
            Target[] targets = DESCRIPTOR.getTargets();
            if (targets.length > 0) {
                ipTemp = targets[0].getUrl() + "," + targets[0].getDatabase();
            }
        }
        return ipTemp;
    }

    public void setSelectedTarget(String target) {
        Preconditions.checkNotNull(target);
        this.selectedTarget = target;
    }

    public String getCustomPrefix() {
        return customPrefix;
    }

    @DataBoundSetter
    public void setCustomPrefix(String customPrefix) {
        this.customPrefix = customPrefix;
    }

    @DataBoundSetter
    public void setCustomData (Map<String, Object> customData) {
        this.customData = customData;
    }

    public Map<String, Object> getCustomData () {
        return customData;
    }

    @DataBoundSetter
    public void setCustomDataMap(Map<String, Map<String, Object>> customDataMap) {
        this.customDataMap = customDataMap;
    }

    public Map<String, Map<String, Object>> getCustomDataMap() {
        return customDataMap;
    }

    public Target getTarget() {
        Target[] targets = DESCRIPTOR.getTargets();
        if (selectedTarget == null && targets.length > 0) {
            return targets[0];
        }
        for (Target target : targets) {
            String targetInfo = target.getUrl() + "," + target.getDatabase();
            if (targetInfo.equals(selectedTarget)) {
                return target;
            }
        }
        return null;
    }

    //@Override
    public boolean prebuild(Run<?, ?> build, TaskListener listener) {
        return true;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {

        MeasurementRenderer<Run<?, ?>> measurementRenderer = new ProjectNameRenderer(customPrefix);

        // get the target from the job's config
        Target target = getTarget();
        if (target==null) {
            throw new RuntimeException("Target was null!");
        }

        // prepare a meaningful logmessage
        String logMessage = "[InfluxDB Plugin] Publishing data to: " + target.toString();

        // write to jenkins logger
        logger.log(Level.INFO, logMessage);
        // write to jenkins console
        listener.getLogger().println(logMessage);

        // connect to InfluxDB
        InfluxDB influxDB = InfluxDBFactory.connect(target.getUrl(), target.getUsername(), target.getPassword());
        List<Point> pointsToWrite = new ArrayList<Point>();

        // finally write to InfluxDB
        JenkinsBasePointGenerator jGen = new JenkinsBasePointGenerator(measurementRenderer, customPrefix, build);
        pointsToWrite.addAll(Arrays.asList(jGen.generate()));

        CustomDataPointGenerator cdGen = new CustomDataPointGenerator(measurementRenderer, customPrefix, build, customData);
        if (cdGen.hasReport()) {
            listener.getLogger().println("[InfluxDB Plugin] Custom data found. Writing to InfluxDB...");
            pointsToWrite.addAll(Arrays.asList(cdGen.generate()));
        }

        CustomDataMapPointGenerator cdmGen = new CustomDataMapPointGenerator(measurementRenderer, customPrefix, build, customDataMap);
        if (cdmGen.hasReport()) {
            listener.getLogger().println("[InfluxDB Plugin] Custom data map found. Writing to InfluxDB...");
            pointsToWrite.addAll(Arrays.asList(cdmGen.generate()));
        }

        CoberturaPointGenerator cGen = new CoberturaPointGenerator(measurementRenderer, customPrefix, build);
        if (cGen.hasReport()) {
            listener.getLogger().println("[InfluxDB Plugin] Cobertura data found. Writing to InfluxDB...");
            pointsToWrite.addAll(Arrays.asList(cGen.generate()));
        }

        RobotFrameworkPointGenerator rfGen = new RobotFrameworkPointGenerator(measurementRenderer, customPrefix, build);
        if (rfGen.hasReport()) {
            listener.getLogger().println("[InfluxDB Plugin] Robot Framework data found. Writing to InfluxDB...");
            pointsToWrite.addAll(Arrays.asList(rfGen.generate()));
        }

        JacocoPointGenerator jacoGen = new JacocoPointGenerator(measurementRenderer, customPrefix, build);
        if (jacoGen.hasReport()) {
            listener.getLogger().println("[InfluxDB Plugin] Jacoco data found. Writing to InfluxDB...");
            pointsToWrite.addAll(Arrays.asList(jacoGen.generate()));
        }

        PerformancePointGenerator perfGen = new PerformancePointGenerator(measurementRenderer, customPrefix, build);
        if (perfGen.hasReport()) {
            listener.getLogger().println("[InfluxDB Plugin] Performance data found. Writing to InfluxDB...");
            pointsToWrite.addAll(Arrays.asList(perfGen.generate()));
        }

        writeToInflux(target, influxDB, pointsToWrite);

    }

    private void writeToInflux(Target target, InfluxDB influxDB, List<Point> pointsToWrite) {
        /**
         * build batchpoints for a single write.
         */
        try {
            BatchPoints batchPoints = BatchPoints
                    .database(target.getDatabase())
                    .points(pointsToWrite.toArray(new Point[0]))
                    .retentionPolicy(target.getRetentionPolicy())
                    .consistency(ConsistencyLevel.ALL)
                    .build();
            influxDB.write(batchPoints);
        } catch (Exception e) {
            if (target.isExposeExceptions()) {
                throw new InfluxReportException(e);
            } else {
                //Exceptions not exposed by configuration. Just log and ignore.
                logger.log(Level.WARNING, "Could not report to InfluxDB. Ignoring Exception.", e);
            }
        }
    }
}
