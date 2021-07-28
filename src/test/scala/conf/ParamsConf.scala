package conf

import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.StringDeserializer

object ParamsConf {
  private val conf = ConfigFactory.load()
  val topics = conf.getString("kafka.topic").split(",")
  val groupId = conf.getString("kafka.group.id")
  val brokers = conf.getString("kafka.broker.list")
  val kafkaParams = Map[String, Object](
    "bootstrap.servers" -> brokers,
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> groupId,
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )
  val produceTopic: Array[String] = conf.getString("kafka.producer.topic").split(",")
  val kafkaCheckPoint: String = conf.getString("kafka.checkPoint")
}
