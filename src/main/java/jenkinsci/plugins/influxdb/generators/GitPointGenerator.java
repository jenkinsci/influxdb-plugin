package jenkinsci.plugins.influxdb.generators;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.influxdb.client.write.Point;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

public class GitPointGenerator extends AbstractPointGenerator {

    // Point fields names
    private static final String GIT_REPOSITORY = "git_repository";
    private static final String GIT_REVISION = "git_revision";
    private static final String GIT_REFERENCE = "git_reference";

    // Point fields values
    private String gitRepository;
    private String gitRevision;
    private String gitReference;

    // Log patterns
    private static final String REPOSITORY_PATTERN_IN_LOG = Pattern.quote("Checking out Revision ");
    private static final String REVISION_PATTERN_IN_LOG = Pattern.quote("Cloning repository ");

    private String customPrefix;

    public GitPointGenerator(Run<?, ?> build, TaskListener listener, ProjectNameRenderer projectNameRenderer,
            long timestamp, String jenkinsEnvParameterTag, String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
    }

    /**
     * Check if git infos are presents in the logs of the build
     * 
     * @return true if present
     */
    @Override
    public boolean hasReport() {
        String[] result = null;

        try {
            result = getGitInfosFromBuildLog(build);
            gitRepository = result[0];
            gitRevision = result[1];
            gitReference = result[2];

            return !StringUtils.isEmpty(gitRepository);
        } catch (IOException | IndexOutOfBoundsException | UncheckedIOException ignored) {
            return false;
        }
    }

    /**
     * Retrieve git infos in the log of the build
     * 
     * @param build
     * @return Array of string : String[] { gitRepository, gitRevision, gitReference
     *         }
     * @throws IOException
     */
    private String[] getGitInfosFromBuildLog(Run<?, ?> build) throws IOException {
        String gitRepo = null;
        String gitRev = null;
        String gitRef = null;

        try (BufferedReader br = new BufferedReader(build.getLogReader())) {
            String line;
            Matcher match;
            String[] splitLine;
            final Pattern gitRevisionPattern = Pattern.compile(REVISION_PATTERN_IN_LOG);
            final Pattern gitRepositoryPattern = Pattern.compile(REPOSITORY_PATTERN_IN_LOG);

            while ((line = br.readLine()) != null) {
                match = gitRevisionPattern.matcher(line);
                if (match.matches()) {
                    splitLine = line.split(" ");
                    gitRev = splitLine.length >= 4 ? splitLine[3] : "";
                    gitRef = splitLine.length >= 5 ? splitLine[4].substring(1, splitLine[4].length() - 1) : "";
                    continue;
                }
                match = gitRepositoryPattern.matcher(line);
                if (match.matches()) {
                    splitLine = line.split(" ");
                    gitRepo = splitLine.length >= 2 ? line.split(" ")[2] : "";
                    break;
                }
            }
        }
        return new String[] { gitRepo, gitRev, gitRef };
    }

    @Override
    public Point[] generate() {
        Point point = buildPoint("git_data", customPrefix, build)//
                .addField(GIT_REPOSITORY, gitRepository)//
                .addField(GIT_REFERENCE, gitReference)//
                .addField(GIT_REVISION, gitRevision);//

        return new Point[] { point };
    }

}
