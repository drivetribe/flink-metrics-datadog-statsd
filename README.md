# Datadog reporter for Flink metrics

Implementation of a metrics reporter that uses the StatsD protocol with Datadog extensions for tagging to push metrics to the Datadog agent.

## Requirements

* The datadog agent is running on some host
* The jar produced by `sbt package` is in the classpath of the Flink runner (e.g. it is copied in the `flink/lib` folder)
* To configure the reporter in `flink-conf.yaml`

## Configuration

An example configuration assuming that the datadog agent is running on all the cluster nodes on the default port:

    metrics.scope.jm: <host>.jobmanager.nil.nil.nil.nil.nil
    metrics.scope.jm.job: <host>.jobmanager.<job_name>.nil.nil.nil.nil
    metrics.scope.tm: <host>.taskmanager.<tm_id>.nil.nil.nil.nil
    metrics.scope.tm.job: <host>.taskmanager.<tm_id>.<job_name>.nil.nil.nil
    metrics.scope.task: <host>.taskmanager.<tm_id>.<job_name>.<task_name>.nil.<subtask_index>
    metrics.scope.operator: <host>.taskmanager.<tm_id>.<job_name>.<task_name>.<operator_name>.<subtask_index>

    metrics.reporters: statsd_datadog
    metrics.reporter.statsd_datadog.class: io.chumps.flink.FlinkDatadogStatsDReporter
    metrics.reporter.statsd_datadog.pattern: host.type.task_manager.job.task.operator.subtask_index

The `pattern` property extracts segments of the metric label to tags in Datadog; any non matched bits at the end will be the name of the metric. If `pattern` is not provided the default behaviour is to name the metrics `metric` and tag them with the whole label.

Other properties that can be configured are:

* `host` the host where the Datadog agent is running (default `localhost`)
* `port` the port that the Datadog agent is listening to for StatsD datagrams (default `8125`)
* `prefix` the prefix for all the metric names being published (default `flink`)
