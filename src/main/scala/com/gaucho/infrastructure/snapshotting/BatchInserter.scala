package com.gaucho.infrastructure.snapshotting

import akka.Done

import scala.concurrent.Future

trait BatchInserter[E] {
  def insertBatch(snapshots: Seq[E]): Future[Done]
}
