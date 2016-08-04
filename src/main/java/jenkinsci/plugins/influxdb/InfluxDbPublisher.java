package jenkinsci.plugins.influxdb;
 
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.System;
 
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
 
import jenkins.tasks.SimpleBuildStep;
import jenkinsci.plugins.influxdb.models.BuildData;
import jenkinsci.plugins.influxdb.models.Target;
import jenkinsci.plugins.influxdb.generators.CoberturaPointGenerator;
import jenkinsci.plugins.influxdb.generators.JenkinsBasePointGenerator;
import jenkinsci.plugins.influxdb.generators.RobotFrameworkPointGenerator;
//import jenkinsci.plugins.influxdb.generators.ZAProxyPointGenerator;
import jenkinsci.plugins.influxdb.generators.PointGenerator;
import hudson.Extension;
import hudson.Launcher;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import org.kohsuke.stapler.DataBoundConstructor;
 
public class InfluxDbPublisher extends Notifier implements SimpleBuildStep{
 
    /** The logger. **/
    private static final Logger logger = Logger.getLogger(InfluxDbPublisher.class.getName());
 
    private static final String INFLUX_MEASUREMENT_PREFIX = "build";
    private static final String INFLUX_FIELDNAME_JOBDURATION = "jobduration";
 
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
 
    private String selectedTarget;
    private String coberturaReportLocation;
 
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
        this.selectedTarget = target;
    }
    
    public String getCoberturaReportLocation() {
       return this.coberturaReportLocation; 
    }
    
    public void setCoberturaReportLocation(String location) {
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
        // finally write to InfluxDB
        influxDB.write(generateInfluxData(buildData, target));

        JenkinsBasePointGenerator jGen = new JenkinsBasePointGenerator(build);
        writeDataToDatabase(influxDB, target, jGen.generate());

        CoberturaPointGenerator cGen = new CoberturaPointGenerator(build, workspace, coberturaReportLocation);
        if (cGen.hasReport()) {
            listener.getLogger().println("Cobertura data found. Writing to InfluxDB...");
            writeDataToDatabase(influxDB, target, cGen.generate());
        }

        RobotFrameworkPointGenerator rfGen = new RobotFrameworkPointGenerator(build);
        if (rfGen.hasReport()) {
            listener.getLogger().println("Robot Framework data found. Writing to InfluxDB...");
            writeDataToDatabase(influxDB, target, rfGen.generate());
        }

        /*
        ZAProxyPointGenerator zGen = new ZAProxyPointGenerator(build, workspace);
        if (zGen.hasReport())
            writeDataToDatabase(influxDB, target, zGen.generate());
        */
    }

    // Write multiple points from an array. 
    private void writeDataToDatabase(InfluxDB influxDB, Target target, Point[] data) {
        for (Point p : data) {
            influxDB.write(target.getDatabase(), "default", p);
        }
    }
 
    private BatchPoints generateInfluxData(BuildData buildData, Target target) {
        // create the list of batchpoints
        BatchPoints batchPoints = BatchPoints.database(target.getDatabase())
                .tag("async", "true")
                .retentionPolicy("default")
                .consistency(ConsistencyLevel.ALL)
                .build();

        // prepare the measurement point for the timeseries
        Point point = Point.measurement(INFLUX_MEASUREMENT_PREFIX + "_" + buildData.getJobName())
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .field(INFLUX_FIELDNAME_JOBDURATION, buildData.getJobDurationSeconds())
                .build();
 
        batchPoints.point(point);
        return batchPoints;
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
