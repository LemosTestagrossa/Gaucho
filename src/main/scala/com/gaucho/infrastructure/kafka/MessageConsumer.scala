package com.gaucho.infrastructure.kafka

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.ActorAttributes.Dispatcher
import akka.stream.scaladsl.{Sink, Source}
import com.gaucho.domain.MessageWithOffset
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.concurrent.{ExecutionContext, Future}
import com.gaucho.infrastructure.persistent_actor.Persistence._
import org.apache.kafka.clients.producer.ProducerRecord

class MessageConsumer(config: Config)(
    implicit
    system: ActorSystem,
    ec: ExecutionContext
) extends MessageConsumerLike {

  private val consumerSettings =
    ConsumerSettings(config.getConfig("akka.kafka.consumer"), new StringDeserializer, new StringDeserializer)
      .withBootstrapServers("0.0.0.0:9092")
      .withDispatcher("kafka-consumer-dispatcher")

  def consume(
      group: String,
      topic: String,
      partition: Int
  )(
      callback: MessageWithOffset => Unit
  ): Future[Done] = {

    Consumer
      .plainSource(
        settings = consumerSettings.withGroupId(group),
        subscription = Subscriptions
          .assignmentWithOffset(
            tp = new TopicPartition(topic, partition),
            offset = readOffset(topic, partition) + 1
          )
      )
      .map { msg =>
        MessageWithOffset(msg.value, msg.offset)
      }
      .map(callback)
      //.mapAsyncUnordered(Runtime.getRuntime.availableProcessors() - 1) { a =>
        //Future {
          //callback(a)
        //}
      //}
      .runWith(Sink.ignore)
  }

}
