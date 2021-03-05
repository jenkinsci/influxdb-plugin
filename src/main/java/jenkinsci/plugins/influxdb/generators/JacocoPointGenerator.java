package jenkinsci.plugins.influxdb.generators;

import hudson.model.TaskListener;
import hudson.plugins.jacoco.model.Coverage;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.influxdb.dto.Point;

import hudson.model.Run;
import hudson.plugins.jacoco.JacocoBuildAction;

public class JacocoPointGenerator extends AbstractPointGenerator {
    private static final String JACOCO_CLASS = "jacoco_class";
    private static final String JACOCO_LINE = "jacoco_line";
    private static final String JACOCO_BRANCH = "jacoco_branch";
    private static final String JACOCO_METHOD = "jacoco_method";
    private static final String JACOCO_INSTRUCTION = "jacoco_instruction";
    private static final String JACOCO_COMPLEXITY = "jacoco_complexity";

    private final String customPrefix;
    private final JacocoBuildAction jacocoBuildAction;

    public JacocoPointGenerator(Run<?, ?> build, TaskListener listener,
                                ProjectNameRenderer projectNameRenderer,
                                long timestamp, String jenkinsEnvParameterTag,
                                String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
        jacocoBuildAction = build.getAction(JacocoBuildAction.class);
    }

    public boolean hasReport() {
        return jacocoBuildAction != null && jacocoBuildAction.getResult() != null;
    }

    public Point[] generate() {
        Point.Builder builder = buildPoint("jacoco_data", customPrefix, build);
            addFields(builder, JACOCO_CLASS, jacocoBuildAction.getResult().getClassCoverage());
            addFields(builder, JACOCO_LINE, jacocoBuildAction.getResult().getLineCoverage());
            addFields(builder, JACOCO_BRANCH, jacocoBuildAction.getResult().getBranchCoverage());
            addFields(builder, JACOCO_METHOD, jacocoBuildAction.getResult().getMethodCoverage());
            addFields(builder, JACOCO_INSTRUCTION, jacocoBuildAction.getResult().getInstructionCoverage());
            addFields(builder, JACOCO_COMPLEXITY, jacocoBuildAction.getResult().getComplexityScore());
            Point point = builder.build();
        return new Point[] {point};
    }

    private void addFields(Point.Builder builder, String prefix, Coverage coverage) {
        builder.addField(prefix + "_coverage_rate", coverage.getPercentageFloat());
        builder.addField(prefix + "_covered", coverage.getCovered());
        builder.addField(prefix + "_missed", coverage.getMissed());
    }

}
