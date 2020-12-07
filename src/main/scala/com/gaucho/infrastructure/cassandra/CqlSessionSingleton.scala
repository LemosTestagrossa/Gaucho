package com.gaucho.infrastructure.cassandra

import akka.actor.ActorSystem
import akka.stream.alpakka.cassandra.CassandraSessionSettings
import akka.stream.alpakka.cassandra.scaladsl.{CassandraSession, CassandraSessionRegistry}

object CqlSessionSingleton {
  private var sessionInstance: CassandraSession = null
  implicit def session(implicit system: ActorSystem): CassandraSession = {
    if (sessionInstance == null) {
      val sessionRegistry = CassandraSessionRegistry.get(system)
      val sessionSettings: CassandraSessionSettings = CassandraSessionSettings()
      val newSession: CassandraSession = sessionRegistry.sessionFor(sessionSettings)
      sessionInstance = newSession
    }
    sessionInstance
  }

}
