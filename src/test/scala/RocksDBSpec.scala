import akka.actor.ActorSystem
import org.scalatest.flatspec.AnyFlatSpec
import com.gaucho.infrastructure.rocksdb.RocksDBUtils._
import com.gaucho.domain.Event.Snapshot
import com.gaucho.domain._
import com.gaucho.infrastructure.rocksdb.RocksDBUtils._
import io.circe
import org.scalatest.concurrent.Futures
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import concurrent.ExecutionContext.Implicits.global

class RocksDBSpec extends AnyFlatSpec with Futures {

  implicit val system = ActorSystem("rocksDBSpec")
  "read and write" should "work" in {
    val snapshot = Snapshot(
      "group",
      "topic",
      1,
      deliveryId = 1,
      aggregateRoot = "a",
      json = "hey"
    )

    put(snapshot.event.aggregateRoot, snapshot)

    val decoded: Either[circe.Error, Snapshot] = SnapshotFromJson(load(snapshot.event.aggregateRoot).get)

    decoded.map { decodedSnapshot =>
      assert(decodedSnapshot == snapshot)
    }
  }
}
