package com.gaucho.infrastructure.rocksdb

import akka.Done
import akka.actor.ActorSystem
import com.gaucho.infrastructure.persistent_actor.WithAggregateRoot
import com.typesafe.config.ConfigFactory
import org.rocksdb.{Options, RocksDB, WriteBatch, WriteOptions}

import scala.concurrent.{ExecutionContext, Future}
import com.gaucho.infrastructure.resilience.Retry.retryFuture
object RocksDBUtils { // TODO make a class and a trait StateMap extends MapLike

  val rocksDB = RocksDB.open({
    new Options().setCreateIfMissing(true)
  }, ConfigFactory.load().getString("rocksdb-journal.dir"))

  def load(key: String)(
      implicit
      ec: ExecutionContext,
      system: ActorSystem
  ): Future[String] =
    retryFuture(Future {
      val loaded = rocksDB.get(key.getBytes)
      println(s"loaded was ${loaded}")
      val loadedd = loaded.map(_.toChar).mkString
      println(s"loadedd was ${loadedd}")
      loadedd
    })

  def put[A](key: String, value: A)(
      implicit
      serializer: A => String,
      ec: ExecutionContext,
      system: ActorSystem
  ): Future[Done] =
    retryFuture(for {
      _ <- Future { rocksDB.put(key.getBytes, serializer(value).getBytes) }
    } yield Done)

  def put[A <: WithAggregateRoot](values: Seq[A])(
      implicit
      serializer: A => String,
      ec: ExecutionContext,
      system: ActorSystem
  ): Future[Done] = {
    retryFuture(for {
      _ <- Future {
        val batch: WriteBatch = new WriteBatch()
        values.foreach { m: A =>
          batch.put(m.aggregateRoot.getBytes, serializer(m).getBytes())
        }
        rocksDB.write(new WriteOptions(), batch)
        batch.close()
      }
    } yield Done)
  }

}
