package com.gaucho.infrastructure.snapshotting

import akka.Done
import scala.concurrent.Future

trait BatchRecoverer[E] {
  def readSnapshots(batchProcessing: Seq[E] => Future[Done]): Future[Done]
}
