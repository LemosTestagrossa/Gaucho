package com.gaucho.infrastructure.monitoring.algebra

trait Monitoring {
  def counter(name: String): Counter

  def histogram(name: String): Histogram

  def gauge(name: String): Gauge
}
