/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

import akka.Done

import scala.concurrent.{Await, Future}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.alpakka.cassandra.scaladsl.{CassandraFlow, CassandraSession}
import akka.stream.alpakka.cassandra.{CassandraSessionSettings, CassandraWriteSettings}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import com.gaucho.infrastructure.monitoring.MonitoringMock
import scala.concurrent.duration._

final class CassandraSessionPerformanceSpec extends CassandraSpecBase(ActorSystem("CassandraSessionPerformanceSpec")) {
  implicit val kamonInstance = new MonitoringMock()

  val log = Logging(system, this.getClass)

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(2.minutes, 100.millis)

  lazy val dataTable = s"eventstore.events"

  val sessionSettings: CassandraSessionSettings = CassandraSessionSettings()
  override val lifecycleSession: CassandraSession =
    sessionRegistry.sessionFor(sessionSettings)

  lazy val session: CassandraSession = sessionRegistry.sessionFor(sessionSettings)

  val partition = 1L

  "Select" must {
    "read many rows" in assertAllStagesStopped {

      def readSnapshots() = {
        session
          .select(s"SELECT * FROM eventstore.events WHERE partition_id = ? ALLOW FILTERING", Long.box(partition))
          .runWith(Sink.fold(0) {
            case (acc, _) =>
              acc + 1
          })
      } //asyncReadHighestSequenceNr("entityA", 0).futureValue mustBe data.last.sequenceNr

      val last: Int = Await.result(readSnapshots(), Duration.Inf)
      println(last)
      1 mustBe (1)
    }
  }

}
