import java.util

import entity.GraphWithSectionAssociation
import flowdistribute.{LogitModelComputeService, OdWithTime}
import flowreduce.SectionComputeService
import kspcalculation._
import org.apache.spark.sql.SparkSession

import scala.collection.JavaConverters._

class TestKPathCompute {
  private var url: String = "jdbc:oracle:thin:@localhost:1521:ORCL"
  private var originTable: String = _

  def this(url: String, originTable: String) {
    this()
    this.url = url
    this.originTable = originTable
  }

  def getRoadInfo(): Unit = {
    val sparkSession = SparkSession.builder()
      .master("local[*]")
      .appName("calculate.AccessRoadInfo")
      .getOrCreate()
    val prop = new java.util.Properties()
    prop.put("user", "scott")
    prop.put("password", "tiger")
    val dataFrame = sparkSession.read.jdbc(url, "\"dic_section\"", prop)
    dataFrame.createOrReplaceTempView("dic_section")
    val sectionOnlyIds = new java.util.ArrayList[SectionIntData]()
    val list = dataFrame.select("QJ_ID", "CZ1_ID", "CZ2_ID", "QJ_SXX", "QJ_LENGTH").collect
    val sectionOnlyMap = new java.util.HashMap[String, SectionIntData]()
    list.map(x => {
      val sectionId = Integer.parseInt(x.getDecimal(0).toString)
      val weight = java.lang.Double.parseDouble(x.getDecimal(4).toString)
      val fromId = Integer.parseInt(x.getDecimal(1).toString)
      val toId = Integer.parseInt(x.getDecimal(2).toString)
      val direction = x.getString(3)
      val sectionOnlyId = new SectionIntData(sectionId, weight, fromId, toId, direction)
      sectionOnlyMap.put(s"$fromId $toId", sectionOnlyId)
      sectionOnlyIds.add(sectionOnlyId)
    })
    val stationFrame = sparkSession.read.jdbc(url, "\"dic_station\"", prop)
    stationFrame.createOrReplaceTempView("dic_station")
    val transferStationSql = "select origin.CZ_ID in_id,target.CZ_ID out_id " +
      "FROM (SELECT CZ_ID,CZ_NAME FROM dic_station " +
      "WHERE CZ_NAME " +
      "in (SELECT CZ1_NAME " +
      "FROM (select DISTINCT CZ1_ID,CZ1_NAME " +
      "FROM dic_section) GROUP BY CZ1_NAME HAVING count(*)>=2)) origin " +
      "JOIN dic_station target on origin.CZ_NAME=target.CZ_NAME " +
      "WHERE origin.CZ_ID <>target.CZ_ID GROUP BY origin.CZ_ID,target.CZ_ID  " +
      "ORDER BY\norigin.CZ_ID"
    val transferStation = sparkSession.sql(transferStationSql).select("in_id", "out_id")
    val transferNodeMap = new java.util.HashMap[String, String]()
    transferStation.collect
      .map(x => {
        val sectionId = sectionOnlyIds.size() + 1
        val weight = 0.001
        val fromId = Integer.parseInt(x.getDecimal(0).toString)
        val toId = Integer.parseInt(x.getDecimal(1).toString)
        val direction = "0"
        val sectionOnlyId = new SectionIntData(sectionId, weight, fromId, toId, direction)
        sectionOnlyIds.add(sectionOnlyId)
        sectionOnlyMap.put(s"$fromId $toId", sectionOnlyId)
        transferNodeMap.put(s"$fromId $toId", direction)
      })
    val edges = KspServiceImpl.getEdgesOfId(sectionOnlyIds)
    val graph = new Graph(edges)
    val odWithTime = new OdWithTime("50", "200", "2018-09-01 16:00:00", "2018-09-01 16:15:00", 1.0)
    val lineFrame = dataFrame.select("CZ1_ID", "CZ2_ID", "QJ_LJM").collect
    val lineMap = new java.util.HashMap[String, String]()
    lineFrame.map(x => {
      val fromId = Integer.parseInt(x.getDecimal(0).toString)
      val toId = Integer.parseInt(x.getDecimal(1).toString)
      val lineName = x.getString(2)
      lineMap.put(s"$fromId $toId", lineName)
    })
  }

  def graphLoad(): Unit = {
    val sparkSession = SparkSession.builder()
      .master("local[*]")
      .appName("TestKPathCompute")
      .getOrCreate()
    val prop = new java.util.Properties()
    prop.put("user", "scott")
    prop.put("password", "tiger")
    val dataFrame = sparkSession.read.jdbc(url, "\"dic_section\"", prop)
    val stationFrame = sparkSession.read.jdbc(url, "\"dic_station\"", prop)
    val liveStation = sparkSession.read.jdbc(url, "LIVESTATION", prop)
    liveStation.createOrReplaceTempView("live_station")
    val liveSelect = "SELECT QJ_ID,CZ1_ID,CZ2_ID,QJ_SXX,QJ_LENGTH,QJ_LJM " +
      "FROM dic_section origin " +
      "JOIN live_station target1 on origin.CZ1_NAME=target1.IN_NAME AND origin.QJ_LJM=target1.LINE " +
      "JOIN live_station target2 ON origin.CZ2_NAME=target2.IN_NAME AND origin.QJ_LJM=target2.LINE"
    dataFrame.createOrReplaceTempView("dic_section")
    val liveSectionFrame = sparkSession.sql(liveSelect)
    val sectionHashList = new java.util.ArrayList[SectionStrData]()
    val list = liveSectionFrame.select("QJ_ID", "CZ1_ID", "CZ2_ID", "QJ_SXX", "QJ_LENGTH", "QJ_LJM").collect
    val sectionOnlyMap = new java.util.HashMap[String, SectionStrData]()
    val sectionInfoMap = new util.HashMap[String, SectionAllInfo]()

    list.map(x => {
      val sectionId = Integer.parseInt(x.getDecimal(0).toString)
      val weight = java.lang.Double.parseDouble(x.getDecimal(4).toString)
      val fromId = x.getDecimal(1).toString
      val toId = x.getDecimal(2).toString
      val direction = x.getString(3)
      val lineName = x.getString(5)
      val sectionHash = new SectionStrData(sectionId, weight, fromId, toId, direction)
      sectionOnlyMap.put(s"$fromId $toId", sectionHash)
      val info = new SectionAllInfo(sectionId, weight, lineName, direction)
      sectionInfoMap.put(s"$fromId $toId", info)
      sectionHashList.add(sectionHash)
    })

    stationFrame.createOrReplaceTempView("dic_station")
    val transferStationSql = "select origin.CZ_ID in_id,target.CZ_ID out_id " +
      "FROM (SELECT CZ_ID,CZ_NAME FROM dic_station " +
      "WHERE CZ_NAME " +
      "in (SELECT CZ1_NAME " +
      "FROM (select DISTINCT CZ1_ID,CZ1_NAME " +
      "FROM dic_section) GROUP BY CZ1_NAME HAVING count(*)>=2)) origin " +
      "JOIN dic_station target on origin.CZ_NAME=target.CZ_NAME " +
      "WHERE origin.CZ_ID <>target.CZ_ID GROUP BY origin.CZ_ID,target.CZ_ID  " +
      "ORDER BY\norigin.CZ_ID"
    val transferStation = sparkSession.sql(transferStationSql).select("in_id", "out_id")
    transferStation.collect
      .map(x => {
        val sectionId = sectionHashList.size() + 1
        val weight = 0.001
        val fromId = x.getDecimal(0).toString
        val toId = x.getDecimal(1).toString
        val direction = "0"
        val sectionHash = new SectionStrData(sectionId, weight, fromId, toId, direction)
        val info = new SectionAllInfo(sectionId, weight, direction)
        sectionInfoMap.put(s"$fromId $toId", info)
        sectionHashList.add(sectionHash)
        sectionOnlyMap.put(s"$fromId $toId", sectionHash)
      })
    val edges = SectionStrData.getEdgesOfId(sectionHashList)
    val graph = new Graph(edges)
    val odWithTime = new OdWithTime("100", "200", "2018-09-01 16:00:00", "2018-09-01 17:15:00", 10.0)
    val paths = new PathComputeService(10).fixedPathOdCompute(graph, odWithTime)
    val associationMap = new SectionAssociationMap(sectionInfoMap)
    val pathWithTransferNum = PathComputeService.removeTheFirstAndLastTransfer(associationMap, paths)
    val kPathResult = new KPathResult(odWithTime, pathWithTransferNum)
    val service = new LogitModelComputeService("dynamic")
    val logitComputeResult = LogitModelComputeService.logit(kPathResult, service)
    val graphWithSectionAssociation = new GraphWithSectionAssociation(graph, associationMap)
    val timeMapContainSectionMap = SectionComputeService.getSectionBylogitResult(logitComputeResult, graphWithSectionAssociation)
    val map = timeMapContainSectionMap.getLineResultMap
    val lineSaves = SectionComputeService.lineSave(map)
    val lineResultScala = lineSaves.asScala
    val lineResultRdd = sparkSession.sparkContext.makeRDD(lineResultScala)
    lineResultRdd
      .filter(x => x.getLineName != null)
      .map(x => {
        s"${x.getStartTime}, ${x.getEndTime}, ${x.getLineName}, ${x.getFlow}"
      }).foreach(x => println(x))
  }
}

object TestKPathCompute {
  def main(args: Array[String]): Unit = {
    val compute = new TestKPathCompute()
    compute.graphLoad()
  }

}
