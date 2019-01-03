package jenkinsci.plugins.influxdb.generators;

import hudson.EnvVars;
import hudson.model.Cause;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.test.AbstractTestResultAction;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.dto.Point;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class JenkinsBasePointGenerator extends AbstractPointGenerator {

    /**
     * The logger.
     **/
    private static final Logger logger = Logger.getLogger(JenkinsBasePointGenerator.class.getName());

    public static final String BUILD_TIME = "build_time";
    public static final String BUILD_STATUS_MESSAGE = "build_status_message";
    public static final String TIME_IN_QUEUE = "time_in_queue";
    public static final String BUILD_SCHEDULED_TIME = "build_scheduled_time";
    public static final String BUILD_EXEC_TIME = "build_exec_time";
    public static final String BUILD_MEASURED_TIME = "build_measured_time";

    /* BUILD_RESULT BUILD_RESULT_ORDINAL BUILD_IS_SUCCESSFUL - explanation
     * SUCCESS   0 true  - The build had no errors.
     * UNSTABLE  1 true  - The build had some errors but they were not fatal. For example, some tests failed.
     * FAILURE   2 false - The build had a fatal error.
     * NOT_BUILT 3 false - The module was not built.
     * ABORTED   4 false - The build was manually aborted.
     */
    public static final String BUILD_RESULT = "build_result";
    public static final String BUILD_RESULT_ORDINAL = "build_result_ordinal";
    public static final String BUILD_IS_SUCCESSFUL = "build_successful";

    public static final String BUILD_AGENT_NAME = "build_agent_name";
    public static final String BUILD_BRANCH_NAME = "build_branch_name";
    public static final String BUILD_CAUSER = "build_causer";

    public static final String PROJECT_BUILD_HEALTH = "project_build_health";
    public static final String PROJECT_LAST_SUCCESSFUL = "last_successful_build";
    public static final String PROJECT_LAST_STABLE = "last_stable_build";
    public static final String TESTS_FAILED = "tests_failed";
    public static final String TESTS_SKIPPED = "tests_skipped";
    public static final String TESTS_TOTAL = "tests_total";

    private final Run<?, ?> build;
    private final String customPrefix;
    private final String jenkinsEnvParameterField;
    private final String measurementName;
    private EnvVars env;


    // (Run<?, ?> build, TaskListener listener, MeasurementRenderer projectNameRenderer, long timestamp, String jenkinsEnvParameterTag) {
    public JenkinsBasePointGenerator(Run<?, ?> build, TaskListener listener,
                                     ProjectNameRenderer projectNameRenderer,
                                     long timestamp, String jenkinsEnvParameterTag, String jenkinsEnvParameterField,
                                     String customPrefix, String measurementName, EnvVars env) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.build = build;
        this.customPrefix = customPrefix;
        this.jenkinsEnvParameterField = jenkinsEnvParameterField;
        this.measurementName = measurementName;
        this.env = env;
    }

    public boolean hasReport() {
        return true;
    }

    public Point[] generate() {
        // Build is not finished when running with pipelines. Duration must be calculated manually
        long startTime = build.getTimeInMillis();
        long currTime = System.currentTimeMillis();
        long dt = currTime - startTime;
        // Build is not finished when running with pipelines. Set build status as unknown and ordinal
        // as something not predefined
        String result;
        int ordinal;
        Result buildResult = build.getResult();
        if (buildResult == null) {
            result = "?";
            ordinal = 5;
        } else {
            result = buildResult.toString();
            ordinal = buildResult.ordinal;
        }

        Point point = buildPoint(measurementName, customPrefix, build);

        point.addField(BUILD_TIME, build.getDuration() == 0 ? dt : build.getDuration())
                .addField(BUILD_SCHEDULED_TIME, build.getTimeInMillis())
                .addField(BUILD_EXEC_TIME, build.getStartTimeInMillis())
                .addField(BUILD_MEASURED_TIME, currTime)
                .addField(BUILD_STATUS_MESSAGE, build.getBuildStatusSummary().message)
                .addField(BUILD_RESULT, result)
                .addField(BUILD_RESULT_ORDINAL, ordinal)
                .addField(BUILD_IS_SUCCESSFUL, ordinal < 2)
                .addField(BUILD_AGENT_NAME, getBuildEnv("NODE_NAME"))
                .addField(BUILD_BRANCH_NAME, getBuildEnv("BRANCH_NAME"))
                .addField(PROJECT_BUILD_HEALTH, build.getParent().getBuildHealth().getScore())
                .addField(PROJECT_LAST_SUCCESSFUL, getLastSuccessfulBuild())
                .addField(PROJECT_LAST_STABLE, getLastStableBuild())
                .addField(BUILD_CAUSER, getCauseShortDescription())
                .addTag(BUILD_RESULT, result);

        if (hasTestResults(build)) {
            point.addField(TESTS_FAILED, build.getAction(AbstractTestResultAction.class).getFailCount());
            point.addField(TESTS_SKIPPED, build.getAction(AbstractTestResultAction.class).getSkipCount());
            point.addField(TESTS_TOTAL, build.getAction(AbstractTestResultAction.class).getTotalCount());
        }

        if (hasMetricsPlugin(build)) {
            point.addField(TIME_IN_QUEUE, build.getAction(jenkins.metrics.impl.TimeInQueueAction.class).getQueuingDurationMillis());
        }

        if (StringUtils.isNotBlank(jenkinsEnvParameterField)) {
            Properties fieldProperties = parsePropertiesString(jenkinsEnvParameterField);
            Map fieldMap = resolveEnvParameterAndTransformToMap(fieldProperties);
            point.addFields(fieldMap);
        }

        setServiceIdTag(point);

        return new Point[]{point};
    }

    private void setServiceIdTag(Point.Builder point) {
        try {
            if (setServiceTagFromRun(build, point)) {
                return;
            }
            Cause.UpstreamCause cause = build.getCause(Cause.UpstreamCause.class);
            while (cause != null) {
                if (cause.getUpstreamRun() != null && setServiceTagFromRun(cause.getUpstreamRun(), point)) {
                    return;
                }
                cause = cause.getUpstreamRun().getCause(Cause.UpstreamCause.class);
            }
        } catch (Exception e) {
            logger.warning("Could not retrieve service_id, " + e);
        }
    }

    private boolean setServiceTagFromRun(Run<?, ?> build, Point.Builder point) throws IOException {
        String serviceId = new RunWrapper(build, false).getBuildVariables().get("SERVICE_ID");
        if (serviceId != null) {
            point.tag("service_id", serviceId);
            return true;
        }
        return false;
    }

    private String getBuildEnv(String buildEnv) {
        String s = env.get(buildEnv);
        return s == null ? "" : s;
    }

    private boolean hasTestResults(Run<?, ?> build) {
        return build.getAction(AbstractTestResultAction.class) != null;
    }

    private boolean hasMetricsPlugin(Run<?, ?> build) {
        try {
            return build.getAction(jenkins.metrics.impl.TimeInQueueAction.class) != null;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    private String getCauseShortDescription() {
        try {
            List<Cause> shortDescriptionList = build.getCauses();
            Cause shortDescription = shortDescriptionList.get(0);
            return shortDescription != null ? shortDescription.getShortDescription() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private int getLastSuccessfulBuild() {
        Run<?, ?> lastSuccessfulBuild = build.getParent().getLastSuccessfulBuild();
        return lastSuccessfulBuild != null ? lastSuccessfulBuild.getNumber() : 0;
    }

    private int getLastStableBuild() {
        Run<?, ?> lastStableBuild = build.getParent().getLastStableBuild();
        return lastStableBuild != null ? lastStableBuild.getNumber() : 0;
    }
}
