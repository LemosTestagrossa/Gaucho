package com.gaucho.infrastructure.snapshotting.cassandra

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.cassandra.CassandraWriteSettings
import akka.stream.alpakka.cassandra.scaladsl.CassandraFlow
import akka.stream.scaladsl.{Sink, Source}
import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}
import com.gaucho.infrastructure.resilience.Retry
import com.gaucho.infrastructure.monitoring.algebra.{Counter, Monitoring}

import scala.concurrent.{ExecutionContext, Future}
import com.gaucho.infrastructure.cassandra.CqlSessionSingleton.session
import com.gaucho.infrastructure.rocksdb
import com.gaucho.infrastructure.snapshotting.BatchInserter
import com.typesafe.config.ConfigFactory

import scala.collection.IterableOnce.iterableOnceExtensionMethods

abstract class CassandraBatchInserter[T](
    implicit
    system: ActorSystem,
    ec: ExecutionContext,
    monitoring: Monitoring
) extends BatchInserter[T] {

  val eventsSavedToCassandra: Counter
  val eventsReadToCassandra: Counter

  def cqlStatement: String
  def statementBinder: (T, PreparedStatement) => BoundStatement

  final def insertBatch(events: Seq[T]): Future[Done] =
    createBatch[T](events)(
      cqlStatement,
      statementBinder
    )

  val partition = ConfigFactory.load().getInt("partition")

  private final def createBatch[T](events: Seq[T])(
      cqlStatement: String,
      statementBinder: (T, PreparedStatement) => BoundStatement
  ): Future[Done] = {
    Retry.retryFuture(for {
      done <- Source(events)
        .via {
          CassandraFlow.createBatch[T, Long](
            writeSettings = CassandraWriteSettings.create().withMaxBatchSize(10000),
            cqlStatement,
            statementBinder,
            (_: T) => partition
          )(session)
        }
        .runWith(Sink.ignore)
    } yield {
      eventsSavedToCassandra.add(events.size)
      done
    })
  }
}
