package jenkinsci.plugins.influxdb.generators;

import org.influxdb.dto.Point;

import hudson.model.Run;
import hudson.plugins.cobertura.CoberturaBuildAction;
import hudson.plugins.cobertura.targets.CoverageMetric;

public class CoberturaPointGenerator extends AbstractPointGenerator {

    public static final String COBERTURA_PACKAGE_COVERAGE_RATE = "cobertura_package_coverage_rate";
    public static final String COBERTURA_CLASS_COVERAGE_RATE = "cobertura_class_coverage_rate";
    public static final String COBERTURA_LINE_COVERAGE_RATE = "cobertura_line_coverage_rate";
    public static final String COBERTURA_BRANCH_COVERAGE_RATE = "cobertura_branch_coverage_rate";
    public static final String COBERTURA_NUMBER_OF_PACKAGES = "cobertura_number_of_packages";
    public static final String COBERTURA_NUMBER_OF_SOURCEFILES = "cobertura_number_of_sourcefiles";
    public static final String COBERTURA_NUMBER_OF_CLASSES = "cobertura_number_of_classes";

    private final Run<?, ?> build;
    private final CoberturaBuildAction coberturaBuildAction;

    public CoberturaPointGenerator(Run<?, ?> build) {
        this.build = build;
        coberturaBuildAction = build.getAction(CoberturaBuildAction.class);
    }

    public boolean hasReport() {
        return coberturaBuildAction != null && coberturaBuildAction.getResult() != null;
    }

    public Point[] generate() {
        Point point = buildPoint("cobertura_data", build)
            .field(COBERTURA_BRANCH_COVERAGE_RATE, coberturaBuildAction.getResult().getCoverage(CoverageMetric.CONDITIONAL).getPercentageFloat())
            .field(COBERTURA_LINE_COVERAGE_RATE, coberturaBuildAction.getResult().getCoverage(CoverageMetric.LINE).getPercentageFloat())
            .field(COBERTURA_PACKAGE_COVERAGE_RATE, coberturaBuildAction.getResult().getCoverage(CoverageMetric.PACKAGES).getPercentageFloat())
            .field(COBERTURA_CLASS_COVERAGE_RATE, coberturaBuildAction.getResult().getCoverage(CoverageMetric.CLASSES).getPercentageFloat())
            .build();
        return new Point[] {point};
    }
}
