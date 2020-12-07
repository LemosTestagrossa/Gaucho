package com.gaucho.infrastructure.snapshotting.cassandra

import akka.actor.ActorSystem
import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}
import com.gaucho.domain.Event
import com.gaucho.infrastructure.monitoring.algebra.{Counter, Monitoring}

import scala.concurrent.ExecutionContext

class CassandraEventBatchInserter(
    implicit
    system: ActorSystem,
    ec: ExecutionContext,
    monitoring: Monitoring
) extends CassandraBatchInserter[Event] {
  val eventsSavedToCassandra: Counter = monitoring.counter("events_saved_to_cassandra")
  val eventsReadToCassandra: Counter = monitoring.counter("events_read_from_cassandra")

  def cqlStatement = s"INSERT INTO eventstore.events(partition_id, seq_nr, id, event) VALUES (?, ?, ?, ?)"
  def statementBinder: (Event, PreparedStatement) => BoundStatement =
    (event: Event, ps: PreparedStatement) =>
      ps.bind(Long.box(partition), Long.box(event.deliveryId), event.aggregateRoot, event.json)

  /*def readSnapshots(batchProcessing: Seq[Snapshot] => Future[Done]) = {
    session
      .select(s"SELECT * FROM eventstore.snapshots WHERE partition_id = ? ALLOW FILTERING", Long.box(partition))
      .map(e =>
        Snapshot(
          "group",
          "topic",
          1L,
          deliveryId = e.getLong("seq_nr"),
          json = e.getString("event"),
          aggregateRoot = e.getString("id")
        )
      )
      .grouped(10000)
      .mapAsync(2)(batchProcessing)
      .runWith(Sink.ignore)
  }*/
}
