# InfluxDB v2 Integration

There are two options to authenticate and connect with an InfluxDB v2 target
## Directly, with user name and password 
This method is convenient, but not scalable if many clients access the same InfluxDB instance.
- Create a Jenkins Credential with username and password. 
- Create the same name username and password in InfluxDB with the appropriate privileges to access the target organization and bucket.
- Configure the InfluxDB v2 target using the Jenkins Credential, organization, bucket and data retention policy values.

## Indirectly, through Telegraf plugins
Consider using Telegraf plugins to ingest metrics. They have the distinct advantage to process the stream of metrics emited by potentially numerous clients before they reach the data store. 

They create a pipeline that could authenticate clients, aggregate and filter metrics, add new or remove existing tags, split a stream and other advanced operations to optimize the ingestion, querying and management of data in InfluxDB.  

Telegraf plugins act also as a buffer and decouple the clients from the InfluxDB data store. This in turn could improve the HA, scalability and operational capabilities of InfluxDB service.

- Create a Jenkins Credential with username and password. 
- Create InfluxDB API token with the appropriatee organization and bucket privileges.
- Configure the InfluxDB v2 target using the Jenkins Credential, organization, bucket and data retention policy values. The oganization, bucker and retention policy are not used in this case.
- Configure input `http_listener_v2` plugin for HTTP Basic authentication with the password of the Jenkins Credential. The password is significant but the username is not and it should be commented out. See the example below.
- Configure output `influxdb_v2` plugin to connect to the appropriate InfluxDB v2 instance using url, API token, organization, and bucket settings. Use `namepass` setting to accept only certain measurements and filter out the rest. 
  
# References 
1. Telegraf: https://docs.influxdata.com/telegraf/v1.20/
2. Metrics filtering: https://docs.influxdata.com/telegraf/v1.20/administration/configuration/#metric-filtering
2. A ticket that tracks the work to use API token for InfluxDB v2 authentication:  https://issues.jenkins.io/browse/JENKINS-65830

# Example of Telegraf plugins configuration

1. Create a Telegraf `http_listener_v2` listener for influxdb_plugin API `/api/v2/write` calls using HTTP Basic authentication. Enable HTTPS if desired.
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

2. Create a bucket in InfluxDB v2 and an API token with write privileges. The `influxdb_v2` output plugin will capture the metrics pushed by the `influxdb_plugin`. It is possible to filter metrics by measurements, fields, and tags names. Check Telegraf documentation for more details. 
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