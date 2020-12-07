package com.gaucho.infrastructure.monitoring.interpreter

import akka.NotUsed
import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source}
import akka.http.scaladsl.Http
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model.sse.ServerSentEvent

import scala.concurrent.duration._
import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME

import akka.pattern.ask
import akka.actor._
import akka.http.scaladsl.server._
import com.gaucho.infrastructure.monitoring.interpreter.MonitoringApiActor.{GetMetrics, MonitoringApiActorRef}
import Directives._
import akka.util.Timeout
import com.gaucho.infrastructure.http.AkkaHttpServer
import scala.concurrent.Future

object MetricsApi {

  def start(implicit system: ActorSystem,
            timeout: Timeout,
            monitoringApiActor: MonitoringApiActorRef): Future[Http.ServerBinding] = {
    val route = get {
      path("metrics") {
        complete {
          (monitoringApiActor.ref ? GetMetrics()).mapTo[String]
        }
      }
    }
    AkkaHttpServer.start(route, "0.0.0.0", 9095)(system)

  }

}
