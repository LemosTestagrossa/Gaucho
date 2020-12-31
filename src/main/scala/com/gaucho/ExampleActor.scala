package com.gaucho

import akka.actor.{ActorLogging, Props}
import com.gaucho.domain.Event
import com.gaucho.infrastructure.monitoring.algebra.Monitoring
import com.gaucho.infrastructure.persistent_actor.NonAuditablePersistentActor
import com.gaucho.infrastructure.sharding.ActorRefOf.{AggregateRoot, PropsOf}
import com.gaucho.infrastructure.snapshotting.EventOptimalBatcher
import com.gaucho.domain.Event.{AuditableEvent, Snapshot}

object ExampleActor {

  sealed trait ExampleActorProtocol {
    def aggregateRoot: String
  }
  case class SetState(aggregateRoot: String, a: String) extends ExampleActorProtocol
  case class GetState(aggregateRoot: String) extends ExampleActorProtocol
  case class Hello(
      group: String,
      topic: String,
      partition: Int,
      deliveryId: Long,
      aggregateRoot: String,
      json: String
  ) extends ExampleActorProtocol
  object Hello {
    def apply(group: String, topic: String, partition: Int, i: Int): Hello = {
      Hello(
        "defaultGroup",
        topic,
        partition,
        i,
        i.toString,
        i.toString
      )
    }

    import io.circe._
    import io.circe.generic.auto._
    import io.circe.parser._
    import io.circe.syntax._
    implicit def helloToJson(instance: Hello): String =
      instance.asJson.noSpaces
    implicit def helloFromJson(json: String): Either[Error, Hello] =
      decode[Hello](json)
  }

  implicit val propsOf: PropsOf[ExampleActorProtocol, (EventOptimalBatcher, EventOptimalBatcher, Monitoring)] =
    PropsOf(r => Props(new ExampleActor("empty")(r.requirements._1, r.requirements._2)(r.requirements._3)))
  implicit val root: AggregateRoot[ExampleActorProtocol] = AggregateRoot(s => s.aggregateRoot)
}

private class ExampleActor(initial: String)(rocksDbBatcher: EventOptimalBatcher, cassandra: EventOptimalBatcher)(
    implicit monitoring: Monitoring
) extends NonAuditablePersistentActor()(rocksDbBatcher, monitoring)
    with ActorLogging {
  import ExampleActor._

  val name = self.path.name
  val actorMessagesCounter = monitoring.counter(s"${name}_messages")

  override def preStart(): Unit = {
    super.preStart()
    log.info("executing preStart of actor: {}", self.path)
  }

  override def postStop(): Unit = {
    log.info("executing postStop of actor: {}", self.path)
    super.postStop()
  }

  var state: String = initial
  override def receiveCommand: Receive = {
    case Hello(group, topic, partition, deliveryId, aggregateRoot, json) =>
      actorMessagesCounter.increment()
      val event = Event(
        group,
        topic,
        partition,
        deliveryId,
        aggregateRoot,
        json
      )
      persist(AuditableEvent(event), Snapshot(event))(sender()) { sender =>
        //sender ! akka.Done
      }
    case SetState(_, a) =>
      actorMessagesCounter.increment()
      log.debug("actor {} received: SetState {}", self.path, a)
      state = a
    case GetState(_) =>
      actorMessagesCounter.increment()
      log.debug("actor {} received: GetState", self.path)
      sender() ! state
  }

  override protected def receiveRecover: Event.Snapshot => Unit = {
    case Snapshot(event) =>
      println(s"Recovered ${event.json}")
      state = event.json
  }
}
