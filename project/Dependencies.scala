import sbt.Keys.libraryDependencies
import sbt._

object Dependencies {
  // Versions
  lazy val scalaVersion = "2.13.1"
  private lazy val akkaVersion = "2.6.6"

  // Resolvers
  lazy val commonResolvers = Seq(
    Resolver sonatypeRepo "public",
    Resolver typesafeRepo "releases"
  )

  // Modules
  trait Module {
    def modules: Seq[ModuleID]
  }

  object Test extends Module {
    private lazy val scalaTestVersion = "3.1.0"
    private lazy val scalaCheckVersion = "1.14.0"

    private lazy val scalaTic = "org.scalactic" %% "scalactic" % scalaTestVersion
    private lazy val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion
    private lazy val specs2 = "org.specs2" %% "specs2-core" % "4.10.5"
    private lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % scalaCheckVersion
    private lazy val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
    private lazy val akkaTypedTestKit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion
    private lazy val akkaStreamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion
    private lazy val kafkaTestKit = "com.typesafe.akka" %% "akka-stream-kafka-testkit" % "2.0.0-RC1"

    private lazy val kafkaVersion = "2.4.0"

    override def modules: Seq[ModuleID] =
      scalaTest :: scalaTic :: scalaCheck :: specs2 :: akkaTestKit :: akkaTypedTestKit :: akkaStreamTestKit :: kafkaTestKit :: Nil
  }

  object TestDB extends Module {
    private lazy val lvlDbVersion = "0.12"
    private lazy val lvlDbJniVersion = "1.8"

    private lazy val lvlDb = "org.iq80.leveldb" % "leveldb" % lvlDbVersion
    private lazy val lvlDbJni = "org.fusesource.leveldbjni" % "leveldbjni-all" % lvlDbJniVersion

    override def modules: Seq[ModuleID] =
      lvlDb :: lvlDbJni :: Nil
  }

  object Akka extends Module {
    val akkaHttpVersion = "10.1.11"
    val akkaManagementVersion = "1.0.3"

    private def akkaModule(name: String) = "com.typesafe.akka" %% name % akkaVersion
    private def akkaHttpModule(name: String) = "com.typesafe.akka" %% name % akkaHttpVersion
    private def akkaManagmentModule(name: String) = "com.lightbend.akka.management" %% name % akkaManagementVersion
    private lazy val akkaStreamKafka = "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.4"

    override def modules: Seq[ModuleID] =
      akkaModule("akka-actor") ::
      "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "2.0.2" ::
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion ::
      "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion ::
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion ::
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion ::
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion ::
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion ::
      "com.typesafe.akka" %% "akka-protobuf" % akkaVersion ::
      "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion ::
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion ::
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion ::
      "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion ::
      "com.lightbend.akka.management" %% "akka-management" % akkaManagementVersion ::
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion ::
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion ::
      akkaStreamKafka ::
      Nil
  }

  object ScalaZ extends Module {
    private lazy val scalazVersion = "7.2.28"

    private lazy val scalazCore = "org.scalaz" %% "scalaz-core" % scalazVersion
    private lazy val scalazConcurrent = "org.scalaz" %% "scalaz-concurrent" % scalazVersion

    override def modules: Seq[ModuleID] = scalazCore :: scalazConcurrent :: Nil
  }

  object Kamon extends Module {
    private lazy val kamonBundle = "io.kamon" %% "kamon-bundle" % "2.1.0"
    private lazy val kamonAPM = "io.kamon" %% "kamon-apm-reporter" % "2.1.0"
    // private lazy val kamonLogStash = "com.codekeepersinc" %% "kamonlogstash" % "0.0.1"

    val core = "io.kamon" %% "kamon-core" % "2.1.4"
    val status = "io.kamon" %% "kamon-status-page" % "2.1.4"
    val prometheus = "io.kamon" %% "kamon-prometheus" % "2.1.4"
    override def modules: Seq[sbt.ModuleID] = core :: status :: prometheus :: Nil
  }

  object Utils extends Module {
    private lazy val logbackVersion = "1.2.3"
    private lazy val kryoVersion = "0.9.3"
    private lazy val circeVersion = "0.14.0-M1"

    private lazy val logback = "ch.qos.logback" % "logback-classic" % logbackVersion
    private lazy val logbackEncoder = "net.logstash.logback" % "logstash-logback-encoder" % "5.3"
    private lazy val kryo = "io.altoo" %% "akka-kryo-serialization" % "1.1.0" //"com.twitter" %% "chill-akka" % kryoVersion
    private lazy val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"
    private lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
    private lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
    private lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
    private lazy val rocksDB = "org.rocksdb" % "rocksdbjni" % "6.13.3"
    private lazy val kafkaFS2 = "com.github.fd4s" %% "fs2-kafka" % "1.1.0"

    override def modules: Seq[ModuleID] =
      logback ::
      logbackEncoder ::
      kryo ::
      circeCore ::
      circeGeneric ::
      circeParser ::
      rocksDB ::
      kafkaFS2 ::
      Nil
  }

  // Projects
  lazy val mainDeps: Seq[sbt.ModuleID] = Akka.modules ++ ScalaZ.modules ++ Utils.modules ++ Kamon.modules
  lazy val testDeps: Seq[sbt.ModuleID] = Test.modules ++ TestDB.modules
}

trait Dependencies {
  val scalaVersionUsed: String = Dependencies.scalaVersion
  val commonResolvers: Seq[MavenRepository] = Dependencies.commonResolvers
  val mainDeps: Seq[sbt.ModuleID] = Dependencies.mainDeps
  val testDeps: Seq[sbt.ModuleID] = Dependencies.testDeps
}
