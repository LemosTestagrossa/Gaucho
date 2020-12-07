package com.gaucho.infrastructure.monitoring.interpreter

import com.gaucho.infrastructure.monitoring.algebra.{Counter, Gauge, Histogram, Monitoring}
import com.gaucho.infrastructure.monitoring.interpreter.MonitoringApiActor.MonitoringApiActorRef

class AkkaBasedMonitoring(implicit monitoringApiActorRef: MonitoringApiActorRef) extends Monitoring {

  override def counter(name: String): Counter =
    new AkkaCounter(name)

  override def histogram(name: String): Histogram =
    ???

  override def gauge(name: String): Gauge =
    new AkkaGauge(name)

}
