name := """metrics-dashboard"""

version := "1.0"

scalaVersion := "2.11.1"

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

resolvers += "spray repo" at "http://repo.spray.io"

val novusRels = "repo.novus rels" at "http://repo.novus.com/releases/"
val novusSnaps = "repo.novus snaps" at "http://repo.novus.com/snapshots/"
val salat = "com.novus" %% "salat-core" % "0.0.7" // latest version at time of this writing

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "org.json4s" %% "json4s-jackson" % "3.2.10",
  "org.eclipse.mylyn.github" % "org.eclipse.egit.github.core" % "2.1.5",
  "com.novus" %% "salat" % "1.9.9"
)
