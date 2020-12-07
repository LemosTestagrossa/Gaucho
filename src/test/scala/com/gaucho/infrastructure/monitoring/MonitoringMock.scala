package com.gaucho.infrastructure.monitoring

import com.gaucho.infrastructure.monitoring.algebra.{Counter, Gauge, Histogram, Monitoring}

class MonitoringMock extends Monitoring {

  class C extends Counter {
    override def increment(): Unit = ()

    override def add(num: Int): Unit = ()
  }
  class G extends Gauge {
    override def increment(): Unit = ()

    override def add(num: Int): Unit = ()

    override def decrement(): Unit = ()

    override def subtract(num: Int): Unit = ()

    override def set(num: Int): Unit = ()
    override def set(num: Double): Unit = ()
  }
  override def counter(name: String): Counter = new C

  override def histogram(name: String): Histogram = ???

  override def gauge(name: String): Gauge = new G
}
