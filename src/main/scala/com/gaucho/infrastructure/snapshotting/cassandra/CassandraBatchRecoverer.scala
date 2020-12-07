package com.gaucho.infrastructure.snapshotting.cassandra

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.gaucho.infrastructure.monitoring.algebra.{Counter, Monitoring}

import scala.concurrent.{ExecutionContext, Future}
import com.gaucho.infrastructure.cassandra.CqlSessionSingleton.session
import com.gaucho.domain.Event.Snapshot
import com.gaucho.infrastructure.snapshotting.rocksdb.RocksdbBatchInserter
import com.gaucho.infrastructure.snapshotting.{BatchInserter, BatchRecoverer}
import com.typesafe.config.ConfigFactory

class CassandraBatchRecoverer(
    implicit
    system: ActorSystem,
    ec: ExecutionContext,
    monitoring: Monitoring
) extends BatchRecoverer[Snapshot] {
  val eventsSavedToCassandra: Counter = monitoring.counter("events_saved_to_cassandra")
  val eventsReadToCassandra: Counter = monitoring.counter("events_read_from_cassandra")
  val partition = ConfigFactory.load().getInt("partition")

  def readSnapshots(batchProcessing: Seq[Snapshot] => Future[Done]): Future[Done] = {
    session
      .select(s"SELECT * FROM eventstore.snapshots WHERE partition_id = ? ALLOW FILTERING", Long.box(partition))
      .map(e =>
        Snapshot(
          "group",
          "topic",
          1,
          deliveryId = e.getLong("seq_nr"),
          json = e.getString("event"),
          aggregateRoot = e.getString("id")
        )
      )
      .grouped(10000)
      .mapAsync(2)(batchProcessing)
      .runWith(Sink.ignore)
  }
}

object CassandraBatchRecoverer {
  def toRocksdb(
      implicit
      system: ActorSystem,
      ec: ExecutionContext,
      monitoring: Monitoring,
      o: RocksdbBatchInserter
  ): Unit =
    new CassandraBatchRecoverer().readSnapshots({ batch =>
      o.insertBatch(batch.map(_.event))
    })
}
