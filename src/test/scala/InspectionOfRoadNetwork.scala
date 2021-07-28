import java.util

import calculate.StaticCalculate
import costcompute.{Section, SectionTravelGraph, TimeIntervalTraffic}
import dataload.base.OracleChongQingLoad
import dataload.{BaseDataLoad, Load}
import domain.dto.SectionResult
import flowdistribute.OdWithTime
import kspcalculation.PathComputeService
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import utils.TimeKey

import scala.collection.JavaConverters._

case class SectionOnly(inId: String, outId: String)

class InspectionOfRoadNetwork {

}

//测试路网存在的问题
object InspectionOfRoadNetwork {
//  def main(args: Array[String]): Unit = {
//    val sparkSession = SparkSession.builder().master("local[*]").getOrCreate()
//
//    val odWithTimeRdd = loadOd(sparkSession)
//    val baseDataLoad = new BaseDataLoad
//    val timeSectionTraffic = new util.HashMap[TimeKey, util.Map[Section, java.lang.Double]]()
//    val intervalTraffic = new TimeIntervalTraffic(timeSectionTraffic)
//    val staticCalculate = new StaticCalculate
//    val resultRdd = odWithTimeRdd.map(odWithTime => {
//      staticCalculate.calculate(baseDataLoad, intervalTraffic, odWithTime, 15)
//    })
//    val sectionResultRdd: RDD[SectionResult] = resultRdd.flatMap(result => {
//      val sectionTraffic = result.getTimeIntervalTraffic.getTimeSectionTraffic
//      val sectionResultIterable = sectionTraffic.asScala.flatMap(timeKeyWithMap => {
//        val timeKey = timeKeyWithMap._1
//        val sectionMap = timeKeyWithMap._2.asScala
//        val resultIterable = sectionMap.map(sectionFlow => {
//          val section = sectionFlow._1
//          val flow = sectionFlow._2
//          SectionResult(timeKey.getStartTime, timeKey.getEndTime,
//            section.getInId, section.getOutId, flow)
//        })
//        resultIterable
//      })
//      sectionResultIterable
//    })
//    import sparkSession.implicits._
//    val sectionFrame = sectionResultRdd.toDF()
//    sectionFrame.createOrReplaceTempView("sectionResult")
//    val sumFlowSql = "select startTime START_TIME,endTime END_TIME,startStationId START_STATION_ID,endStationId END_STATION_ID,cast(sum(passengers) as int) PASSENGERS from sectionResult group by startTime,endTime,startStationId,endStationId"
//    val resultFrame = sparkSession.sql(sumFlowSql)
//    import sparkSession.implicits._
//    val pathComputeService = initData(baseDataLoad)
//    val sectionRdd = compute(pathComputeService, odWithTimeRdd)
//    val sectionOnly = sectionRdd.toDF()
//    sectionOnly.createOrReplaceTempView("section_table")
//    val selectSectionTable = "select * from section_table group by inId,outId"
//    val url = Load.url
//    val prop = Load.prop
//    sparkSession.sql(selectSectionTable).write.mode("append").jdbc(url, "section_test", prop)
//    resultFrame.write.mode("append").jdbc(url, "SECTION_GRAPH_TEST", prop)
//  }

  def loadOd(sparkSession: SparkSession): RDD[OdWithTime] = {
    val url = Load.url
    val prop = Load.prop
    val afcStationTable = "\"chongqing_stations_nm\""
    val afcStationFrame = sparkSession.read.jdbc(url, afcStationTable, prop)
    afcStationFrame.createOrReplaceTempView("AFC_STATION")
    val afcODSql = "SELECT ORIGIN.STATIONID IN_ID,TARGET.STATIONID OUT_ID FROM AFC_STATION ORIGIN JOIN AFC_STATION TARGET WHERE ORIGIN.STATIONID<>TARGET.STATIONID"
    val odFrame = sparkSession.sql(afcODSql)
    val chongQingLoad = new OracleChongQingLoad()
    val mapIdFrame = chongQingLoad.load()
    mapIdFrame.createOrReplaceTempView("od_transfer")
    odFrame.createOrReplaceTempView("od")
    val odTransferIdSql = "select target1.CZ_ID in_number,target2.CZ_ID out_number " +
      "from od origin join od_transfer target1 on origin.IN_ID=target1.AFC_ID " +
      "join od_transfer target2 on origin.OUT_ID=target2.AFC_ID"
    val odTransferIdFrame = sparkSession.sql(odTransferIdSql)
    val odWithTimeRdd: RDD[OdWithTime] = odTransferIdFrame.rdd.map(od => {
      val inId = od.getDecimal(0).intValue().toString
      val outId = od.getDecimal(1).intValue().toString
      val odWithTime = new OdWithTime(inId, outId, "2021-02-24 15:00:00", 5.0)
      odWithTime
    })
    odWithTimeRdd
  }

  def initData(baseDataLoad: BaseDataLoad): PathComputeService = {
    val graph = baseDataLoad.getGraph
    val pathComputeService = new PathComputeService(3, graph, baseDataLoad.getSectionWithDirectionMap)
    pathComputeService
  }

//  def compute(pathComputeService: PathComputeService, odWithTimeRdd: RDD[OdWithTime]) = {
//    val sectionWithDirectionMap = pathComputeService.getSectionWithDirectionMap
//    val sectionRdd = odWithTimeRdd.flatMap(odWithTime => {
//      val paths = pathComputeService.getLegalKPath(odWithTime.getInId, odWithTime.getOutId)
//      val sectionList = paths.asScala.flatMap(path => {
//        val edges = path.getEdges.asScala
//        val sectionBuffer = edges.map(edge => {
//          val section = Section.createSectionByEdge(edge)
//          if (sectionWithDirectionMap.get(section) != null) {
//            SectionOnly(edge.getFromNode, edge.getToNode)
//          } else {
//            SectionOnly("0", "0")
//          }
//        })
//        sectionBuffer
//      })
//      sectionList
//    })
//    sectionRdd
//  }
}
