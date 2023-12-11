package jenkinsci.plugins.influxdb.generators.serenity;

import com.influxdb.client.write.Point;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkinsci.plugins.influxdb.generators.AbstractPointGenerator;

public class SerenityPointGenerator extends AbstractPointGenerator {

    private static final String SERENITY_RESULTS_COUNTS_TOTAL = "serenity_results_counts_total";
    private static final String SERENITY_RESULTS_COUNTS_SUCCESS = "serenity_results_counts_success";
    private static final String SERENITY_RESULTS_COUNTS_PENDING = "serenity_results_counts_pending";
    private static final String SERENITY_RESULTS_COUNTS_IGNORED = "serenity_results_counts_ignored";
    private static final String SERENITY_RESULTS_COUNTS_SKIPPED = "serenity_results_counts_skipped";
    private static final String SERENITY_RESULTS_COUNTS_FAILURE = "serenity_results_counts_failure";
    private static final String SERENITY_RESULTS_COUNTS_ERROR = "serenity_results_counts_error";
    private static final String SERENITY_RESULTS_COUNTS_COMPROMISED = "serenity_results_counts_compromised";

    private static final String SERENITY_RESULTS_PERCENTAGES_SUCCESS = "serenity_results_percentages_success";
    private static final String SERENITY_RESULTS_PERCENTAGES_PENDING = "serenity_results_percentages_pending";
    private static final String SERENITY_RESULTS_PERCENTAGES_IGNORED = "serenity_results_percentages_ignored";
    private static final String SERENITY_RESULTS_PERCENTAGES_SKIPPED = "serenity_results_percentages_skipped";
    private static final String SERENITY_RESULTS_PERCENTAGES_FAILURE = "serenity_results_percentages_failure";
    private static final String SERENITY_RESULTS_PERCENTAGES_ERROR = "serenity_results_percentages_error";
    private static final String SERENITY_RESULTS_PERCENTAGES_COMPROMISED = "serenity_results_percentages_compromised";

    private static final String SERENITY_RESULTS_TOTAL_TEST_DURATION = "serenity_results_total_test_duration";
    private static final String SERENITY_RESULTS_TOTAL_CLOCK_DURATION = "serenity_results_total_clock_duration";
    private static final String SERENITY_RESULTS_MIN_TEST_DURATION = "serenity_results_min_test_duration";
    private static final String SERENITY_RESULTS_MAX_TEST_DURATION = "serenity_results_max_test_duration";
    private static final String SERENITY_RESULTS_AVERAGE_TEST_DURATION = "serenity_results_average_test_duration";

    private final Run<?, ?> build;
    private final String customPrefix;
    private final TaskListener listener;

    // support for dependency injection
    ISerenityJsonSummaryFile serenityJsonSummaryFile = null;

    /*
                                Run<?, ?> build, TaskListener listener,
                                MeasurementRenderer<Run<?, ?>> projectNameRenderer,
                                long timestamp, String jenkinsEnvParameterTag,
                                String customPrefix) {
     */
    /*
        public SerenityPointGenerator(MeasurementRenderer<Run<?, ?>> projectNameRenderer, String customPrefix,
                                  Run<?, ?> build, long timestamp, TaskListener listener,
                                  ISerenityJsonSummaryFile serenityJsonSummaryFile) {
     */

    public SerenityPointGenerator(Run<?, ?> build, TaskListener listener,
                                  ProjectNameRenderer projectNameRenderer,
                                  long timestamp, String jenkinsEnvParameterTag, String customPrefix,
                                  ISerenityJsonSummaryFile serenityJsonSummaryFile) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.build = build;
        this.customPrefix = customPrefix;
        this.listener = listener;
        this.serenityJsonSummaryFile = serenityJsonSummaryFile;
    }

    public boolean hasReport() {
        return serenityJsonSummaryFile.exists();
    }

    public Point[] generate() {
        String contents;
        try {
            contents = serenityJsonSummaryFile.getContents();
        } catch (IOException e) {
            listener.getLogger().println("Failed to read from file " + serenityJsonSummaryFile.getPath() + ", due to: " + e);
            return null;
        }

        JSONObject root = JSONObject.fromObject(contents);
        JSONObject results = root.getJSONObject("results");
        JSONObject resultsCounts = results.getJSONObject("counts");
        JSONObject resultsPercentages = results.getJSONObject("percentages");
        JSONArray tagTypes = root.getJSONArray("tags");

        Point point = buildPoint("serenity_data", customPrefix, build);

        // include results.counts fields
        point
            .addField(SERENITY_RESULTS_COUNTS_TOTAL, resultsCounts.getInt("total"))
            .addField(SERENITY_RESULTS_COUNTS_SUCCESS, resultsCounts.getInt("success"))
            .addField(SERENITY_RESULTS_COUNTS_PENDING, resultsCounts.getInt("pending"))
            .addField(SERENITY_RESULTS_COUNTS_IGNORED, resultsCounts.getInt("ignored"))
            .addField(SERENITY_RESULTS_COUNTS_SKIPPED, resultsCounts.getInt("skipped"))
            .addField(SERENITY_RESULTS_COUNTS_FAILURE, resultsCounts.getInt("failure"))
            .addField(SERENITY_RESULTS_COUNTS_ERROR, resultsCounts.getInt("error"))
            .addField(SERENITY_RESULTS_COUNTS_COMPROMISED, resultsCounts.getInt("compromised"));

        // include results.percentages fields
        point
            .addField(SERENITY_RESULTS_PERCENTAGES_SUCCESS, resultsPercentages.getInt("success"))
            .addField(SERENITY_RESULTS_PERCENTAGES_PENDING, resultsPercentages.getInt("pending"))
            .addField(SERENITY_RESULTS_PERCENTAGES_IGNORED, resultsPercentages.getInt("ignored"))
            .addField(SERENITY_RESULTS_PERCENTAGES_SKIPPED, resultsPercentages.getInt("skipped"))
            .addField(SERENITY_RESULTS_PERCENTAGES_FAILURE, resultsPercentages.getInt("failure"))
            .addField(SERENITY_RESULTS_PERCENTAGES_ERROR, resultsPercentages.getInt("error"))
            .addField(SERENITY_RESULTS_PERCENTAGES_COMPROMISED, resultsPercentages.getInt("compromised"));

        // include remaining results fields
        point
            .addField(SERENITY_RESULTS_TOTAL_TEST_DURATION, results.getLong("totalTestDuration"))
            .addField(SERENITY_RESULTS_TOTAL_CLOCK_DURATION, results.getLong("totalClockDuration"))
            .addField(SERENITY_RESULTS_MIN_TEST_DURATION, results.getLong("minTestDuration"))
            .addField(SERENITY_RESULTS_MAX_TEST_DURATION, results.getLong("maxTestDuration"))
            .addField(SERENITY_RESULTS_AVERAGE_TEST_DURATION, results.getLong("averageTestDuration"));

        // include tags fields
        for (int iTagType = 0; iTagType < tagTypes.size(); iTagType++) {
            JSONObject tagType = tagTypes.getJSONObject(iTagType);
            String tagTypeType = tagType.getString("tagType");
            JSONArray tagResults = tagType.getJSONArray("tagResults");
            for (int iTag = 0; iTag < tagResults.size(); iTag++) {
                JSONObject tagResult = tagResults.getJSONObject(iTag);
                String field = "serenity_tags_" + tagTypeType + ":" + tagResult.getString("tagName");
                point
                    .addField(field, tagResult.getInt("count"));
            }
        }

        return new Point[]{point};
    }
}
