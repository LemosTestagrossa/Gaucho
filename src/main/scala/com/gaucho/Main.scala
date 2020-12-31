package com.gaucho

import akka.Done
import com.gaucho.ExampleActor.{ExampleActorProtocol, Hello}
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.util.Timeout
import com.gaucho.Main.timeout
import com.gaucho.domain.MessageWithOffset
import com.gaucho.infrastructure.kafka.KafkaMessageProducer.{bootstrapServers, KeyValue}
import com.gaucho.infrastructure.kafka.{KafkaMessageProducer, MessageConsumer}
import com.gaucho.infrastructure.monitoring.algebra.Monitoring
import com.gaucho.infrastructure.monitoring.interpreter.{AkkaBasedMonitoring, MetricsApi, MonitoringApiActor}
import com.gaucho.infrastructure.sharding.ActorRefOf.RequirementsOf
import com.gaucho.infrastructure.sharding.GuardianSystem
import com.gaucho.infrastructure.sharding.GuardianSystem.GuardianSystemRef
import com.gaucho.infrastructure.snapshotting.EventOptimalBatcher
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object Main extends App {

  val logger = LoggerFactory.getLogger(getClass)
  val config =
    ConfigFactory.parseString("""
      |
      |      akka.cluster.seed-nodes = ["akka://application@0.0.0.0:2551"]
      |      akka.remote.artery.canonical.port = 2551
      |
      |""".stripMargin).withFallback(ConfigFactory.load())

  implicit val system = ActorSystem("application", config)
  implicit val timeout: Timeout = Timeout(15 * 60 seconds)
  implicit val ec = system.dispatcher

  implicit val monitoringApiActor = MonitoringApiActor.start
  implicit val monitoring = new AkkaBasedMonitoring()

  MetricsApi.start.onComplete {
    case Success(bound) =>
      logger.info(
        s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/"
      )
    case Failure(e) =>
      logger.error("Server could not start!")
      e.printStackTrace()
  }

  val messagesPublishedToKafka = monitoring.counter("messages_published_to_kafka")
  val messagesProcessedByActorCounter = monitoring.counter("messages_processed_by_actor")

  //Await.result(checkpointer.writeCassandraEventsToRocksDB(), Duration.Inf)
  logger.info("DONE writeCassandraEventsToRocksDB")

  val topic = "pepe"

  val messageProducer = new KafkaMessageProducer()(
    system,
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)
  )
  val messageConsumer = new MessageConsumer(config)

  val million = 1000 * 1000

  def producer(topic: String, i: Int): Future[Done] = {
    messageProducer
      .produce(
        () =>  (1 to million)
          .map(index => Hello.apply(group = "defaultGroup", topic, partition = 0, index))
          .map(
            _.copy(aggregateRoot = i match {
              case 1 => "actorA"
              case 2 => "actorB"
              case 3 => "actorC"
              case 4 => "actorD"
              case 5 => "actorE"
            })
          )
          .map(hello => KeyValue(hello.aggregateRoot, Hello.helloToJson(hello))),
        topic
      )
      .map { done =>
        messagesPublishedToKafka.add(million)
        done
      }
  }

  /*Await.result(
    Future.sequence(
      Seq(
        producer("topic1", 1),
        producer("topic2", 2),
        producer("topic3", 3),
        producer("topic4", 4),
        producer("topic5", 5)
      )
    ),
    Duration.Inf
  )*/

  val guardian: GuardianSystemRef = GuardianSystem.create(RequirementsOf(config))
  val exampleActor =
    guardian.actorOf[ExampleActorProtocol, (EventOptimalBatcher, EventOptimalBatcher, Monitoring)](
      RequirementsOf((EventOptimalBatcher.rocksdbBatcher, EventOptimalBatcher.cassandradbBatcher, monitoring))
    )

  def consumer(topic: String, i: Int) =
    messageConsumer.consume("group", topic, 0) { msg: MessageWithOffset =>
      (Hello helloFromJson msg.value)
        .map(_.copy(deliveryId = msg.offset))
        .map { hello =>
          messagesProcessedByActorCounter.increment()
          exampleActor ! hello
        }
    }

  consumer("topic1", 1)
  consumer("topic2", 2)
  consumer("topic3", 3)
  consumer("topic4", 4)
  consumer("topic5", 5)

  Await.result(system.whenTerminated, Duration.Inf)

}
