lazy val scalaVersionString = "2.12.7"

lazy val flinkVersion = "1.7.0"

lazy val `flink-metrics-datadog-statsd` = (project in file("."))
  .settings(
    scalaVersion := scalaVersionString,
    version := "0.2",
    libraryDependencies ++= Seq(
      "org.apache.flink" % "flink-metrics-core" % flinkVersion,
      "org.scalatest" %% "scalatest" % "3.0.1" % "test"
    )
  )
