package com.gaucho.infrastructure.kafka

import akka.Done
import com.gaucho.infrastructure.kafka.KafkaMessageProducer.KeyValue

import scala.concurrent.{ExecutionContext, Future}

trait MessageProducerLike {
  def createTopic(topic: String): Future[Done]
  def produce(data: () => Seq[KeyValue], topic: String)(implicit ex: ExecutionContext): Future[Done]
}
