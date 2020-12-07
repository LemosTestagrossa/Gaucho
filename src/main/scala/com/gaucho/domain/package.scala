package com.gaucho

import com.gaucho.domain.Event.Snapshot

package object domain {

  import io.circe._
  import io.circe.generic.auto._
  import io.circe.parser._
  import io.circe.syntax._
  implicit def EventToJson(instance: Event): String =
    instance.asJson.noSpaces
  implicit def EventFromJson(json: String): Either[Error, Event] =
    decode[Event](json)

  implicit def SnapshotToJson(instance: Snapshot): String =
    instance.asJson.noSpaces
  implicit def SnapshotFromJson(json: String): Either[Error, Snapshot] =
    decode[Snapshot](json)

  implicit def longToString(l: Long): String = l.toString
  implicit def bigIntToString(l: BigInt): String = l.toString

  implicit def GroupTopicPartitionToJson(instance: GroupTopicPartition): String =
    instance.asJson.noSpaces
  implicit def GroupTopicPartitionFromJson(json: String): Either[Error, GroupTopicPartition] =
    decode[GroupTopicPartition](json)

  implicit def GroupTopicPartitionOffsetToJson(instance: GroupTopicPartitionOffset): String =
    instance.asJson.noSpaces
  implicit def GroupTopicPartitionOffsetFromJson(json: String): Either[Error, GroupTopicPartitionOffset] =
    decode[GroupTopicPartitionOffset](json)

  implicit def MessageWithOffsetToJson(instance: MessageWithOffset): String =
    instance.asJson.noSpaces
  implicit def MessageWithOffsetFromJson(json: String): Either[Error, MessageWithOffset] =
    decode[MessageWithOffset](json)
}
