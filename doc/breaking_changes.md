# Breaking Changes

## 3.0

- "username" and "password" are no longer used when defining new InfluxDB Targets.
Credentials are used instead. Please check your InfluxDB Target configurations from
Manage Jenkins --> Configure System.
  - All pipelines that create a new Target inside
     the pipeline need to be modified so, that they use `target.credentialsId` instead of
     `target.username` and `target.password`.
  - JCasC configurations need to be modified, so that they use `credentialsId` instead of `username`
      and `password`.
- JUnit names changed from name to fullname. Test names now include the package and class name of the test.

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