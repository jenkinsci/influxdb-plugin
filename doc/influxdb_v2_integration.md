# InfluxDB v2 Integration

The current plugin version uses InfluxDB v1.x authentication semantics (user name and password, and a database), while v2 is using API token, bucket, and organization to regulate the access to the data. 

The current Jenkins InfluxDB Target configuration requires the following: 
- A Jenkins Credential with username and password, instead of a Secret Text that would better store the ImfluxDB v2 token. 
- InfluxDB v2 settings: bucket, org, and a secret. If the org is not specified the plugin creates an InfluxDB v1 client. 
- Performs HTTP Basic authentication with the provided username and password credentials. The password is significant but the username is not which the Basic authentication scheme used by InfluxDB v2 client, which results in an authentication failure. 

The solution is to use InfluxDB Telegraf http listener that serves as a proxy between the influxdb_plugin and InfluxDB v2 instance. The listener can authenticate the influxdb_plugin client using only the password with a Basic schema, and stream the metrics to InfluxDB v2 with the help of influxdb_v2 output plugin, that in turn could perform filtering on the measurements and tags. 

## Reference 
1. Telegraf: https://docs.influxdata.com/telegraf/v1.20/
2. Metrics filtering: https://docs.influxdata.com/telegraf/v1.20/administration/configuration/#metric-filtering
2. A ticket that tracks the work to use API token for InfluxDB v2 authentication:  https://issues.jenkins.io/browse/JENKINS-65830

## Workaround'
With the help of Telegraf input and output plugins it is possible to bridge this gap in version inconsistencies.

1. Create a username and password Credential in Jenkins.
   The username is not significant and disregarded by the InfluxDB v2 client, while the password is, and it used by the Telegraf http_listener_v2 listener to authenticate the influxdb_plugin client.

2. Create a Telegraf `http_listener_v2` listener for influxdb_plugin API `/api/v2/write` calls using HTTP Basic authentication. Enable HTTPS if desired.
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

Create a bucket in InfluxDB v2 and a token with write privileges. The `influxdb_v2` output plugin will capture the metrics pushed by the influxdb_plugin. It is possible to filter metrics by measurements, fields, and tags names. Check Telegraf documentation for more details. 
```
## Forward jenkins_custom_data provided by influxdb plugin
[[outputs.influxdb_v2]]

  urls = ["http://127.0.0.1:8086"]

  ## A Token for authentication with InfluxDB v2 servers defined in urls setting.
  token = "*******************************"

  ## Organization is the name of the organization you wish to write to; must exist.
  organization = "my-org"

  ## Destination bucket to write into.
  bucket = "quality-metrics"

  # Filters for measurements
  namepass = ["junit_data", "sonarqube_data", "jacoco_data", "jenkins_test"]

  # accept only certain fields
  # https://github.com/jenkinsci/influxdb-plugin/blob/development/doc/available_metrics.md
  fieldpass = ["test_status", "jacoco_line_*", "*_issues", "*coverage", "lines_*", "vulnerabilities"]

  influx_uint_support = true
```