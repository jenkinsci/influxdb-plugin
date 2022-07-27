package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.write.Point;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.metrics.impl.TimeInQueueAction;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

public class MetricsPointGenerator extends AbstractPointGenerator {
    private static final String BLOCKED_TIME = "blocked_time";
    private static final String BUILDABLE_TIME = "buildable_time";
    private static final String BUILDING_TIME = "building_time";
    private static final String EXECUTING_TIME = "executing_time";
    private static final String EXECUTOR_UTILIZATION = "executor_utilization";
    private static final String QUEUEING_TIME = "queue_time";
    private static final String SUBTASK_COUNT = "subtask_count";
    private static final String TOTAL_DURATION = "total_duration";
    private static final String WAITING_TIME = "waiting_time";

    private final Run<?, ?> build;
    private final String customPrefix;
    private final TimeInQueueAction timeInQueueAction;

    public MetricsPointGenerator(Run<?, ?> build, TaskListener listener,
                                        ProjectNameRenderer projectNameRenderer,
                                        long timestamp, String jenkinsEnvParameterTag,
                                        String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.build = build;
        this.customPrefix = customPrefix;
        timeInQueueAction = build.getAction(TimeInQueueAction.class);
    }

    public boolean hasReport() {
        return timeInQueueAction != null;
    }

    public Point[] generate() {
        Point point = buildPoint("metrics_data", customPrefix, build);
        point.addField(BLOCKED_TIME, timeInQueueAction.getBlockedDurationMillis());
        point.addField(BUILDABLE_TIME, timeInQueueAction.getBuildableDurationMillis());
        point.addField(BUILDING_TIME, timeInQueueAction.getBuildingDurationMillis());
        point.addField(EXECUTING_TIME, timeInQueueAction.getExecutingTimeMillis());
        point.addField(EXECUTOR_UTILIZATION, timeInQueueAction.getExecutorUtilization());
        point.addField(QUEUEING_TIME, timeInQueueAction.getQueuingDurationMillis());
        point.addField(SUBTASK_COUNT, timeInQueueAction.getSubTaskCount());
        point.addField(TOTAL_DURATION, timeInQueueAction.getTotalDurationMillis());
        point.addField(WAITING_TIME, timeInQueueAction.getWaitingDurationMillis());
        return new Point[] {point};
    }
}
