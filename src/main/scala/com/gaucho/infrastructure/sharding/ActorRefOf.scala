package com.gaucho.infrastructure.sharding

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.AskableActorRef
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

case class ActorRefOf[P](underlying: ActorRef, name: String) {
  def tell(msg: P)(implicit sender: ActorRef = Actor.noSender): Unit =
    underlying.tell(msg, sender)

  private val askTimeout: Timeout = 5 seconds
  def ask(msg: P)(implicit sender: ActorRef = Actor.noSender, timeout: Timeout = askTimeout): Future[Any] =
    new AskableActorRef(underlying).ask(msg)
}

object ActorRefOf {
  case class RequirementsOf[P, R](requirements: R)
  case class PropsOf[P, R](props: RequirementsOf[P, R] => Props)
  case class AggregateRoot[P](aggregateRoot: P => String)
}
