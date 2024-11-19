package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.write.Point;
import edu.hm.hafner.coverage.Metric;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.model.CoverageStatistics;
import io.jenkins.plugins.coverage.metrics.model.ElementFormatter;
import io.jenkins.plugins.coverage.metrics.steps.CoverageBuildAction;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


public class CoveragePointGenerator extends AbstractPointGenerator {


    private final String customPrefix;

    public CoveragePointGenerator(Run<?, ?> build,
                                  TaskListener listener,
                                  ProjectNameRenderer projectNameRenderer,
                                  long timestamp,
                                  String jenkinsEnvParameterTag,
                                  String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
    }

    @Override
    public boolean hasReport() {
        return build.getAction(CoverageBuildAction.class) != null;
    }

    @Override
    public Point[] generate() {

        List<Point> points = new ArrayList<>();
        CoverageBuildAction action = build.getAction(CoverageBuildAction.class);
        CoverageStatistics coverageStatistics = action.getStatistics();
        for (Baseline baseline : new Baseline[]{Baseline.PROJECT, Baseline.MODIFIED_LINES, Baseline.MODIFIED_FILES}) {
            points.add(buildSubPoint("coverage_" + baseline.toString().toLowerCase() + "_data", customPrefix, build, baseline, coverageStatistics));
        }

        return points.toArray(new Point[0]);
    }

    private Point buildSubPoint(String name, String customPrefix, Run<?, ?> build, Baseline baseline, CoverageStatistics coverageStatistics) {
        Point point = buildPoint(name, customPrefix, build);
        ElementFormatter formatter = new ElementFormatter();
        for (Metric m : Metric.values()) {
            coverageStatistics.getValue(baseline, m)
                    .ifPresent(value -> {
                        String x = formatter.format(value);
                        try {
                            Number number = NumberFormat.getInstance().parse(StringUtils.substringBefore(x, "%"));
                            if (StringUtils.contains(x, "%")) {
                                number = number.floatValue();
                            }
                            listener.getLogger().println("Adding field '" + m.toTagName() + "' with value " + number);
                            point.addField(m.toTagName(), number);
                        } catch (ParseException e) {
                            // No operation
                        }
                    });
        }

        return point;
    }
}
