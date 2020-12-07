package com.gaucho.infrastructure.persistent_actor

import akka.actor.ActorRef
import com.gaucho.infrastructure.monitoring.algebra.Monitoring
import com.gaucho.infrastructure.snapshotting.EventOptimalBatcher
import com.gaucho.domain.Event._

abstract class AuditablePersistentActor(rocksdb: EventOptimalBatcher, cassandra: EventOptimalBatcher)(
    implicit
    monitoring: Monitoring
) extends PersistentActor {
  final protected def persist(
      auditory: AuditableEvent,
      snapshot: Snapshot
  )(
      sender: ActorRef
  )(
      callback: ActorRef => Unit
  ): Unit = {
    cassandra.storeEvent(auditory.event)
    rocksdb.storeEvent(snapshot.event)
    callback(sender)
  }
}
