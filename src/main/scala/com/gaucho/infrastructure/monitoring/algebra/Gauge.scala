package com.gaucho.infrastructure.monitoring.algebra

trait Gauge {
  def increment(): Unit
  def decrement(): Unit
  def add(num: Int): Unit
  def subtract(num: Int): Unit
  def set(num: Int): Unit
  def set(num: Double): Unit
}
