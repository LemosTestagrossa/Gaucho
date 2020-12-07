package com.gaucho.domain

case class GroupTopicPartitionOffset(group: String, topic: String, partition: Int, offset: Long) {
  def groupTopicPartition = GroupTopicPartition(group, topic, partition)
}
