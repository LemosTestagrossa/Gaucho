import sbt.Keys._
import sbt._

object Settings extends Dependencies with CommonScalac {

  lazy val globalResources = file("./common/src/main/resources")
  unmanagedResourceDirectories in Compile += globalResources

  val modulesSettings = Seq(
    scalacOptions ++= scalacSettings,
    scalaVersion := scalaVersionUsed,
    resolvers ++= commonResolvers,
    libraryDependencies ++= mainDeps,
    libraryDependencies ++= testDeps map (_ % Test)
  )

  lazy val scalaFmtSettings = Seq(
    //scalafmtOnCompile := true,
    //scalafmtConfig := file(".scalafmt.conf")
  )

  lazy val testSettings = Seq(
    Test / parallelExecution := true,
    Test / fork := true,
    Test / javaOptions += "-Xmx2G",
    triggeredMessage := Watched.clearWhenTriggered,
    autoStartServer := false
  )

  lazy val mainSettings = Seq(
    fork := true,
    fork in run := true,
    fork in Test := true,
    mainClass in (Compile, run) := Some("Main")
  )

  scalacOptions ++= Seq(
      "-feature",
      "-unchecked",
      "-language:higherKinds",
      "-language:postfixOps",
      "-deprecation"
    ) ++ scalacSettings
}
