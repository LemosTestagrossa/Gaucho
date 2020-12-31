package com.gaucho.infrastructure.snapshotting.kafka

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
import com.gaucho.infrastructure.kafka.KafkaMessageProducer.KeyValue
import com.gaucho.infrastructure.kafka.MessageProducerLike
import com.gaucho.infrastructure.snapshotting.OptimalBatcher.OptimalBatcherRef

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{Map => MMap}
import scala.concurrent.duration.DurationInt
class KafkaBatchInserter(
    implicit
    system: ActorSystem,
    ec: ExecutionContext,
    monitoring: Monitoring,
    messageProducer: MessageProducerLike
) extends BatchInserter[(String, KeyValue)] {
  val messages_published_to_kafka: Counter = monitoring.counter("messages_published_to_kafka")

  def insertBatch(messages: Seq[(String, KeyValue)]): Future[Done] = {
    retryFuture(for {
      _ <- Future.sequence(
        messages.groupBy(_._1).map {
          case (str, value) =>
            messageProducer.produce(() => value.map(_._2), str)
        }
      )
    } yield {
      messages_published_to_kafka.add(messages.size)
      Done
    })
  }

}

object KafkaBatchInserter {

  case class KafkaInserterOptimalBatcher(ref: OptimalBatcherRef[(String, KeyValue)]) {
    def publishToKafka(message: (String, KeyValue)): Unit = {
      ref.ref ! message
    }
  }

}
