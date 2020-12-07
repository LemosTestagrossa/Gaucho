package com.gaucho.domain

import com.gaucho.infrastructure.persistent_actor.WithAggregateRoot

case class Event(
    group: String,
    topic: String,
    partition: Int,
    deliveryId: Long,
    aggregateRoot: String,
    json: String
) extends WithAggregateRoot {

  final def persistenceId = aggregateRoot
  final def sequenceNr = deliveryId
}

object Event {

  case class AuditableEvent(event: Event)
  object AuditableEvent {
    def apply(
        group: String,
        topic: String,
        partition: Int,
        deliveryId: Long,
        aggregateRoot: String,
        json: String
    ): AuditableEvent =
      AuditableEvent(Event(group, topic, partition, deliveryId, aggregateRoot, json))
  }

  case class Snapshot(event: Event)
  object Snapshot {

    def apply(
        group: String,
        topic: String,
        partition: Int,
        deliveryId: Long,
        aggregateRoot: String,
        json: String
    ): Snapshot =
      Snapshot(Event(group, topic, partition, deliveryId, aggregateRoot, json))
  }

  def apply(id: Int): AuditableEvent =
    AuditableEvent(
      "group",
      "topic",
      1,
      deliveryId = id,
      aggregateRoot = s"entityA",
      json = s""" { "name" : "$id" } """
    ) // TODO REMOVE

}
