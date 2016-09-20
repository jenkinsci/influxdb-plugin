package jenkinsci.plugins.influxdb.generators;

import org.influxdb.dto.Point;

import hudson.model.Run;
import hudson.plugins.jacoco.JacocoBuildAction;

public class JacocoPointGenerator extends AbstractPointGenerator {

    public static final String JACOCO_PACKAGE_COVERAGE_RATE = "jacoco_package_coverage_rate";
    public static final String JACOCO_CLASS_COVERAGE_RATE = "jacoco_class_coverage_rate";
    public static final String JACOCO_LINE_COVERAGE_RATE = "jacoco_line_coverage_rate";
    public static final String JACOCO_BRANCH_COVERAGE_RATE = "jacoco_branch_coverage_rate";
    public static final String JACOCO_METHOD_COVERAGE_RATE = "jacoco_method_coverage_rate";
    public static final String JACOCO_INSTRUCTION_COVERAGE_RATE = "jacoco_instruction_coverage_rate";

    private final Run<?, ?> build;
    private final String customPrefix;
    private final JacocoBuildAction jacocoBuildAction;

    public JacocoPointGenerator(String customPrefix, Run<?, ?> build) {
        this.build = build;
        this.customPrefix = customPrefix;
        jacocoBuildAction = build.getAction(JacocoBuildAction.class);
    }

    public boolean hasReport() {
        return jacocoBuildAction != null && jacocoBuildAction.getResult() != null;
    }

    public Point[] generate() {
        Point point = buildPoint(measurementName("jacoco_data"), customPrefix, build)
            .field(JACOCO_INSTRUCTION_COVERAGE_RATE, jacocoBuildAction.getResult().getInstructionCoverage().getPercentageFloat())
            .field(JACOCO_CLASS_COVERAGE_RATE, jacocoBuildAction.getResult().getClassCoverage().getPercentageFloat())
            .field(JACOCO_BRANCH_COVERAGE_RATE, jacocoBuildAction.getResult().getBranchCoverage().getPercentageFloat())
            .field(JACOCO_LINE_COVERAGE_RATE, jacocoBuildAction.getResult().getLineCoverage().getPercentageFloat())
            .field(JACOCO_METHOD_COVERAGE_RATE, jacocoBuildAction.getResult().getMethodCoverage().getPercentageFloat())
            .build();
        return new Point[] {point};
    }

}
