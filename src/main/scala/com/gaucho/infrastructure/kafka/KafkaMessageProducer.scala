package com.gaucho.infrastructure.kafka

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}
import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import cats.effect.IOApp
import com.gaucho.infrastructure.kafka.KafkaMessageProducer.KeyValue
import com.gaucho.infrastructure.monitoring.algebra.Monitoring
import com.typesafe.config.ConfigFactory
import fs2.kafka.KafkaProducer
import org.apache.kafka.clients.Metadata
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.FutureConverters._
import java.util.Properties

class KafkaMessageProducer(
    implicit
    system: ActorSystem,
    producerSettings: ProducerSettings[String, String]
) extends MessageProducerLike {

  import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
  import org.apache.kafka.common.serialization.StringSerializer

  lazy val bootstrapServers: String = Try { ConfigFactory.load().getString("kafka.brokers") }.getOrElse("0.0.0.0:9092")


  val kafkaProducerProps: Properties = {
    val props = new Properties()
    props.put("bootstrap.servers", bootstrapServers)
    props.put("key.serializer", classOf[StringSerializer].getName)
    props.put("value.serializer", classOf[StringSerializer].getName)
    props
  }

  val producer = new KafkaProducer[String, String](kafkaProducerProps)

  //val log = LoggerFactory.getLogger(this.getClass)

  def createTopic(topic: String): Future[Done] = Future.successful(Done)

  def produce(data: () => Seq[KeyValue], topic: String)(implicit ex: ExecutionContext): Future[Done] =
    Future.sequence(
      data().map { data =>
          Future{
            producer.send(
              new ProducerRecord[String, String]("myTopic", data.aggregateRoot, data.json)
            ).get
          }
      }
    ).map(_ => Done)
}

object KafkaMessageProducer {

  lazy val bootstrapServers: String = Try { ConfigFactory.load().getString("kafka.brokers") }.getOrElse("0.0.0.0:9092")

  case class KeyValue(aggregateRoot: String, json: String) {
    val key = aggregateRoot
    val value = json
  }

  def apply(monitoring: Monitoring,
            rebalancerListener: ActorRef)(implicit system: ActorSystem): KafkaMessageProducer = {
    implicit def producerSettings: ProducerSettings[String, String] =
      ProducerSettings(system, new StringSerializer, new StringSerializer)
        .withBootstrapServers(bootstrapServers)
    new KafkaMessageProducer()
  }
}
