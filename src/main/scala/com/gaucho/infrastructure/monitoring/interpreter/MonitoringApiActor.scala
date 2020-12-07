package com.gaucho.infrastructure.monitoring.interpreter

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.scaladsl.{BroadcastHub, Keep, Source, _}

import scala.concurrent.duration.DurationInt
import akka.Done
import akka.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import com.gaucho.infrastructure.monitoring.interpreter.MonitoringApiActor.{MonitoringData, MonitoringScreen}

import collection.mutable.{Map => MMap}
object MonitoringApiActor {
  sealed trait MonitoringData
  case class MonitoringScreen(
      metrics: MMap[String, BigDecimal]
  ) {
    def prometheusScrapingTarget: Seq[String] =
      metrics.map {
        case (k: String, v: BigDecimal) =>
          s"""$k $v"""
      }.toSeq
  }

  object Gauge {
    case class Increase(name: String) extends MonitoringData
    case class Decrease(name: String) extends MonitoringData
    case class Add(name: String, value: BigDecimal) extends MonitoringData
    case class Substract(name: String, value: BigDecimal) extends MonitoringData
    case class Set(name: String, value: BigDecimal) extends MonitoringData
    case class SetD(name: String, value: Double) extends MonitoringData
  }

  object Counter {
    case class Increase(name: String) extends MonitoringData
    case class Reset(name: String) extends MonitoringData
  }
  case class GetMetrics()

  case class MonitoringApiActorRef(ref: ActorRef)
  def start(implicit system: ActorSystem) =
    MonitoringApiActorRef(system.actorOf(Props(new MonitoringApiActor()), "MonitoringApiActor"))
}
class MonitoringApiActor() extends Actor {
  import MonitoringApiActor._
  val metrics: MMap[String, BigDecimal] = MMap.empty

  override def preStart(): Unit = {
    super.preStart()
  }

  def add(name: String, s: BigDecimal) =
    metrics.get(name) match {
      case Some(value) => metrics.put(name, value + s)
      case None => metrics.put(name, 0 + s)
    }
  def substract(name: String, s: BigDecimal) =
    metrics.get(name) match {
      case Some(value) => metrics.put(name, value - s)
      case None => metrics.put(name, 0 - s)
    }
  def set(name: String, s: BigDecimal) =
    metrics.get(name) match {
      case Some(value) => metrics.put(name, s)
      case None => metrics.put(name, s)
    }
  def reset(name: String) =
    set(name, 0)
  def add1(name: String) =
    add(name, 1)
  def substract1(name: String) =
    substract(name, 1)

  def receive: Receive = {
    case m: MonitoringData =>
      m match {
        case Gauge.Increase(name) =>
          add1(name)
        case Gauge.Decrease(name) =>
          substract1(name)
        case Gauge.Add(name, value) =>
          add(name, value)
        case Gauge.Substract(name, value) =>
          substract(name, value)
        case Gauge.Set(name, value) =>
          set(name, value)
        case Gauge.SetD(name, value) =>
          set(name, value)
        case Counter.Increase(name) =>
          add1(name)
        case Counter.Reset(name) =>
          reset(name)
      }
    case GetMetrics() =>
      val r: String = MonitoringScreen(metrics).prometheusScrapingTarget.mkString("\n")
      sender() ! r
  }

}
