package com.gaucho.infrastructure.kafka

import akka.Done
import com.gaucho.domain.MessageWithOffset

import scala.concurrent.Future

trait MessageConsumerLike {
  def consume(
      group: String,
      topic: String,
      partition: Int
  )(
      callback: MessageWithOffset => Unit
  ): Future[Done]
}
