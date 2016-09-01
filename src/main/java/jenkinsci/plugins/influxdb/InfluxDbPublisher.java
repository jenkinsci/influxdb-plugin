package jenkinsci.plugins.influxdb;

import com.google.common.base.Preconditions;
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
import jenkinsci.plugins.influxdb.generators.CoberturaPointGenerator;
import jenkinsci.plugins.influxdb.generators.JacocoPointGenerator;
import jenkinsci.plugins.influxdb.generators.JenkinsBasePointGenerator;
import jenkinsci.plugins.influxdb.generators.RobotFrameworkPointGenerator;
import jenkinsci.plugins.influxdb.models.BuildData;
import jenkinsci.plugins.influxdb.models.Target;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//import jenkinsci.plugins.influxdb.generators.ZAProxyPointGenerator;

public class InfluxDbPublisher extends Notifier implements SimpleBuildStep{

    /** The logger. **/
    private static final Logger logger = Logger.getLogger(InfluxDbPublisher.class.getName());

    private static final String INFLUX_MEASUREMENT_PREFIX = "build";
    private static final String INFLUX_FIELDNAME_JOBDURATION = "jobduration";

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private String selectedTarget;
    //default - unless overridden
    private String coberturaReportLocation = "target/cobertura/cobertura.ser";

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

    public String getCoberturaReportLocation() {
       return this.coberturaReportLocation;
    }

    public void setCoberturaReportLocation(String location) {
        Preconditions.checkNotNull(location);
        this.coberturaReportLocation = location;
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

        // get the target from the job's config
        Target target = getTarget();
        if (target==null) {
            throw new RuntimeException("Target was null!");
        }
        // extract the required buildData from the build context
        BuildData buildData = getBuildData(build);

        // prepare a meaningful logmessage
        String logMessage = "publishing data: " + buildData.toString() + " to " + target.toString();

        // write to jenkins logger
        logger.log(Level.INFO, logMessage);
        // write to jenkins console
        listener.getLogger().println(logMessage);

        // connect to InfluxDB
        InfluxDB influxDB = InfluxDBFactory.connect(target.getUrl(), target.getUsername(), target.getPassword());
        List<Point> pointsToWrite = new ArrayList<Point>();

        // finally write to InfluxDB
        pointsToWrite.addAll(generateInfluxData(buildData));

        JenkinsBasePointGenerator jGen = new JenkinsBasePointGenerator(build);
        pointsToWrite.addAll(Arrays.asList(jGen.generate()));

        CoberturaPointGenerator cGen = new CoberturaPointGenerator(build, workspace, coberturaReportLocation);
        if (cGen.hasReport()) {
            listener.getLogger().println("Cobertura data found. Writing to InfluxDB...");
            pointsToWrite.addAll(Arrays.asList(cGen.generate()));
        }


        RobotFrameworkPointGenerator rfGen = new RobotFrameworkPointGenerator(build);
        if (rfGen.hasReport()) {
            listener.getLogger().println("Robot Framework data found. Writing to InfluxDB...");
            pointsToWrite.addAll(Arrays.asList(rfGen.generate()));
        }

        JacocoPointGenerator jacoGen = new JacocoPointGenerator(build, workspace);
        if (jacoGen.hasReport()) {
            listener.getLogger().println("Jacoco data found. Writing to InfluxDB...");
            pointsToWrite.addAll(Arrays.asList(jacoGen.generate()));
        }

        /*
        ZAProxyPointGenerator zGen = new ZAProxyPointGenerator(build, workspace);
        if (zGen.hasReport()) {
            listener.getLogger().println("ZAProxy data found. Writing to InfluxDB...");
            writeDataToDatabase(influxDB, target, zGen.generate());
        }
        */
        writeToInflux(target, influxDB, pointsToWrite);


    }

    private void writeToInflux(Target target, InfluxDB influxDB, List<Point> pointsToWrite) {
        /**
         * build batchpoints for a single write.
         */
        BatchPoints batchPoints = BatchPoints
                .database(target.getDatabase())
                .points(pointsToWrite.toArray(new Point[0]))
                .retentionPolicy(target.getRetentionPolicy())
                .tag("async", "true")
                .consistency(ConsistencyLevel.ALL)
                .build();
        influxDB.write(batchPoints);
    }


    private List<Point> generateInfluxData(BuildData buildData) {
        // prepare the measurement point for the timeseries
        Point point = Point.measurement(INFLUX_MEASUREMENT_PREFIX + "_" + jobName(buildData))
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .field(INFLUX_FIELDNAME_JOBDURATION, buildData.getJobDurationSeconds())
                .build();
        return Lists.newArrayList(point);
    }

    //influx disallows "-" in measurements.
    private String jobName(BuildData buildData) {
        return buildData.getJobName().replaceAll("-", "_");
    }

    @Deprecated
    private BuildData getBuildData(AbstractBuild<?, ?> build) {
        BuildData buildData = new BuildData();
        buildData.setJobName(build.getProject().getName());
        buildData.setJobDurationSeconds(build.getDuration());
        return buildData;
    }

    private BuildData getBuildData(Run<?, ?> build) {
        BuildData buildData = new BuildData();
        buildData.setJobName(build.getParent().getName());
        buildData.setJobDurationSeconds(System.currentTimeMillis() - build.getTimeInMillis());
        return buildData;
    }
}
