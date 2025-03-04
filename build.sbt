name := """flight-backend"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.15"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.2"
libraryDependencies += "com.google.guava" % "guava" % "32.0.1-jre"
libraryDependencies += filters

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.10" % Test,
  "org.mockito" %% "mockito-scala" % "1.17.30" % Test,
  "org.mockito" % "mockito-core" % "5.10.0" % Test,
  
  // AWS SDK v2 for DynamoDB
  "software.amazon.awssdk" % "dynamodb" % "2.20.162",
  
  // Scanamo for better Scala-DynamoDB integration
  "org.scanamo" %% "scanamo" % "1.0.0-M23"
)

ThisBuild / scalafmtOnCompile := true

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
