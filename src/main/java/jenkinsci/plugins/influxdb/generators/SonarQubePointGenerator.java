package jenkinsci.plugins.influxdb.generators;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.HttpURLConnection;
import java.lang.InterruptedException;
import java.util.Base64;
import java.net.URL;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.dto.Point;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.IOUtils;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class SonarQubePointGenerator extends AbstractPointGenerator {

	private static final String BUILD_DISPLAY_NAME = "display_name";
	private static final String SONARQUBE_LINES_OF_CODE = "lines_of_code";
	private static final String SONARQUBE_COMPLEXITY = "complexity";
	private static final String SONARQUBE_CRITICAL_ISSUES = "critical_issues";
	private static final String SONARQUBE_MAJOR_ISSUES = "major_issues";
	private static final String SONARQUBE_MINOR_ISSUES = "minor_issues";
	private static final String SONARQUBE_INFO_ISSUES = "info_issues";
	private static final String SONARQUBE_BLOCKER_ISSUES = "blocker_issues";

	private static final String URL_PATTERN_IN_LOGS = ".*" + Pattern.quote("ANALYSIS SUCCESSFUL, you can browse ")
			+ "(.*)";

	private static final String SONAR_ISSUES_BASE_URL = "/api/issues/search?ps=500&projectKeys=";

	private static final String SONAR_METRICS_BASE_URL = "/api/measures/component?metricKeys=ncloc,complexity,violations&componentKey=";

	private String SONAR_ISSUES_URL;
	private String SONAR_METRICS_URL;
	private String sonarServer;
	private String sonarProjectName;

	private final Run<?, ?> build;
	private final String customPrefix;
	private final TaskListener listener;

	public SonarQubePointGenerator(MeasurementRenderer<Run<?, ?>> measurementRenderer, String customPrefix,
			Run<?, ?> build, long timestamp, TaskListener listener, boolean replaceDashWithUnderscore) {
		super(measurementRenderer, timestamp, replaceDashWithUnderscore);
		this.build = build;
		this.customPrefix = customPrefix;
		this.listener = listener;
	}

	public boolean hasReport() {
		String sonarBuildLink = null;
		try {
			sonarBuildLink = getSonarProjectURLFromBuildLogs(build);
			if (!StringUtils.isEmpty(sonarBuildLink)) {
				setSonarDetails(sonarBuildLink);
				return true;
			}
		} catch (IOException e) {
			//
		}

		return false;
	}

	public void setSonarDetails(String sonarBuildLink) {
		try {
			this.sonarProjectName = getSonarProjectName(sonarBuildLink);
			// Use SONAR_HOST_URL environment variable if possible
			String url = "";
			try {
				url = build.getEnvironment(listener).get("SONAR_HOST_URL");
			} catch (InterruptedException|IOException e) {
				// handle
			}
			if (url != null && !url.isEmpty()) {
				this.sonarServer = url;
			} else {
				if (sonarBuildLink.indexOf("/dashboard?id=" + this.sonarProjectName) > 0) {
					this.sonarServer = sonarBuildLink.substring(0,
							sonarBuildLink.indexOf("/dashboard?id=" + this.sonarProjectName));
				} else {
					this.sonarServer = sonarBuildLink.substring(0,
							sonarBuildLink.indexOf("/dashboard/index/" + this.sonarProjectName));
				}
			}
			this.SONAR_ISSUES_URL = sonarServer + SONAR_ISSUES_BASE_URL + sonarProjectName + "&resolved=false&severities=";
			this.SONAR_METRICS_URL = sonarServer + SONAR_METRICS_BASE_URL + sonarProjectName;
		} catch (URISyntaxException e) {
			//
		}

	}

	public Point[] generate() {
		Point point = null;
		try {
			point = buildPoint(measurementName("sonarqube_data"), customPrefix, build)
					.addField(BUILD_DISPLAY_NAME, build.getDisplayName())
					.addField(SONARQUBE_CRITICAL_ISSUES, getSonarIssues(this.SONAR_ISSUES_URL, "CRITICAL"))
					.addField(SONARQUBE_BLOCKER_ISSUES, getSonarIssues(this.SONAR_ISSUES_URL, "BLOCKER"))
					.addField(SONARQUBE_MAJOR_ISSUES, getSonarIssues(this.SONAR_ISSUES_URL, "MAJOR"))
					.addField(SONARQUBE_MINOR_ISSUES, getSonarIssues(this.SONAR_ISSUES_URL, "MINOR"))
					.addField(SONARQUBE_INFO_ISSUES, getSonarIssues(this.SONAR_ISSUES_URL, "INFO"))
					.addField(SONARQUBE_LINES_OF_CODE, getLinesOfCode(this.SONAR_METRICS_URL)).build();
		} catch (IOException e) {
			// handle
		}
		return new Point[] { point };
	}

	public String getResult(String request) throws IOException {
		StringBuffer result = new StringBuffer();
		
		try 
		{
			String auth = "";
			try {
				String token = build.getEnvironment(listener).get("SONAR_AUTH_TOKEN");
				if (token != null) {
					token = token + ":";
					String encoding = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
					auth = "Basic " + encoding;
				}
			} catch (InterruptedException|IOException e) {
				// handle
			}
			URL url = new URL(request);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			if (!auth.isEmpty())
				conn.setRequestProperty("Authorization", auth);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode() + " from URL : " + conn.getURL());
			}

			BufferedReader rd = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			conn.disconnect();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return result.toString();
	}

	@SuppressWarnings("deprecation")
	private String getSonarProjectURLFromBuildLogs(Run<?, ?> build) throws IOException {
		BufferedReader br = null;
		String url = null;
		try {
			br = new BufferedReader(build.getLogReader());
			String strLine;
			Pattern p = Pattern.compile(URL_PATTERN_IN_LOGS);
			while ((strLine = br.readLine()) != null) {
				Matcher match = p.matcher(strLine);
				if (match.matches()) {
					url = match.group(1);
				}
			}
		} finally {
			IOUtils.closeQuietly(br);
		}
		return url;
	}

	protected String getSonarProjectName(String url) throws URISyntaxException {
		//String sonarVersion = getResult("api/server/version");
		URI uri = new URI(url);
		String[] projectUrl;
		try {
			projectUrl = uri.getRawQuery().split("id=");
		} catch (NullPointerException e) {
			projectUrl = uri.getRawPath().split("/");
		}
		return projectUrl.length > 1 ? projectUrl[projectUrl.length - 1] : "";
	}

	public int getLinesOfCode(String url) throws IOException {
		String output = getResult(url);
		JSONObject metricsObjects = JSONObject.fromObject(output);
		int linesOfCodeCount = 0;
		JSONArray array = metricsObjects.getJSONObject("component").getJSONArray("measures");
		for (int i = 0; i < array.size(); i++) {
			JSONObject metricsObject = array.getJSONObject(i);
			if (metricsObject.get("metric").equals("ncloc")) {
				linesOfCodeCount = metricsObject.getInt("value");
			}
		}

		return linesOfCodeCount;
	}

	public int getSonarIssues(String url, String severity) throws IOException {
		String output = getResult(url + severity);
		return JSONObject.fromObject(output).getInt("total");
	}

}
