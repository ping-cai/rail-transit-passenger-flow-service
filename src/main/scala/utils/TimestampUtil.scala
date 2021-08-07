package utils

import java.sql.Timestamp

object TimestampUtil extends Serializable {
  val Minutes = 60000.0
  val defaultGranularity = 15

  def timeAgg(inputTime: Timestamp, aggGranularity: Int): Timestamp = {
    val currentTime = inputTime.getTime
    val aggTime = (currentTime / Minutes / aggGranularity).toLong
    new Timestamp(aggTime * Minutes.toInt * aggGranularity)
  }

  def timeAgg(inputTime: Timestamp): Timestamp = {
    val currentTime = inputTime.getTime
    val aggTime = (currentTime / Minutes / defaultGranularity).toLong
    new Timestamp(aggTime * Minutes.toInt * defaultGranularity)
  }
}