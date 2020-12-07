package com.gaucho.infrastructure.sharding

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import ActorRefOf.{AggregateRoot, PropsOf, RequirementsOf}
import com.typesafe.config.Config

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Try

object GuardianSystem {
  private val name: String = "GuardianProtocol"

  sealed trait GuardianProtocol
  private case class ActorOf[P, R](props: PropsOf[P, R], r: RequirementsOf[P, R], name: String) extends GuardianProtocol

  private val propsOf: PropsOf[GuardianProtocol, Config] = PropsOf(r => Props(new GuardianSystem(r.requirements)))

  case class GuardianSystemRef(ref: ActorRefOf[GuardianProtocol]) {
    def actorOf[P, R](r: RequirementsOf[P, R])(
        implicit root: AggregateRoot[P],
        props: PropsOf[P, R]
    ): RuntimeActorRefOf[P] = RuntimeActorRefOf { p =>
      Await.result(
        ref.ask(ActorOf[P, R](props, r, root.aggregateRoot(p))).mapTo[ActorRefOf[P]],
        5 seconds
      )
    }
  }

  case class RuntimeActorRefOf[P: AggregateRoot](ref: P => ActorRefOf[P]) {
    def tell(msg: P)(implicit sender: ActorRef = Actor.noSender): Unit =
      ref(msg).tell(msg)

    def !(msg: P)(implicit sender: ActorRef = Actor.noSender): Future[Unit] =
      Future.successful(tell(msg))

    def ?(msg: P)(implicit sender: ActorRef = Actor.noSender): Future[Any] =
      ref(msg).ask(msg)
  }

  def create(r: RequirementsOf[GuardianProtocol, Config])(implicit a: ActorSystem): GuardianSystemRef =
    GuardianSystemRef(
      ActorRefOf[GuardianProtocol](
        a.actorOf(propsOf.props(r), name),
        name
      )
    )
}

private class GuardianSystem(config: Config) extends Actor with ActorLogging {
  import GuardianSystem._
  override def preStart(): Unit = {
    super.preStart()
    log.info("executing preStart of actor: {} with config: {}", self.path, config)
  }
  override def postStop(): Unit = {
    log.info("executing postStop of actor: {}", self.path)
    super.postStop()
  }
  override def receive: Receive = {
    case ActorOf(propsOf: PropsOf[_, _], r: RequirementsOf[_, _], _name: String) =>
      sender ! actorOf(propsOf, r, _name)
  }

  private implicit val selectionTimeOut: Timeout = 5 second
  private def actorOf[P, R](propsOf: PropsOf[P, R], r: RequirementsOf[P, R], name: String): ActorRefOf[P] =
    Try(ActorRefOf[P](context.actorOf(propsOf.props(r), name), name)).getOrElse(
      ActorRefOf[P](
        Await.result(
          context.actorSelection(self.path / name).resolveOne(),
          selectionTimeOut.duration
        ),
        name
      )
    )
}
