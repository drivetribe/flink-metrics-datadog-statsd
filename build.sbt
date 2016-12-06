lazy val scalaVersionString = "2.11.8"

lazy val flinkVersion = "1.1.3"

lazy val `flink-metrics-datadog-statsd` = (project in file("."))
  .settings(
    scalaVersion := scalaVersionString,
    version := "0.1",
    libraryDependencies ++= Seq(
      "org.apache.flink" % "flink-metrics-core" % flinkVersion,
      "org.scalatest" %% "scalatest" % "2.2.2" % "test"
    )
  )
