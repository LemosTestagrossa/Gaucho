import Settings._

lazy val commonSettings = Seq(
  organization in ThisBuild := "LemosTestagrossa",
  name := "Gaucho",
  version := "0.0.1",
  scalaVersion := Dependencies.scalaVersion
)

lazy val publishSettings = Seq(
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  bintrayOrganization := Some("lemostestagrossa"),
  bintrayRepository := "Gaucho",
  bintrayPackageLabels := Seq("akka")
)

// This is an example.  sbt-bintray requires licenses to be specified
// (using a canonical name).
ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayOrganization := Some("lemostestagrossa")
bintrayReleaseOnPublish in ThisBuild := false

lazy val root = (project in file("."))
  .settings(
    publishSettings
  )
  .settings(commonSettings)
  .settings(modulesSettings)
  .settings(mainSettings)
  .settings(testSettings)
  .settings(scalaFmtSettings)
