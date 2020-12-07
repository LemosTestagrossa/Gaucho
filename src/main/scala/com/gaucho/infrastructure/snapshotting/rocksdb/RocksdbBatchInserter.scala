package com.gaucho.infrastructure.snapshotting.rocksdb

import akka.Done
import akka.actor.{ActorSystem, Scheduler}
import com.gaucho.domain.{Event, GroupTopicPartitionOffset}
import com.gaucho.infrastructure.monitoring.algebra.{Counter, Monitoring}
import com.gaucho.infrastructure.resilience.Retry.retryFuture
import com.gaucho.infrastructure.rocksdb.RocksDBUtils._
import com.gaucho.domain._
import com.gaucho.infrastructure.persistent_actor.Persistence.saveOffset

import scala.concurrent.{ExecutionContext, Future}
import com.gaucho.infrastructure.snapshotting.BatchInserter
import com.gaucho.domain.GroupTopicPartitionOffset

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{Map => MMap}
import scala.concurrent.duration.DurationInt
class RocksdbBatchInserter(
    implicit
    system: ActorSystem,
    ec: ExecutionContext,
    monitoring: Monitoring
) extends BatchInserter[Event] {
  val eventsSavedToRocksDb: Counter = monitoring.counter("events_saved_to_rocksdb")

  implicit val scheduler: Scheduler = system.scheduler

  var offsets = MMap.empty[GroupTopicPartition, Set[Long]]
  def addOffsets(groupTopicPartition: GroupTopicPartition, offsetList: Seq[Long]): Unit =
    offsets.put(
      groupTopicPartition,
      offsets.getOrElse(groupTopicPartition, Set.empty) ++ offsetList
    )
  def commitableOffsets: MMap[GroupTopicPartition, ListBuffer[Long]] =
    offsets.map {
      case (key, value: Set[Long]) =>
        val offsetsToCommit = ListBuffer.empty[Long]
        var lastOffset = 0L
        for (e <- value) {
          if (e - lastOffset == 1) {
            lastOffset = e
            offsetsToCommit.addOne(e)
          }
        }
        (key, offsetsToCommit)
    }
  def commitOffsets(): Unit =
    commitableOffsets.foreach {
      case (g, o) if o.nonEmpty =>
        saveOffset(
          g.group,
          g.topic,
          g.partition
        )(o.last)
        offsets(g) --= o
        monitoring
          .gauge(
            s"kafka_offset" +
            s"_group_${g.group}" +
            s"_topic_${g.topic}" +
            s"_partition_${g.partition}"
          )
          .set(o.last)
    }

  system.scheduler.scheduleAtFixedRate(15 seconds, 15 seconds)({ () =>
    commitOffsets()
  })

  def insertBatch(events: Seq[Event]): Future[Done] = {
    retryFuture(for {
      _ <- put(events)
    } yield {
      eventsSavedToRocksDb.add(events.size)
      events
        .map(e => GroupTopicPartitionOffset(e.group, e.topic, e.partition, e.sequenceNr))
        .groupBy(e => e.groupTopicPartition)
        .foreach {
          case (groupTopicPartition, rest) =>
            addOffsets(groupTopicPartition, rest.map(_.offset))
        }
      Done
    })
  }

}
