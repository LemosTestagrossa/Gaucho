package com.gaucho.infrastructure.sharding

import akka.actor.{Actor, ActorLogging, Props}
import ActorRefOf.{AggregateRoot, PropsOf}
import com.gaucho.domain.Event
import com.gaucho.infrastructure.monitoring.algebra.Monitoring
import com.gaucho.infrastructure.persistent_actor.NonAuditablePersistentActor
import com.gaucho.infrastructure.snapshotting.EventOptimalBatcher
import com.gaucho.domain.Event.Snapshot

object ExampleActor {
  sealed trait ExampleActorProtocol {
    def aggregateRoot: String
  }
  case class SetState(aggregateRoot: String, a: String) extends ExampleActorProtocol
  case class GetState(aggregateRoot: String) extends ExampleActorProtocol

  implicit val propsOf: PropsOf[ExampleActorProtocol, (EventOptimalBatcher, EventOptimalBatcher, Monitoring)] =
    PropsOf(r => Props(new ExampleActor("empty")(r.requirements._1, r.requirements._2)(r.requirements._3)))
  implicit val root: AggregateRoot[ExampleActorProtocol] = AggregateRoot(s => s.aggregateRoot)
}

private class ExampleActor(initial: String)(rocksDbBatcher: EventOptimalBatcher, cassandra: EventOptimalBatcher)(
    implicit monitoring: Monitoring
) extends NonAuditablePersistentActor(rocksDbBatcher, cassandra)
    with ActorLogging {
  import ExampleActor._

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
    case SetState(_, a) =>
      log.info("actor {} received: SetState {}", self.path, a)
      state = a
    case GetState(_) =>
      log.info("actor {} received: GetState", self.path)
      sender() ! state
  }

  override protected def receiveRecover: Event.Snapshot => Unit = {
    case Snapshot(event) => state = event.json
  }
}
