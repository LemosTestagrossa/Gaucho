package com.gaucho.infrastructure.persistent_actor

trait WithAggregateRoot {
  def aggregateRoot: String
}
