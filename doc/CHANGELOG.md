# Changelog

## Newer Versions
See [GitHub Releases](https://github.com/jenkinsci/influxdb-plugin/releases).

## 2.0.1
29.8.2019

-   Fix initialization in pipelines
    ([JENKINS-59104](https://issues.jenkins-ci.org/browse/JENKINS-59104))

## 2.0
23.8.2019

-   Add project path as a tag
    ([Pull Request 65](https://github.com/jenkinsci/influxdb-plugin/pull/65))
-   Fix handling environment variables in pipelines
    ([JENKINS-47776](https://issues.jenkins-ci.org/browse/JENKINS-47776)
    / [Pull Request 74](https://github.com/jenkinsci/influxdb-plugin/pull/74))

    This causes a backwards incompatibility issue with Configuration As Code:
    the configuration needs to be changed from `influxDbPublisher` to `influxDbGlobalConfig`.

    Might cause issues when creating new targets in pipelines.
    The `InfluxDbPublisher` instance is now under jenkinsci.plugins.influxdb.**InfluxDbStep**.DescriptorImpl.

-   Fix empty `build_agent_name` field value
    ([JENKINS-58945](https://issues.jenkins-ci.org/browse/JENKINS-58945))
-   Minor fixes and improvements
    ([Pull Request 63](https://github.com/jenkinsci/influxdb-plugin/pull/63),
    [Pull Request 64](https://github.com/jenkinsci/influxdb-plugin/pull/64),
    [Pull Request 66](https://github.com/jenkinsci/influxdb-plugin/pull/66),
    [Pull Request 67](https://github.com/jenkinsci/influxdb-plugin/pull/67),
    [Pull Request 68](https://github.com/jenkinsci/influxdb-plugin/pull/68),
    [Pull Request 69](https://github.com/jenkinsci/influxdb-plugin/pull/69),
    [Pull Request 70](https://github.com/jenkinsci/influxdb-plugin/pull/70),
    [Pull Request 71](https://github.com/jenkinsci/influxdb-plugin/pull/71),
    [Pull Request 72](https://github.com/jenkinsci/influxdb-plugin/pull/72))

## 1.23
28.6.2019

-   Re-enable Jenkins proxy usage
    ([Pull Request 61](https://github.com/jenkinsci/influxdb-plugin/pull/61))
-   Code quality improvements
    ([Pull Request 62](https://github.com/jenkinsci/influxdb-plugin/pull/62))
-   Expand environment variables for custom prefix
    ([JENKINS-50167](https://issues.jenkins-ci.org/browse/JENKINS-50167))
-   Don't add "\_" to `project_name` tag value if custom prefix is `null`
    ([JENKINS-57944](https://issues.jenkins-ci.org/browse/JENKINS-57944))

## 1.22
31.5.2019

-   Fix [security issue](https://jenkins.io/security/advisory/2019-05-31/#SECURITY-1403)

## 1.21
16.5.2019

-   Allow plugin to be used in pipelines with cleaner syntax `influxDbPublisher()`
    ([Pull Request 54](https://github.com/jenkinsci/influxdb-plugin/pull/54))
-   Improvements to code quality, updated dependencies to other plugins
    ([Pull Request 55](https://github.com/jenkinsci/influxdb-plugin/pull/55))
-   Add support for configuration-as-code
    ([JENKINS-53950](https://issues.jenkins-ci.org/browse/JENKINS-53950)
    / [Pull Request 57](https://github.com/jenkinsci/influxdb-plugin/pull/57))
-   Add help texts to configuration
    ([JENKINS-47307](https://issues.jenkins-ci.org/browse/JENKINS-47307)
    / [Pull Request 58](https://github.com/jenkinsci/influxdb-plugin/pull/58))

## 1.20.4
20.2.2019

-   Re-continue support for older SonarQube versions
    ([JENKINS-56038](https://issues.jenkins-ci.org/browse/JENKINS-56038))

## 1.20.3
5.2.2019

-   InfluxDbPublisher: avoid `NullPointerException` on empty targets
    ([Pull Request 53](https://github.com/jenkinsci/influxdb-plugin/pull/53)
    / [JENKINS-55594](https://issues.jenkins-ci.org/browse/JENKINS-55594))
-   Fix writing usernames and passwords to be logged by InfluxDB when data is written
    ([JENKINS-55823](https://issues.jenkins-ci.org/browse/JENKINS-55823))

## 1.20.2
14.12.2018

-   Fix support for SonarQube 7.4 and discontinue support for lower SonarQube versions
    ([JENKINS-55009](https://issues.jenkins-ci.org/browse/JENKINS-55009))
-   Fix calculating build time in custom data points
    ([Pull Request 52](https://github.com/jenkinsci/influxdb-plugin/pull/52))
-   Improved logging if plugin fails to connect to SonarQube
    ([JENKINS-55032](https://issues.jenkins-ci.org/browse/JENKINS-55032))

## 1.20.1
29.11.2018

-   Don't change dashes to underscores in tag value
    ([JENKINS-50575](https://issues.jenkins-ci.org/browse/JENKINS-50575))
-   Add tags for `suite_result`, `testcase_point`, and `tag_point`
    ([JENKINS-51699](https://issues.jenkins-ci.org/browse/JENKINS-51699))
-   Fix adding multiple targets
    ([JENKINS-54595](https://issues.jenkins-ci.org/browse/JENKINS-54595))
-   Check `null` values in SonarQube URL
    ([JENKINS-54560](https://issues.jenkins-ci.org/browse/JENKINS-54560))

## 1.20
4.10.2018

-   Fix for `NullPointerException` in `CustomPointDataGenerator`
    ([Pull Request 47](https://github.com/jenkinsci/influxdb-plugin/pull/47))
-   Allow targets to be set as global listeners
    ([Pull Request 49](https://github.com/jenkinsci/influxdb-plugin/pull/49))
-   Reduce verbosity in Jenkins system logs
    ([Pull Request 50](https://github.com/jenkinsci/influxdb-plugin/pull/50))
-   Fix losing targets after Jenkins restart
    ([JENKINS-53861](https://issues.jenkins-ci.org/browse/JENKINS-53861))

## 1.19
5.9.2018

-   Set InfluxDB server detail from groovy scripts
    ([Pull Request 35](https://github.com/jenkinsci/influxdb-plugin/pull/35),
    [Pull Request 46](https://github.com/jenkinsci/influxdb-plugin/pull/46),
    [JENKINS-50962](https://issues.jenkins-ci.org/browse/JENKINS-50962))
-   Add possibility to configure the scheduled job time as InfluxDB points timestamp
    ([Pull Request 48](https://github.com/jenkinsci/influxdb-plugin/pull/48))
-   Use SONAR\_HOST\_URL environment variable if possible for SonarQube server
    ([JENKINS-49799](https://issues.jenkins-ci.org/browse/JENKINS-49799))

## 1.18
15.8.2018

-   Add `test_name` tag so that the 'group by' clause can be used for perfpublisher results
    ([Pull Request 39](https://github.com/jenkinsci/influxdb-plugin/pull/39))
-   Add a possibility to change measurement `jenkins_data` to a custom name in pipelines
    ([Pull request 40](https://github.com/jenkinsci/influxdb-plugin/pull/40))
-   Add additional log output for skipped point generators
    ([Pull Request 41](https://github.com/jenkinsci/influxdb-plugin/pull/41))
-   Set a consistent timestamp for all point generation
    ([Pull Request 42](https://github.com/jenkinsci/influxdb-plugin/pull/42))
-   Fix bug preventing setting target in pipeline step
    ([Pull Request 43](https://github.com/jenkinsci/influxdb-plugin/pull/43))

## 1.17
29.6.2018

-   Fixed an issue with Jenkins log being cluttered with warning messages
    ([JENKINS-49105](https://issues.jenkins-ci.org/browse/JENKINS-49105))
-   Changed minimum required Cobertura Plugin to get rid of warnings when creating a maven package
-   Enable the use of environment variables as fields or tags for `jenkins_data`
    ([Pull Request 32](https://github.com/jenkinsci/influxdb-plugin/pull/32))
-   Add `build_scheduled_time`, `build_exec_time`, and `build_measured_time` as fields in `JenkinsPointGenerator`
    ([Pull Request 38](https://github.com/jenkinsci/influxdb-plugin/pull/38))

## 1.16
4.6.2018

-   Fixed issue with `null` custom data map tags
    ([JENKINS-51389](https://issues.jenkins-ci.org/browse/JENKINS-51389))
-   Added "Time in queue" metric if plugin is available
    ([Pull request 29](https://github.com/jenkinsci/influxdb-plugin/pull/29))
-   Fixed SonarQube data collection if project name has slashes
    ([JENKINS-50763](https://issues.jenkins-ci.org/browse/JENKINS-50763)
    / [Pull request 30](https://github.com/jenkinsci/influxdb-plugin/pull/30))

## 1.15
11.5.2018

-   Allow to use Jenkins global proxy when connecting to InfluxDB server
    ([Pull request 23](https://github.com/jenkinsci/influxdb-plugin/pull/23))
-   Added project path and and agent name to `jenkins_data`
    ([Pull request 26](https://github.com/jenkinsci/influxdb-plugin/pull/26))
-   Added support for custom tags
    ([Pull request 28](https://github.com/jenkinsci/influxdb-plugin/pull/28))

## 1.14
13.2.2018

-   Added SonarQube authentication support
    ([JENKINS-47776](https://issues.jenkins-ci.org/browse/JENKINS-47776))
-   Support for custom project name
    ([Pull request 24](https://github.com/jenkinsci/influxdb-plugin/pull/24))
-   Fixed pipeline support if user doesn't have Robot Framework plugin
    ([JENKINS-49308](https://issues.jenkins-ci.org/browse/JENKINS-49308))

## 1.13.2
23.1.2018

-   Fixed Sonarqube data fetching
    ([JENKINS-48858](https://issues.jenkins-ci.org/browse/JENKINS-48858))

## 1.13.1
20.12.2017

-   Fix for running pipelines without all optional plugins installed
    ([Pull request 25](https://github.com/jenkinsci/influxdb-plugin/pull/25))

## 1.13
20.12.2017

-   Fix for empty or `null` usernames
    ([Pull request 19](https://github.com/jenkinsci/influxdb-plugin/pull/19))
-   Ignore resolved issues from Sonarqube scan
    ([Pull request 20](https://github.com/jenkinsci/influxdb-plugin/pull/20))
-   Perfpublisher plugin support
    ([Pull request 21](https://github.com/jenkinsci/influxdb-plugin/pull/21))
-   Fixed insertion with empty customPrefix
    ([Pull request 22](https://github.com/jenkinsci/influxdb-plugin/pull/22))
-   Added help texts for global configuration page
    ([Issue 47307](https://issues.jenkins-ci.org/browse/JENKINS-47307))
-   Changed other plugins to be optional dependencies

## 1.12.3
30.6.2017

-   Fixed SonarQube integration
    ([Pull request 17](https://github.com/jenkinsci/influxdb-plugin/pull/17))
-   Travis CI integration
    ([Pull request 15](https://github.com/jenkinsci/influxdb-plugin/pull/15))

## 1.12.2
21.6.2017

-   Changed consistency level from ALL to ANY
    ([Issue 44840](https://issues.jenkins-ci.org/browse/JENKINS-44840))
-   Changed default retention policy to "autogen"
    ([Pull request 16](https://github.com/jenkinsci/influxdb-plugin/pull/16))
-   Temporarily disabled SonarQube integration due to multiple errors

## 1.12.1
26.5.2017

-   Fixed Performance Plugin support for latest version
    ([Issue 43539](https://issues.jenkins-ci.org/browse/JENKINS-43539))

## 1.12
15.5.2017

-   Added 90-percentile for `performance_data` measurement
    ([Pull Request 12](https://github.com/jenkinsci/influxdb-plugin/pull/12))
-   Fixed Influx-Java version bump
    ([Pull request 13](https://github.com/jenkinsci/influxdb-plugin/pull/13))
-   Added measurement `changelog_data`
    ([Pull request 14](https://github.com/jenkinsci/influxdb-plugin/pull/14))
-   Fixed plugin crash if data generation fails
    ([Jenkins-44011](https://issues.jenkins-ci.org/browse/JENKINS-44011))

## 1.11
22.3.2017

-   Updated pipeline usage
-   Added SonarQube integration
    ([Pull request 11](https://github.com/jenkinsci/influxdb-plugin/pull/11))

## 1.10.3
10.2.2017

-   `rf_tag_name` added to `tag_point`

## 1.10.2
17.1.2017

-   Fixed an issue with pipelines causing a `NullPointerException` for several metrics
    ([JENKINS-41067](https://issues.jenkins-ci.org/browse/JENKINS-41067))

## 1.10.1
30.12.2016

-   Changed performance measurement name from `<unit test file>` to `performance_data`

## 1.10
23.12.2016

-   Exceptions are now ignorable
    ([Pull request 3](https://github.com/jenkinsci/influxdb-plugin/pull/3))
-   New fields for `jenkins_data`:
    build\_result, build\_result\_ordinal, build\_successful, last\_stable\_build, last\_successful\_build
    ([Pull request 10](https://github.com/jenkinsci/influxdb-plugin/pull/10))

## 1.9
8.11.2016

-   Added support for custom data maps
    ([Pull request 8](https://github.com/jenkinsci/influxdb-plugin/pull/8))
-   Added support for custom data
    ([Pull request 7](https://github.com/jenkinsci/influxdb-plugin/pull/7))
-   Removed `build_<job_name_with_dashes_changed_to_underscores>` measurement as obsolete
-   Added support for Performance plugin
    ([JENKINS-38298\\](https://issues.jenkins-ci.org/browse/JENKINS-38298))

## 1.8.1
28.9.2016

-   Added custom prefixes
    ([Pull request 6](https://github.com/jenkinsci/influxdb-plugin/pull/6))

## 1.8
13.9.2016

-   Changed Cobertura integration to get data from
    [Cobertura Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Cobertura+Plugin)
-   Metrics can now be grouped by project name
    ([Pull request 4](https://github.com/jenkinsci/influxdb-plugin/pull/4))

## 1.7
1.9.2016

-   Added JaCoCo support
-   Retention Policy is now configurable in global settings
-   Metrics are now sent as a bulk, instead of sent separately
    ([Pull request 2](https://github.com/jenkinsci/influxdb-plugin/pull/2))

## 1.6
10.8.2016

-   Fixed issue with Cobertura report files on agents were not found.

## 1.5
5.8.2016

-   Fixed issue with selected target not stored correctly in configuration

## 1.4
4.8.2016

-   Support for pipelines
-   Fixed issue with multiple targets with same url, but different database
-   Fixed issue with Cobertura reports in other than default location

## 1.3
26.7.2016

-   First release available from the update center.
