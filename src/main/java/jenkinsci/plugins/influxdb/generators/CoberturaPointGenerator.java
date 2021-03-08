package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.write.Point;
import hudson.model.TaskListener;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

import hudson.model.Run;
import hudson.plugins.cobertura.CoberturaBuildAction;
import hudson.plugins.cobertura.Ratio;
import hudson.plugins.cobertura.targets.CoverageMetric;
import hudson.plugins.cobertura.targets.CoverageResult;

public class CoberturaPointGenerator extends AbstractPointGenerator {

    private static final String COBERTURA_PACKAGE_COVERAGE_RATE = "cobertura_package_coverage_rate";
    private static final String COBERTURA_CLASS_COVERAGE_RATE = "cobertura_class_coverage_rate";
    private static final String COBERTURA_LINE_COVERAGE_RATE = "cobertura_line_coverage_rate";
    private static final String COBERTURA_BRANCH_COVERAGE_RATE = "cobertura_branch_coverage_rate";
    private static final String COBERTURA_NUMBER_OF_PACKAGES = "cobertura_number_of_packages";
    private static final String COBERTURA_NUMBER_OF_SOURCEFILES = "cobertura_number_of_sourcefiles";
    private static final String COBERTURA_NUMBER_OF_CLASSES = "cobertura_number_of_classes";

    private final CoberturaBuildAction coberturaBuildAction;
    private final String customPrefix;

    public CoberturaPointGenerator(Run<?, ?> build, TaskListener listener,
                                   ProjectNameRenderer projectNameRenderer,
                                   long timestamp, String jenkinsEnvParameterTag,
                                   String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
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
            .addField(COBERTURA_NUMBER_OF_PACKAGES, packages.denominator)
            .addField(COBERTURA_NUMBER_OF_SOURCEFILES, files.denominator)
            .addField(COBERTURA_NUMBER_OF_CLASSES, classes.denominator)
            .addField(COBERTURA_BRANCH_COVERAGE_RATE, conditionals.getPercentageFloat())
            .addField(COBERTURA_LINE_COVERAGE_RATE, lines.getPercentageFloat())
            .addField(COBERTURA_PACKAGE_COVERAGE_RATE, packages.getPercentageFloat())
            .addField(COBERTURA_CLASS_COVERAGE_RATE, classes.getPercentageFloat());

        return new Point[] {point};
    }
}
