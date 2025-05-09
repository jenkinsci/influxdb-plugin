package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jacoco.JacocoBuildAction;
import hudson.plugins.jacoco.model.Coverage;
import jenkinsci.plugins.influxdb.models.AbstractPoint;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

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

    public AbstractPoint[] generate() {

        AbstractPoint point = buildPoint("jacoco_data", customPrefix, build);
        addFields(point, JACOCO_CLASS, jacocoBuildAction.getResult().getClassCoverage());
        addFields(point, JACOCO_LINE, jacocoBuildAction.getResult().getLineCoverage());
        addFields(point, JACOCO_BRANCH, jacocoBuildAction.getResult().getBranchCoverage());
        addFields(point, JACOCO_METHOD, jacocoBuildAction.getResult().getMethodCoverage());
        addFields(point, JACOCO_INSTRUCTION, jacocoBuildAction.getResult().getInstructionCoverage());
        addFields(point, JACOCO_COMPLEXITY, jacocoBuildAction.getResult().getComplexityScore());

        return new AbstractPoint[]{point};
    }

    private void addFields(AbstractPoint point, String prefix, Coverage coverage) {
        point.addField(prefix + "_coverage_rate", coverage.getPercentageFloat());
        point.addField(prefix + "_covered", coverage.getCovered());
        point.addField(prefix + "_missed", coverage.getMissed());
    }

}
