package com.gaucho.infrastructure.snapshotting

import akka.actor.ActorSystem
import com.gaucho.domain.{Event, GroupTopicPartition}
import com.gaucho.infrastructure.snapshotting.OptimalBatcher.OptimalBatcherRef
import com.gaucho.infrastructure.monitoring.algebra.Monitoring
import com.gaucho.infrastructure.rocksdb.RocksDBUtils._
import com.gaucho.infrastructure.snapshotting.cassandra.CassandraEventBatchInserter
import com.gaucho.domain.GroupTopicPartition
import com.gaucho.infrastructure.snapshotting.rocksdb.RocksdbBatchInserter
import com.gaucho.domain._
import com.gaucho.infrastructure.kafka.KafkaMessageProducer.KeyValue
import com.gaucho.infrastructure.kafka.MessageProducerLike
import com.gaucho.infrastructure.snapshotting.kafka.KafkaBatchInserter
import com.gaucho.infrastructure.snapshotting.kafka.KafkaBatchInserter.KafkaInserterOptimalBatcher
import org.apache.kafka.common.protocol.types.Field.UUID

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext

case class EventOptimalBatcher(ref: OptimalBatcherRef[Event]) {
  def storeEvent(event: Event): Unit = {
    ref.ref ! event
  }
}

object EventOptimalBatcher {

  private def start(
      name: String,
      batchInserter: BatchInserter[Event]
  )(
      implicit
      system: ActorSystem,
      ec: ExecutionContext,
      monitoring: Monitoring
  ) =
    EventOptimalBatcher(
      OptimalBatcher.start(
        name, { batch: Seq[Event] =>
          val last = batch.last
          val persistenceId = GroupTopicPartition(last.group, last.topic, last.partition)
          for {
            doneSavingBatch <- batchInserter insertBatch batch
            doneSavingOffset <- put(persistenceId, batch.last.sequenceNr)
          } yield doneSavingOffset
        }
      )
    )

  def rocksdbBatcher(
      implicit
      system: ActorSystem,
      ec: ExecutionContext,
      monitoring: Monitoring
  ) = start("RocksdbBatchInserter", new RocksdbBatchInserter)

  def cassandradbBatcher(
      implicit
      system: ActorSystem,
      ec: ExecutionContext,
      monitoring: Monitoring
  ) = start("CassandraEventBatchInserter", new CassandraEventBatchInserter)

  val atomic = new AtomicInteger()
  def kafkaProducerBatcher(
                            recommendedBatchSize: Int = math.min(1, Runtime.getRuntime.availableProcessors - 1),
                            recommendedBatchTime: Option[Int] = None
                          )(
      implicit
      system: ActorSystem,
      ec: ExecutionContext,
      monitoring: Monitoring,
      messageProducer: MessageProducerLike ): KafkaInserterOptimalBatcher = {
    val (name, batchInserter) = ("KafkaBatchInserter_" + atomic.incrementAndGet(), new KafkaBatchInserter)
    KafkaInserterOptimalBatcher(
      OptimalBatcher.start(
        name + atomic.incrementAndGet(), { batch: Seq[(String, KeyValue)] =>
          for {
            doneSavingBatch <- batchInserter insertBatch batch
          } yield {
            println(s"Published ${batch.size} messages to Kafka")
            doneSavingBatch
          }
        },
        recommendedBatchSize,
        recommendedBatchTime
      )
    )
  }

}
