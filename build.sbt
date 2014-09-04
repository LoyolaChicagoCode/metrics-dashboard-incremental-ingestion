import play.PlayScala

name := """metrics-dashboard"""

version := "1.0"

//scalaVersion := "2.11.1"
lazy val root = (project in file(".")).enablePlugins(PlayScala)

//libraryDependencies ++= Seq(
//  jdbc,
//  anorm,
//  cache,
//  ws
//)

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "org.json4s" %% "json4s-jackson" % "3.2.10"
)
