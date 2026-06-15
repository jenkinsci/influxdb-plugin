# SonarQube API integration

## Summary
A short guideline on integrating Jenkins with SonarQube and verifying their integration.

The `SonarQubePointGenerator` resolves the SonarQube report (`report-task.txt`) in the following order:

1. **Workspace file scan** — searches recursively under `$WORKSPACE` for the report file.
2. **Archived artifact** — if the workspace is not accessible (e.g. when the publisher runs on the controller while the build ran on a remote agent), the plugin falls back to reading the report from the build's archived artifacts. Archive `report-task.txt` in your pipeline with `archiveArtifacts` to enable this path. This fallback currently relies on local artifact storage and does not work with jenkins S3-backed managers.
3. **Build log parsing** — as a last resort, the plugin scans the console output for SonarQube scanner log lines that contain the required metadata.

The report file is created by the SonarQube scanner and has the following content:
```
file: ${WORKSPACE}/**/sonar/report-task.txt

projectKey=com.tom:sonarqube-jacoco-code-coverage
serverUrl=http://localhost:9000
serverVersion=8.9.3.48735
dashboardUrl=http://localhost:9000/dashboard?id=com.tom%3Asonarqube-jacoco-code-coverage
ceTaskId=AX0vnvr4_QGKX8b7Yz_v
ceTaskUrl=http://localhost:9000/api/ce/task?id=AX0vnvr4_QGKX8b7Yz_v
```

The actual location of the report file in the workspace depends on the build system used - Maven, Gradle, etc.

If, for whatever reason, a report file is created with a different name, the `SONARQUBE_BUILD_REPORT_NAME` env var
could be used to specify either the file name or the path pattern ending with the file name.

Examples:
```
  stage("InfluxDB v2 publisher") {
    environment {
        SONARQUBE_BUILD_REPORT_NAME="custom-report.txt"
        # SONARQUBE_BUILD_REPORT_NAME="path/custom-report.txt"
    }

    steps {
        withSonarQubeEnv('SonarQube') {
            influxDbPublisher(selectedTarget: 'influxdb_v2',)
        }
    }
  }
```

The information extracted from this file is used to query SonarQube for issues, measures, and task status.

# References
1. [CloudBees Video on SonarQube integration with Jenkins](https://www.youtube.com/watch?v=KsTMy0920go)
2. SonarQube WEB API: https://sonarcloud.io/web_api/

## Verification

The plugin uses the following API calls:

1. [Issues Search](https://sonarcloud.io/web_api/api/issues/search)
2. [Measures Search History](https://sonarcloud.io/web_api/api/measures/search_history)
3. [CE Task Status](https://sonarcloud.io/web_api/api/ce/task)

After setting up a security token in SonarQube, use the following curl commands to verify the API integration manually.

### Measures Search History
export TOKEN=****************
curl -s -G -u ${TOKEN}: \
--data-urlencode "componentKey=com.tom:sonarqube-jacoco-code-coverage" \
--data-urlencode "component=com.tom:sonarqube-jacoco-code-coverage" \
--data-urlencode "metricKeys=ncloc" \
http://localhost:9000/api/measures/component | jq .
```
{
  "component": {
    "key": "com.tom:sonarqube-jacoco-code-coverage",
    "name": "sonarqube-jacoco-code-coverage",
    "qualifier": "TRK",
    "measures": [
      {
        "metric": "ncloc",
        "value": "9"
      }
    ]
  }
}
```

### Issues Search
export TOKEN=****************
curl -s -G -u ${TOKEN}: \
--data-urlencode "componentKeys=com.tom:sonarqube-jacoco-code-coverage" \
--data-urlencode "resolved=false" \
--data-urlencode "severities=INFO" \
http://localhost:9000/api/issues/search?ps=1 | jq .

```
{
  "total": 0,
  "p": 1,
  "ps": 1,
  "paging": {
    "pageIndex": 1,
    "pageSize": 1,
    "total": 0
  },
  "effortTotal": 0,
  "issues": [],
  "components": [],
  "facets": []
}
```

### Task status
export TOKEN=****************
curl -s -G -u ${TOKEN}: \
--data-urlencode "id=com.tom:sonarqube-jacoco-code-coverage" \
http://localhost:9000/api/ce/task?id=AX0vnvr4_QGKX8b7Yz_v | jq .
```
{
  "task": {
    "id": "AX0vnvr4_QGKX8b7Yz_v",
    "type": "REPORT",
    "componentId": "AX0m-2Bde0ytHUET2DEl",
    "componentKey": "com.tom:sonarqube-jacoco-code-coverage",
    "componentName": "sonarqube-jacoco-code-coverage",
    "componentQualifier": "TRK",
    "analysisId": "AX0vnv7gkMePnxoN1IDV",
    "status": "SUCCESS",
    "submittedAt": "2021-11-17T20:38:07+0000",
    "submitterLogin": "admin",
    "startedAt": "2021-11-17T20:38:08+0000",
    "executedAt": "2021-11-17T20:38:22+0000",
    "executionTimeMs": 13679,
    "hasScannerContext": true,
    "warningCount": 0,
    "warnings": []
  }
}
```

# A simple pipeline for testing
```
pipeline {
    agent any

    stages {
        stage('Clone sources') {
            steps {
                git url: 'https://github.com/dgeorgievski/sonarqube-jacoco-code-coverage.git'
            }
        }

        stage('Build') {
            steps {
                sh './gradlew clean test build'
                junit 'build/test-results/test/*.xml'
                step( [ $class: 'JacocoPublisher' ] )
            }
        }

        stage('SonarQube analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh "./gradlew sonarqube -PsonarToken=****************"
                }
            }
        }

        stage("Quality gate") {
            steps {
                waitForQualityGate abortPipeline: true
            }
        }

        stage("InfluxDB v2 publisher") {
            environment {
                LOG_JUNIT_RESULTS="true"
                INFLUXDB_PLUGIN_CUSTOM_PROJECT_NAME = "foo"
            }

            steps {
                withSonarQubeEnv('SonarQube') {
                    influxDbPublisher(selectedTarget: 'influxdb_v2',)
                }
            }
        }

    }
}
```