package com.gaucho.infrastructure.persistent_actor

import akka.Done
import akka.actor.ActorSystem
import com.gaucho.domain.Event.Snapshot
import com.gaucho.domain.GroupTopicPartition
import com.gaucho.infrastructure.rocksdb.RocksDBUtils.{load, put}
import com.gaucho.domain.Event._
import com.gaucho.domain._
import org.rocksdb.RocksDBException
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object Persistence {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def readOffset(topic: String, partition: Int)(
      implicit
      ec: ExecutionContext,
      system: ActorSystem
  ): Long =
    readOffset("defaultGroup", topic, partition)
  def readOffset(group: String, topic: String, partition: Int)(
      implicit
      ec: ExecutionContext,
      system: ActorSystem
  ): Long =
    Try {
      val a = Await.result(load(GroupTopicPartition(group, topic, partition)), 5 second).toLong
      a
    } match {
      case Failure(e: RocksDBException) =>
        -1L
      case Failure(e) =>
        -1L
      case Success(offset) =>
        offset
    }

  def saveOffset(topic: String, partition: Int)(offset: Long)(
      implicit
      ec: ExecutionContext,
      system: ActorSystem
  ): Option[Done] =
    saveOffset("defaultGroup", topic, partition)(offset)
  def saveOffset(group: String, topic: String, partition: Int)(
      offset: Long
  )(
      implicit ec: ExecutionContext,
      system: ActorSystem
  ): Option[Done] =
    Try {
      put(GroupTopicPartition(group, topic, partition), offset)
    } match {
      case Failure(e: RocksDBException) => None
      case Failure(e) => None
      case Success(_) => Some(Done)
    }

  def recoverSnapshot(message: WithAggregateRoot)(
      implicit
      ec: ExecutionContext,
      system: ActorSystem
  ): Future[Option[Snapshot]] =
    recoverSnapshot(message.aggregateRoot)
  def recoverSnapshot(aggregateRoot: String)(
      implicit ec: ExecutionContext,
      system: ActorSystem
  ): Future[Option[Snapshot]] = {
    load(aggregateRoot).map { loaded =>
      val snapshot = for {
        event <- EventFromJson(loaded)
      } yield Snapshot(event)

      snapshot match {
        case Left(value) =>
          logger.error(s"Error while parsing snapshot: ${value}")
          None
        case Right(value) =>
          Some(value)
      }
    }
  }
}
