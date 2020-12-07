package com.gaucho.infrastructure.kafka

import akka.Done

import scala.concurrent.Future

trait MessageProducerLike {
  def produce[A](topic: String, seq: Seq[A])(implicit serializer: A => String): Future[Done]
}
