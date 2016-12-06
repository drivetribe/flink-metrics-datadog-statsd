resolvers += "Sonatype Repository" at "https://oss.sonatype.org/content/groups/public"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.2.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.4")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("org.wartremover" % "sbt-wartremover" % "1.2.1")

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15")

addSbtPlugin("io.gatling" % "gatling-sbt" % "2.2.0")

addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "0.4.10")
