package com.gaucho.infrastructure.persistent_actor

import akka.actor.{Actor, ActorRef}

import scala.concurrent.ExecutionContext
import Persistence._
import com.gaucho.infrastructure.monitoring.algebra.{Counter, Monitoring}
import com.gaucho.domain.Event._
import scala.concurrent.duration._
import akka.actor.PoisonPill 

protected[persistent_actor] abstract class PersistentActor(implicit monitoring: Monitoring) extends Actor {
  protected def receiveCommand: Receive
  final override def receive: Receive = receiveCommand
  protected def receiveRecover: Snapshot => Unit

  implicit protected final val ec: ExecutionContext = context.system.dispatcher
  val eventsReadToRocksDb: Counter = monitoring.counter("events_read_from_rocksdb")
  
  context.system.scheduler.scheduleOnce(2 seconds, self, PoisonPill)

  protected def persist(
      auditory: AuditableEvent,
      snapshot: Snapshot
  )(
      sender: ActorRef
  )(
      callback: ActorRef => Unit
  ): Unit

  final protected val persistenceId: String = self.path.name
  private implicit val system = context.system
  override def preStart(): Unit = {
    super.preStart()
    for {
      snapshot: Snapshot <- recoverSnapshot(persistenceId)
      _ = eventsReadToRocksDb.increment()
    } yield {
      receiveRecover(snapshot)
    }
  }

}
