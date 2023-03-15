# Breaking Changes

## 3.0

- InfluxDB 1.7 and lower are no longer supported. Only supported 1.x version is 1.8.x.
- "username" and "password" are no longer used when defining new InfluxDB Targets.
Credentials are used instead. Please check your InfluxDB Target configurations from
Manage Jenkins --> Configure System.
  - All pipelines that create a new Target inside
     the pipeline need to be modified so, that they use `target.credentialsId` instead of
     `target.username` and `target.password`.
  - JCasC configurations need to be modified, so that they use `credentialsId` instead of `username`
      and `password`.
- JUnit `test_name` field/tag changed to remove pipeline name. If your pipeline had multiple `junit` steps, 
the pipeline step information is now recorded in the `pipeline_step` field/tag. For example:
  - Before <br>`test_name`: `Tests / Test Stage 1 / my_test_name`
  - After  <br>`test_name`: `my_test_name` <br> `test_full_class_name`: `mypackage.MyClass` <br> `pipeline_step`: `Tests / Test Stage 1`


## 2.0

- From version 2.0 onwards `selectedTarget` is a **mandatory** parameter
for pipelines and the `target` parameter is no longer supported.
- Configuration As Code: the configuration needs to be changed from
`influxDbPublisher` to `influxDbGlobalConfig`.
- Might cause issues when creating new targets in pipelines. The
`InfluxDbPublisher` instance is now
under jenkinsci.plugins.influxdb.**InfluxDbStep**.DescriptorImpl.

## 1.13

From version 1.13 onwards different plugins are listed as optional
dependencies. In order to get rid of mandatory dependency errors,
the InfluxDB plugin must be re-installed.