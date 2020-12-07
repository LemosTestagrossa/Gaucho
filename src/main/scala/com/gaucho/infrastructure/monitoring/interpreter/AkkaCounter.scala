package com.gaucho.infrastructure.monitoring.interpreter

import com.gaucho.infrastructure.monitoring.algebra.Counter
import com.gaucho.infrastructure.monitoring.interpreter.MonitoringApiActor.MonitoringApiActorRef

class AkkaCounter(name: String)(implicit monitoringApiActorRef: MonitoringApiActorRef) extends Counter {

  override def increment(): Unit = monitoringApiActorRef.ref ! MonitoringApiActor.Gauge.Increase(name)

  override def add(num: Int): Unit = monitoringApiActorRef.ref ! MonitoringApiActor.Gauge.Add(name, num)
}
