import Settings._

lazy val commonSettings = Seq(
  scalaVersion := Dependencies.scalaVersion
)

lazy val publishSettings = Seq(
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  bintrayOrganization := Some("lemostestagrossa"),
  bintrayRepository := "Gaucho",
  version := "0.0.9834",
  bintrayPackageLabels := Seq("akka")
)

// This is an example.  sbt-bintray requires licenses to be specified
// (using a canonical name).
ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayOrganization := Some("lemostestagrossa")
publishMavenStyle := true
bintrayReleaseOnPublish in ThisBuild := true

lazy val gaucho = (project in file("."))
  .settings(
    publishSettings
  )
  .settings(commonSettings)
  .settings(modulesSettings)
  .settings(mainSettings)
  .settings(testSettings)
  .settings(scalaFmtSettings)
