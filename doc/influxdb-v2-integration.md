# InfluxDB v2 Integration

The current plugin version uses InfluxDB v1.x authentication semantics (user name and password, and a database), while v2 is using APi token, bucket, and organization to regulate the acceess to the database.

The InfluxDB Target configuration options deceptively indicate InfluDB v2 options, but it looks like the plugin still expects to connect to InfluxDB v1.x. See this ticket for more details https://issues.jenkins.io/browse/JENKINS-65830

## Reference 
1. https://docs.influxdata.com/telegraf/v1.20/

## Workaround'
With the help of Telegraf input and output plugins it is possible to bridge this gap in version inconsistencies.

Create a listener for influxdb-plugin calls.
```
[[inputs.http_listener_v2]]
  ## Address and port to host HTTP listener on
  service_address = ":8186"

  ## maximum duration before timing out read of the request
  read_timeout = "10s"
  ## maximum duration before timing out write of the response
  write_timeout = "10s"

  ## Maximum allowed HTTP request body size in bytes.
  ## 0 means to use the default of 32MiB.
  max_body_size = 0

  # http v2
  path = "/api/v2/write"
  data_source = "body"
  data_format = "influx"

  ## Set one or more allowed client CA certificate file names to
  ## enable mutually authenticated TLS connections
  # tls_allowed_cacerts = ["/etc/telegraf/clientca.pem"]

  ## Add service certificate and key
  # tls_cert = "/etc/telegraf/cert.pem"
  # tls_key = "/etc/telegraf/key.pem"

  ## Optional tag name used to store the database name.
  ## If the write has a database in the query string then it will be kept in this tag name.
  ## This tag can be used in downstream outputs.
  ## The default value of nothing means it will be off and the database will not be recorded.
  ## If you have a tag that is the same as the one specified below, and supply a database,
  ## the tag will be overwritten with the database supplied.
  #database_tag = "db"

  # Basic authentication is used in which the user name is discarded.
  ##basic_username = "foobar"
  basic_password = "bar100foo"
```

Create a bucket in InfluxDB and a token with write privileges. The `influxdb_v2` output plugin will capture the
metrics pushed by the influxdb-lugin - regulated by `namepass` and `fieldpass` settings.
```
## Forward jenkins_custom_data provided by influxdb plugin
## supporting currently v1.8 only
[[outputs.influxdb_v2]]

  urls = ["http://127.0.0.1:8086"]

  ## Token for authentication.
  token = "*******************************"

  ## Organization is the name of the organization you wish to write to; must exist.
  organization = "my-org"

  ## Destination bucket to write into.
  bucket = "quality-metrics"

  # accepts only github metrics
  namepass = ["junit_data", "sonarqube_data", "jacoco_data", "jenkins_test"]

  # accept only certain fields
  # https://github.com/jenkinsci/influxdb-plugin/blob/development/doc/available_metrics.md
  fieldpass = ["test_status", "jacoco_line_*", "*_issues", "*coverage", "lines_*", "vulnerabilities"]

  influx_uint_support = true
```