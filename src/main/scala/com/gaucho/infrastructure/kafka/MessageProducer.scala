package com.gaucho.infrastructure.kafka

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Future

class MessageProducer(config: Config)(implicit system: ActorSystem) extends MessageProducerLike {
  val producerSettings =
    ProducerSettings(config.getConfig("akka.kafka.producer"), new StringSerializer, new StringSerializer)
      .withBootstrapServers("0.0.0.0:9092")

  def produce[A](topic: String, seq: Seq[A])(
      implicit serializer: A => String
  ): Future[Done] =
    Source
      .fromIterator(() => seq.iterator)
      .map(serializer)
      .map((string: String) => new ProducerRecord[String, String](topic, string))
      .runWith(Producer.plainSink(producerSettings))
}
