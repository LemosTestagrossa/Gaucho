package com.gaucho.infrastructure.monitoring.algebra

import scala.concurrent.{ExecutionContext, Future}

trait Histogram {
  def record(value: Long): Unit

  def record[T](codeToBenchmark: => T): T = {

    val before = System.nanoTime()
    val result = codeToBenchmark
    val after = System.nanoTime()

    record(after - before)

    result
  }

  def recordFuture[T](codeToBenchmark: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val before = System.currentTimeMillis()
    val result = codeToBenchmark

    result.foreach(_ => record(System.currentTimeMillis() - before))

    result
  }
}
