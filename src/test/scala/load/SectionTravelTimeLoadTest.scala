package load

import costcompute.{Section, SectionTravelGraph}
import dataload.base.OracleSectionTravelTimeLoad
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import save.Save

import scala.collection.JavaConverters._


class SectionTravelTimeLoadTest(saveTable: String) extends Serializable {
  def computeAndSaveTravelTime(sparkSession: SparkSession): Unit = {
    val sectionTravelTimeLoad = new OracleSectionTravelTimeLoad()
    val sectionTravelFrame = sectionTravelTimeLoad.load()
    val sectionTravelTimes = sectionTravelTimeLoad.getSectionTravelTimeList(sectionTravelFrame)
    val sectionTravelGraph = SectionTravelGraph.createTravelGraph(sectionTravelTimes)
    val sectionTravelMap = sectionTravelGraph.sectionTravelMap
    val sc = sparkSession.sparkContext
    val travelTimeRdd: RDD[(Section, Integer)] = sc.makeRDD(sectionTravelMap.asScala.toList)
    val sectionTravelTimeRdd: RDD[SectionTravelTime] = travelTimeRdd.map(sectionWithSecond => {
      val section = sectionWithSecond._1
      val time = sectionWithSecond._2
      SectionTravelTime(section.getInId, section.getOutId, time, 1)
    })
    import sparkSession.implicits._
    val result = sectionTravelTimeRdd.toDF()
    result.write.mode("append").jdbc(Save.url, saveTable, Save.prop)
  }

}

object SectionTravelTimeLoadTest {
  def main(args: Array[String]): Unit = {
    var sectionTravelTime = "TRAVEL_SECONDS_TIME"
    val sparkSession = SparkSession.builder().master("local[*]").getOrCreate()
    val sectionTravelTimeLoadTest = new SectionTravelTimeLoadTest(sectionTravelTime)
    sectionTravelTimeLoadTest.computeAndSaveTravelTime(sparkSession)
  }

}

case class SectionTravelTime(IN_ID: String, OUT_ID: String, TIME_SECONDS: Int, RATE: Double)