package KPathCompute

import java.util.concurrent.TimeUnit
import java.{lang, util}

import batch.back.KPathTrait
import conf.ParamsConf
import dataload.BaseDataLoad
import distribution.{DistributionResult, StationWithType}
import kspcalculation.PathService
import model.back.{EdgeModel, ODWithTimeModel}
import model.{back, _}
import model.dto.{SectionLoadDTO, StationLoadDTO}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.from_json
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.types.{DataTypes, StructType}
import utils.TimeKey

import scala.collection.JavaConverters._
import scala.util.parsing.json._


class KPathService(baseDataLoad: BaseDataLoad, kspNum: Int) extends KPathTrait {
  private val pathService: PathService = new PathService(kspNum, baseDataLoad.getGraph, baseDataLoad.getSectionWithDirectionMap)

  override def compute(ODModel: back.ODModel): List[back.PathModel] = {
    val legalKPath = pathService.getLegalKPath(ODModel.inId, ODModel.outId)
    legalKPath.asScala.flatMap(path => {
      val edges = path.getEdges
      var totalCost = 0.0
      val edgeModelList = edges.asScala.flatMap(edge => {
        totalCost += edge.getWeight
        List(EdgeModel(edge.getFromNode, edge.getToNode, edge.getWeight))
      }).toList
      List(back.PathModel(edgeModelList, totalCost))
    }).toList
  }
}

object KPathService {
  def main(args: Array[String]): Unit = {
    testKPathService()
  }

  def testKPathService(): Unit = {
    val sparkSession = SparkSession.builder().appName("KPathService").master("local[*]").getOrCreate()
    val baseDataLoad = new BaseDataLoad
    val kPathService = new KPathService(baseDataLoad, 4)
    val oDModel = ODWithTimeModel("266", "249", "", "")
    kPathService.compute(oDModel).foreach(x => println(x))
  }

  def testKPathServiceStreaming(): Unit = {
    val sparkSession = SparkSession.builder().master("local[*]").appName("testKPathServiceStreaming").getOrCreate()
    sparkSession.sparkContext.setLogLevel("WARN")
    val brokers = ParamsConf.brokers
    val topics = ParamsConf.topics
    //接收数据
    val schema: StructType = new StructType()
      .add("name", DataTypes.StringType)
      .add("age", DataTypes.StringType)
      .add("hobby", DataTypes.StringType)
    import sparkSession.implicits._
    val kafkaStreaming = sparkSession.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", brokers)
      .option("subscribe", topics(0))
      .option("startingOffsets", "latest")
      .option("enable.auto.commit", "false")
      .option("auto.commit.interval.ms", "1000")
      .load()
    val kafkaData = kafkaStreaming.selectExpr("CAST(key AS STRING)", "CAST (value AS STRING) as json")
      .as[(String, String)]
      .filter(x => JSON.parseFull(x._2).isDefined)
      .select(from_json($"json", schema = schema).alias("data"))
      .select("data.name", "data.age", "data.hobby")
    //    val kafkaData = kafkaStreaming.select(from_json($"value".cast(StringType), schema))

    val query = kafkaData.writeStream.trigger(Trigger.ProcessingTime(10, TimeUnit.SECONDS)).outputMode("append").queryName("table")
      .format("console")
      .start()
    query.awaitTermination()
    sparkSession.stop()
  }

  private def getSectionLoad(distributionResult: Broadcast[DistributionResult], tempResult: DistributionResult) = {
    val sectionTraffic = distributionResult.value.getTimeIntervalTraffic.getTimeSectionTraffic
    val timeWithSectionLoads = tempResult.getTimeIntervalTraffic.getTimeSectionTraffic.asScala.flatMap(timeSection => {
      val timeKey = timeSection._1
      val timeSectionMap = timeSection._2.asScala.map(sectionFlow => {
        val section = sectionFlow._1
        val flow = sectionFlow._2
        val passengers = sectionTraffic.get(timeKey).get(section)
        val trainNum = 6
        val timeWithSectionLoad = TimeWithSectionLoad(TimeKeyModel(timeKey.getStartTime, timeKey.getEndTime),
          SectionLoadDTO(section.getInId, section.getOutId, flow, trainNum, passengers, passengers / 3000))
        timeWithSectionLoad
      })
      timeSectionMap
    })
    timeWithSectionLoads
  }

  private def getStationLoads(distributionResult: Broadcast[DistributionResult], tempResult: DistributionResult) = {
    val dynamicAllTimeStationTraffic: util.Map[TimeKey, util.Map[StationWithType, lang.Double]] = distributionResult.value.getTimeIntervalStationFlow.getTimeStationTraffic
    val timeWithStationLoads = tempResult.getTimeIntervalStationFlow.getTimeStationTraffic.asScala.flatMap(timeStation => {
      val timeKey = timeStation._1
      val stationFlowMap = timeStation._2
      val stationLoads = stationFlowMap.asScala.map(stationFlow => {
        val station = stationFlow._1
        val flow = stationFlow._2
        if ("in".equals(station.getType)) {
          val stationLoad = StationLoadDTO(station.getStationId, flow, flow, 0, dynamicAllTimeStationTraffic.get(timeKey).get(station), flow / 3000)
          TimeWithStationLoad(TimeKeyModel(timeKey.getStartTime, timeKey.getEndTime), stationLoad)
        } else {
          val stationLoad = StationLoadDTO(station.getStationId, flow, 0, flow, dynamicAllTimeStationTraffic.get(timeKey).get(station), flow / 3000)
          TimeWithStationLoad(TimeKeyModel(timeKey.getStartTime, timeKey.getEndTime), stationLoad)
        }
      })
      stationLoads
    })
    timeWithStationLoads
  }
}
