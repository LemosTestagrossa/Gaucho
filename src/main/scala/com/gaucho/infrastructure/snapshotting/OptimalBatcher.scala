package com.gaucho.infrastructure.snapshotting

import java.time.LocalDateTime

import akka.Done
import akka.actor.{Actor, ActorRef, ActorSystem, Props, Timers}
import com.gaucho.infrastructure.monitoring.algebra.Monitoring

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationDouble, DurationInt, DurationLong}
import org.apache.kafka.common.protocol.types.Field.UUID

import scala.reflect.ClassTag
import scala.util.Random
object OptimalBatcher {
  case class OptimalBatcherRef[BatchElement](ref: ActorRef)

  def start[BatchElement](
      name: String,
      insertBatchElements: Seq[BatchElement] => Future[Done],
      recommendedBatchSize: Int = math.min(1, Runtime.getRuntime.availableProcessors - 1),
      recommendedBatchTime: Option[Int] = None
  )(
      implicit
      system: ActorSystem,
      m: Monitoring,
      c: ClassTag[BatchElement]
  ): OptimalBatcherRef[BatchElement] = {
    val actorName = s"OptimalBatcher_$name"
    OptimalBatcherRef(
      system.actorOf(Props(new OptimalBatcher[BatchElement](insertBatchElements, recommendedBatchSize, recommendedBatchTime)), actorName)
    )
  }
}

private class OptimalBatcher[BatchElement: ClassTag](insertBatchElements: Seq[BatchElement] => Future[Done],
                                                     recommendedBatchSize: Int, recommendedBatchTime: Option[Int])(
    implicit m: Monitoring
) extends Actor {

  import collection.mutable.ListBuffer
  var batchElements: ListBuffer[BatchElement] = ListBuffer.empty
  implicit val system = context.system
  implicit val ec = system.dispatcher
  import akka.pattern.pipe

  def now: Long = System.currentTimeMillis()

  val name = self.path.name
  val bufferTimeGauge = m.gauge(name + "buffer_time")
  val bufferSizeGauge = m.gauge(name + "buffer_size")
  var eventualConsistency = 0
  var bufferElements = 0
  var bufferBucketSize = recommendedBatchSize
  var bufferTimeSize = 4000.0 // millis
  var timeThatItTookToWrite = 1000L // millis
  val oneSecond = 1000L
  var timeThatItTookToFillTheBuffer = oneSecond // millis
  var timestampFromLastBufferFlush = now
  var timestampFromBegginingOfWrite = now

  def cap(min: Double, max: Double)(e: Double): Double = if (e < min) min else if (e > max) max else e
  def capByHalf: Double => Double = cap(0.5, 1.5)

  def recalculateEventualConsistency(): Unit = {
    val relation = {
      val r = timeThatItTookToWrite.toDouble / timeThatItTookToFillTheBuffer.toDouble
      if (r.isNaN) 1 else r
    }
    bufferTimeSize = cap(1000, 6 * oneSecond)(bufferTimeSize * capByHalf(relation))
    bufferSizeGauge.set(bufferBucketSize)
    bufferTimeGauge.set(bufferTimeSize)
  }

  case object RecalculateEventualConsistency
  case object FlushBatchElements

  var isKafka = false
  if (recommendedBatchTime.isDefined){
    val time = recommendedBatchTime.get
    isKafka = true
    println("IS KAFKA>")
    context.system.scheduler.scheduleAtFixedRate(time seconds, time seconds , self, FlushBatchElements)
  }
  else {
    context.system.scheduler.scheduleAtFixedRate(2 seconds, 2 seconds, self, RecalculateEventualConsistency)
  }

  def flushBatchElements(): Unit = {
    if (batchElements.nonEmpty) {
      bufferElements = 0
      val e = batchElements.toSeq
      batchElements = ListBuffer.empty
      timestampFromBegginingOfWrite = now
      timeThatItTookToFillTheBuffer = now - timestampFromLastBufferFlush
      timestampFromLastBufferFlush = now

      if (isKafka) println(s"KAFKA FLUSHING EVENTS EVERY $timeThatItTookToFillTheBuffer")
      insertBatchElements(e).map { _ =>
        timeThatItTookToWrite = now - timestampFromBegginingOfWrite
      }
    }
    ()
  }

  override def receive: Receive = {
    case event: BatchElement =>
      batchElements.append(event)
      bufferElements += 1
      if (bufferElements > bufferBucketSize) {
        self ! FlushBatchElements
      }
    case FlushBatchElements =>
      flushBatchElements()
    case RecalculateEventualConsistency =>
      recalculateEventualConsistency()
      context.system.scheduler.scheduleOnce(bufferTimeSize milliseconds, self, FlushBatchElements)
  }
}
