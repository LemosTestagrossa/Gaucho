package com.gaucho.infrastructure.http

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server._

object AkkaHttpServer {

  case class StopAkkaHttpServer()

  def start(routes: Route, host: String, port: Int)(ctx: ActorSystem): Future[Http.ServerBinding] = {

    implicit val system: ActorSystem = ctx
    implicit val ec: ExecutionContext = system.dispatcher

    val server = Http()(system)
      .bindAndHandle(routes, host, port)

    server.onComplete {
      case Success(bound) =>
        ctx.log.info(
          s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/"
        )
      case Failure(e) =>
        ctx.log.error("Server could not start!")
        e.printStackTrace()
    }

    server

  }
}
