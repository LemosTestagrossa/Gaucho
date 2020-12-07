package com.gaucho.infrastructure.resilience

import akka.actor.ActorSystem
import akka.pattern.after
import org.slf4j.LoggerFactory

object Retry {
  val logger = LoggerFactory.getLogger(this.getClass)
  import akka.actor.Scheduler

  import scala.concurrent.duration.{FiniteDuration, _}
  import scala.concurrent.{ExecutionContext, Future}

  val retryStrategy: Seq[FiniteDuration] = Seq.fill(1)(200 millis)
  val defaultDelays = Seq(
    5 seconds,
    10 seconds,
    15 seconds,
    20 seconds,
    25 seconds,
    30 seconds,
    60 seconds,
    2 minutes,
    5 minutes
  )

  def retryFuture[T](
      f: => Future[T],
      delays: Seq[FiniteDuration] = defaultDelays
  )(implicit ec: ExecutionContext, s: Scheduler): Future[T] =
    f recoverWith {
      case e: Throwable if delays.nonEmpty =>
        logger.warn("retrying - failed due to: {}", e.getMessage)
        after(delays.head, s)(retryFuture(f, delays.tail))
    }

  def retryFuture[T](
      f: => Future[T]
  )(implicit system: ActorSystem): Future[T] =
    retryFuture(f, defaultDelays)(system.dispatcher, system.scheduler)
}
