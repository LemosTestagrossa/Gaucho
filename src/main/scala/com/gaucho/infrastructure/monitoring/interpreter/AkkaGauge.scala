package com.gaucho.infrastructure.monitoring.interpreter

import com.gaucho.infrastructure.monitoring.algebra.Gauge
import com.gaucho.infrastructure.monitoring.interpreter.MonitoringApiActor.MonitoringApiActorRef

class AkkaGauge(name: String)(implicit monitoringApiActorRef: MonitoringApiActorRef) extends Gauge {

  override def increment(): Unit = monitoringApiActorRef.ref ! MonitoringApiActor.Gauge.Increase(name)
  override def decrement(): Unit = monitoringApiActorRef.ref ! MonitoringApiActor.Gauge.Decrease(name)

  override def add(num: Int): Unit = monitoringApiActorRef.ref ! MonitoringApiActor.Gauge.Add(name, num)
  override def subtract(num: Int): Unit = monitoringApiActorRef.ref ! MonitoringApiActor.Gauge.Substract(name, num)

  override def set(num: Int): Unit = monitoringApiActorRef.ref ! MonitoringApiActor.Gauge.Set(name, num)
  override def set(num: Double): Unit = monitoringApiActorRef.ref ! MonitoringApiActor.Gauge.SetD(name, num)
}
