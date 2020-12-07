package com.gaucho.infrastructure.sharding

import akka.actor.ActorSystem
import ActorRefOf.RequirementsOf
import ExampleActor.{ExampleActorProtocol, _}
import GuardianSystem.GuardianSystemRef
import com.gaucho.infrastructure.monitoring.MonitoringMock
import com.gaucho.infrastructure.monitoring.algebra.Monitoring
import com.gaucho.infrastructure.sharding.GuardianSystem.GuardianSystemRef
import com.gaucho.infrastructure.snapshotting.EventOptimalBatcher
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.ExecutionContext

class ActorRunner extends AnyFlatSpec {
  implicit val a: ActorSystem = ActorSystem("ActorRunner")
  implicit val ec: ExecutionContext = a.dispatcher
  implicit val kamonInstance = new MonitoringMock
  val config: Config = ConfigFactory.load()
  val r = EventOptimalBatcher.rocksdbBatcher
  def c = EventOptimalBatcher.cassandradbBatcher

  val guardian: GuardianSystemRef = GuardianSystem.create(RequirementsOf(config))
  val exampleActor =
    guardian.actorOf[ExampleActorProtocol, (EventOptimalBatcher, EventOptimalBatcher, Monitoring)](
      RequirementsOf((r, c, kamonInstance))
    )

  val result = for {
    a0 <- exampleActor ? GetState("one")
    _ <- exampleActor ! SetState("one", "5")
    a1 <- exampleActor ? GetState("one")
    b0 <- exampleActor ? GetState("two")
    _ <- exampleActor ! SetState("two", "6")
    b1 <- exampleActor ? GetState("two")
  } yield s"Previous State: $a0-$b0 <<>> New State: $a1-$b1"

  result.map { r: String =>
    assert(r == s"Previous State: empty-empty <<>> New State: 5-6")
  }

  Thread.sleep(1000)

}
