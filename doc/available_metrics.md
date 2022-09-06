# Available Metrics


All measurements share the following metrics:

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| build_number | integer | Build number |  |
| instance | string | Jenkins instance url | 3.4 |
| project_name | string | Build name |  |
| project_namespace | string | Root folder of the project | 3.4 |
| project_path | string | Build path | 1.15 |

All measurements share the following tags:

| Tag | Description | Introduced in |
| --- | --- | --- |
| project_name | Build name |  |
| project_path | Build path | 2.0 |
| * | All values from `JenkinsEnvParameterTag` | 2.1 |




### No extra plugin needed

#### `jenkins_data`

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| build_agent_name | string | Name of the executor node | 1.15 |
| build_branch_name | string | Branch name of multibranch pipeline | 2.4 |
| build_cause | string | Trigger type | 3.4 |
| build_causer | string | Short description of build causer| 2.4 |
| build_exec_time | integer | Start time of the build | 1.17 |
| build_measured_time | integer | Time when InfluxDB plugin is called | 1.17 |
| build_result | string | SUCCESS, FAILURE, NOT BUILT, UNSTABLE, ABORTED, ?  | 1.10 |
| build_result_ordinal | integer | 0-5 in order of `build_result`. 5 is only for pipelines if build result is not set manually.  | 1.10 |
| build_scheduled_time | integer | Time when build was scheduled to run | 1.17 |
| build_status_message | string | Status message (stable, back to normal, broken since #50, etc.) | |
| build_successful | boolean | Boolean whether build succeeded | 1.10 |
| build_time | integer | Build execution time |  |
| build_user | string | User who launch the build | 3.4 |
| last_stable_build | integer | Build number of the last stable build (0 if never) | 1.10 |
| last_successful_build | integer | Build number of the last successful build (0 if never) | 1.10 |
| project_build_health | integer | Health score from build | |
| tests_failed | integer | Amount of failed unit tests (from JUnit plugin) | |
| tests_skipped | integer | Amount of skipped unit tests (from JUnit plugin) | |
| tests_total | integer | total amount of unit tests (from JUnit plugin) | |
| time_in_queue | integer | Time build was in queue (from Metrics plugin) | 1.16 |

Tags specific for this measurement:

| Tag | Description | Introduced in |
| --- | --- | --- |
| build_result | Build result | 1.15 |

#### `changelog_data` (since 1.12)

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| affected_paths | string | Comma-separated list of changed files | |
| commit_count | integer | Amount of commits since last change set | |
| commit_messages | string | Comma-separated list of commit messages | |
| culprits | string | Comma-separated list of commit authors | |

#### `junit_data` (since 2.3)

In order to publish data for this measurement, your job needs to set an environment variable
`LOG_JUNIT_RESULTS` to `true`.

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| suite_name | string | Testsuite name | |
| test_name | string | Test function name | Changed in 3.0 |
| test_full_class_name | string | Test fully-qualified class name (`pacakge.ClassName`) | 3.0 |
| pipeline_step | string | Pipeline steps, separated by ` / ` | 3.0 |
| test_status | string | PASSED, SKIPPED, FAILED, FIXED, REGRESSION | |
| test_status_ordinal | integer | 0-4 in order of test_status | |
| test_duration | float | test duration in seconds | |
| test_duration | float | test duration in seconds | 3.1 |
| test_count | long | Test counter; Useful for aggregations. | 3.1 |

Tags specific for this measurement:

| Tag | Description | Introduced in |
| --- | --- | --- |
| suite_name | Testsuite name | |
| test_name | Test function name | Changed in 3.0 |
| test_full_class_name | Test fully-qualified class name (`pacakge.ClassName`) | 3.0 |
| pipeline_step | Pipeline steps, separated by ` / ` | 3.0 |
| test_status | Test result | |

#### `sonarqube_data` (since 1.11)

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| alert_status | string | State of the Quality Gate | 2.4 |
| quality_gate_details | string | Provides which quality gate condition is failing and which is not.| 3.1 |
| blocker_issues | float | Total amount of blocker issues | |
| branch_coverage | float | Branch coverage | 2.2 |
| bugs | float | Total amount of bugs | 2.2 |
| code_smells | float | Total amount of code smells | 2.2 |
| complexity | float | Total amount of complexity | 2.2 |
| coverage | float | Overall coverage | 2.2 |
| critical_issues | float | Total amount of critical issues | |
| display_name | string | Build display name | |
| duplicated_lines_density | float | Percentage of duplicated lines | 2.2 |
| info_issues | float | Total amount of info issues | |
| line_coverage | float | Line coverage | 2.2 |
| lines_of_code | float (integer until 2.2) | Total amount of lines (including comments) | |
| lines_to_cover | float | Total amount of lines to cover (excluding comments) | 2.2 |
| major_issues | float | Total amount of major issues | |
| minor_issues | float | Total amount of minor issues | |
| vulnerabilities | float | Total amount of vulnerabilities | 2.2 |
| sqale_index | float | Technical Debt | 2.4 |
| sqale_debt_ratio | float | Technical Debt Ratio | 2.4 |

#### `agent_data` (since 3.4)
| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| agent_name | string | Name of an agent called by the build |  |
| agent_label | string | Label of an agent called by the build |  |

### Cobertura plugin

#### `cobertura_data`

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| cobertura_branch_coverage_rate | float | Branch coverage percentage |  |
| cobertura_class_coverage_rate | float | Class coverage percentage |  |
| cobertura_line_coverage_rate | float | Line coverage percentage |  |
| cobertura_number_of_classes | float | Amount of classes |  |
| cobertura_number_of_packages | float | Amount of packages |  |
| cobertura_number_of_sourcefiles | float | Amount of source files |  |
| cobertura_package_coverage_rate | float | Package coverage percentage |  |



### JaCoCo plugin

#### `jacoco_data` (since 1.7)

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| jacoco_brach_coverage_rage | float | Branch coverage percentage | |
| jacoco_brach_covered | integer | Amount of branches covered | 2.1 |
| jacoco_brach_missed | integer | Amount of branches missed | 2.1 |
| jacoco_class_coverage_rage | float | Class coverage percentage | |
| jacoco_class_covered | integer | Amount of classes covered | 2.1 |
| jacoco_class_missed | integer | Amount of classes missed | 2.1 |
| jacoco_complexity_coverage_rage | float | Complexity coverage percentage | |
| jacoco_complexity_covered | integer | Amount of complexity covered | 2.1 |
| jacoco_complexity_missed | integer | Amount of complexity missed | 2.1 |
| jacoco_instruction_coverage_rage | float | Instruction coverage percentage | |
| jacoco_instruction_covered | integer | Amount of instructions covered | 2.1 |
| jacoco_instruction_missed | integer | Amount of instructions missed | 2.1 |
| jacoco_line_coverage_rage | float | Line coverage percentage | |
| jacoco_line_covered | integer | Amount of lines covered | 2.1 |
| jacoco_line_missed | integer | Amount of lines missed | 2.1 |
| jacoco_method_coverage_rage | float | Method coverage percentage | |
| jacoco_method_covered | integer | Amount of methods covered | 2.1 |
| jacoco_method_missed | integer | Amount of methods missed | 2.1 |





### Performance plugin

#### `performance_data` (since 1.10.1)

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| 90percentile | integer | Point when 90 percentile was reached | 1.12 |
| average | float | Average performance (total duration / size) | |
| error_count | integer | Amount of failed samples | |
| error_percent | integer | Percentage of failed samples | |
| max | integer | Maximum duration | |
| median | integer | Median duration | 1.12 |
| min | integer | Minimum duration | |
| size | integer | Amount of samples | |
| total_traffic | integer | Total traffic in KB | |




### Performance publisher plugin

#### `perfpublisher_metric` (since 1.13)

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| average | float | Average value | |
| best | float | Best value | |
| metric_name | string | Metric name | |
| worst | float | Worst value | |

#### `perfpublisher_summary` (since 1.13)

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| average_compile_time | float | Average compilation test time | |
| average_execution_time | float | Average execution test time | |
| average_performance_time | float | Average performance test time | |
| best_compile_time_test_name | string | Best compilation time test name | |
| best_compile_time_test_value | float | Best compilation time test value | |
| best_performance_time_test_name | string | Best performance time test name | |
| best_performance_test_value | float | Best performance test value | |
| best_execution_time_test_name | string | Best execution time test name | |
| best_execution_time_test_value | float | Best execution time test value | |
| number_of_executed_tests | integer | Amount of executed tests | |
| number_of_failed_tests | integer | Amount of failed tests | |
| number_of_not_executed_tests | integer | Amount of not executed tests | |
| number_of_passed_tests | integer | Amount of passed tests | |
| number_of_success_tests | integer | Amount of succeeded tests | |
| number_of_tests | integer | Total amount of tests | |
| number_of_true_false_tests | integer | Amount of true-false tests | |
| worst_compile_time_test_name | string | Worst compilation time test name | |
| worst_compile_time_test_value | float | Worst compilation time test value | |
| worst_performance_time_test_name | string | Worst performance time test name | |
| worst_performance_time_test_value | float | Worst performance time test value | |
| worst_execution_time_test_name | string | Worst execution time test name | |
| worst_execution_time_test_value | float | Worst execution time test value | |

#### `perfpublisher_test` (since 1.13)

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| executed | boolean | Was test executed | |
| execution_time | float | Test execution time | |
| compile_time | float | Test compilation time | |
| message | boolean | Test message | |
| performance | float | Test performance | |
| successful | boolean | Was test successful | |
| test_name | string | Test name | |

Tags specific for this measurement:

| Tag | Description | Introduced in |
| --- | --- | --- |
| test_name | Test name | |

#### `perfpublisher_test_metric` (since 1.13)

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| metric_name | string | Test metric name | |
| relevant | boolean | Is metric relevant |  |
| test_name | string | Test name | |
| value | float | Metric value |  |
| unit | string | Metric unit |  |

Tags specific for this measurement:

| Tag | Description | Introduced in |
| --- | --- | --- |
| test_name | Test name | |


### Robot Framework plugin

#### `rf_results`

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| rf_critical_failed | integer | Amount of failed critical tests |  |
| rf_critical_pass_percentage | float | Percentage of passed critical tests |  |
| rf_critical_passed | integer | Amount of passed critical tests |  |
| rf_critical_total | integer | Total amount of critical tests |  |
| rf_duration | integer | Test execution duration |  |
| rf_failed | integer | Amount of failed tests |  |
| rf_pass_percentage | float | Percentage of passed tests |  |
| rf_passed | integer | Amount of passed tests |  |
| rf_suites | integer | Amount of test suites |  |
| rf_skipped | integer | Amount of skipped tests | 3.0 |
| rf_skip_percentage | float | Percentage of skipped tests | 3.0 |
| rf_total | integer | Total amount of tests |  |

#### `suite_result`

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| rf_critical_failed | integer | Amount of failed critical tests |  |
| rf_critical_passed | integer | Amount of passed critical tests |  |
| rf_critical_total | integer | Total amount of critical tests |  |
| rf_duration | integer | Test execution duration |  |
| rf_failed | integer | Amount of failed tests |  |
| rf_passed | integer | Amount of passed tests |  |
| rf_skipped | integer | Amount of skipped tests | 3.0 |
| rf_suite_name | string | Name of the test suite |  |
| rf_testcases | integer | Total amount of tests (including child suites) |  |
| rf_total | integer | Amount of tests in this suite |  |

Tags specific for this measurement:

| Tag | Description | Introduced in |
| --- | --- | --- |
| rf_suite_name | Name of the test suite | 1.20.1 |

#### `tag_point`

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| rf_critical_failed | integer | Amount of failed critical tests |  |
| rf_critical_passed | integer | Amount of passed critical tests |  |
| rf_critical_total | integer | Total amount of critical tests |  |
| rf_duration | integer | Test execution duration |  |
| rf_failed | integer | Amount of failed tests |  |
| rf_passed | integer | Amount of passed tests |  |
| rf_skipped | integer | Amount of skipped tests | 3.0 |
| rf_total | integer | Total amount of tests |  |
| rf_tag_name | string | Test tag name | 1.20.1 |

Tags specific for this measurement:

| Tag | Description | Introduced in |
| --- | --- | --- |
| rf_tag_name | Tag name | 1.20.1 |

#### `testcase_point`

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| rf_critical_failed | integer | 0 or 1 |  |
| rf_critical_passed | integer | 0 or 1 |  |
| rf_duration | integer | Test case execution duration |  |
| rf_failed | integer | 0 or 1 |  |
| rf_name | string | Name of the test case |  |
| rf_passed | integer | 0 or 1 |  |
| rf_skipped | integer | 0 or 1 | 3.0 |
| rf_suite_name | string | Name of the suite of the test case |  |

Tags specific for this measurement:

| Tag | Description | Introduced in |
| --- | --- | --- |
| rf_name | Name of the test case | 1.20.1 |


### Serenity plugin

#### `serenity_data` (since 2.1)

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| serenity_results_average_test_duration | integer | Maximum average duration in milliseconds |  |
| serenity_results_counts_compromised | integer | Amount of compromised results |  |
| serenity_results_counts_error | integer | Amount of error results |  |
| serenity_results_counts_failure | integer | Amount of failed results |  |
| serenity_results_counts_ignored | integer | Amount of ignored results |  |
| serenity_results_counts_pending | integer | Amount of pending results |  |
| serenity_results_counts_skipped | integer | Amount of skipped results |  |
| serenity_results_counts_success | integer | Amount of success results |  |
| serenity_results_counts_total | integer | Total amount of results |  |
| serenity_results_max_test_duration | integer | Maximum test duration in milliseconds |  |
| serenity_results_min_test_duration | integer | Minimum test duration in milliseconds |  |
| serenity_results_percentages_compromised | integer | Percentage of compromised results |  |
| serenity_results_percentages_error | integer | Percentage of error results |  |
| serenity_results_percentages_failure | integer | Percentage of failure results |  |
| serenity_results_percentages_ignored | integer | Percentage of ignored results |  |
| serenity_results_percentages_pending | integer | Percentage of pending results |  |
| serenity_results_percentages_skipped | integer | Percentage of skipped results |  |
| serenity_results_percentages_success | integer | Percentage of success results |  |
| serenity_results_total_clock_duration | integer | Total test clock duration in milliseconds |  |
| serenity_results_total_test_duration | integer | Total test duration in milliseconds |  |
| serenity_tags_* | integer | Amount of tests for each tag |  |

### Metrics plugin

#### `metrics_data` (since 3.4)

| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| blocked_time | long | Milliseconds in the queue because build was blocked. | |
| buildable_time | long | Milliseconds in the queue in a buildable state. | |
| building_time | long | Milliseconds of the builds | |
| executing_time | long | Milliseconds building from when it left the queue until it was finished. | |
| executor_utilization | double | 0-1 percentage of the executor utilization | |
| queue_time | long | Milliseconds when build entered the queue until it left the queue. | |
| subtask_count | int | Amount of subtasks | |
| total_duration | long | Build duration in milliseconds from when it entered the queue until it was finished. | |
| waiting_time | long | Milliseconds in the queue waiting before it could be considered for execution. | |

### Git plugin

#### `git_data` (since 3.4)
| Metric | Type | Description | Introduced in |
| --- | --- | --- | --- |
| git_repository | string | URL of the Git repository used by the build | |
| git_revision | string | SHA-1 of the commit selected | |
| git_reference | string | reference of the branch | |
