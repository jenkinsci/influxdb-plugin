package jenkinsci.plugins.influxdb.generators;

import org.influxdb.dto.Point;

import hudson.model.Run;
import hudson.plugins.cobertura.CoberturaBuildAction;
import hudson.plugins.cobertura.Ratio;
import hudson.plugins.cobertura.targets.CoverageMetric;
import hudson.plugins.cobertura.targets.CoverageResult;

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
    private final String customPrefix;

    public CoberturaPointGenerator(String customPrefix, Run<?, ?> build) {
        this.build = build;
        this.customPrefix = customPrefix;
        coberturaBuildAction = build.getAction(CoberturaBuildAction.class);
    }

    public boolean hasReport() {
        return coberturaBuildAction != null && coberturaBuildAction.getResult() != null;
    }

    public Point[] generate() {
        CoverageResult result = coberturaBuildAction.getResult();
        Ratio conditionals = result.getCoverage(CoverageMetric.CONDITIONAL);
        Ratio lines = result.getCoverage(CoverageMetric.LINE);
        Ratio packages = result.getCoverage(CoverageMetric.PACKAGES);
        Ratio classes = result.getCoverage(CoverageMetric.CLASSES);
        Ratio files = result.getCoverage(CoverageMetric.FILES);
        Point point = buildPoint("cobertura_data", customPrefix, build)
            .field(COBERTURA_NUMBER_OF_PACKAGES, packages.denominator)
            .field(COBERTURA_NUMBER_OF_SOURCEFILES, files.denominator)
            .field(COBERTURA_NUMBER_OF_CLASSES, classes.denominator)
            .field(COBERTURA_BRANCH_COVERAGE_RATE, conditionals.getPercentageFloat())
            .field(COBERTURA_LINE_COVERAGE_RATE, lines.getPercentageFloat())
            .field(COBERTURA_PACKAGE_COVERAGE_RATE, packages.getPercentageFloat())
            .field(COBERTURA_CLASS_COVERAGE_RATE, classes.getPercentageFloat())
            .build();
        return new Point[] {point};
    }
}
