package check

import java.text.SimpleDateFormat
import java.util.Date

import calculate.{BaseCalculate, Logit}
import control.{Control, ControlInfo}
import costcompute.{SectionTravelGraph, TimeIntervalStationFlow, TimeIntervalTraffic, TimeIntervalTransferFlow}
import dataload.{BaseDataLoad, HDFSODLoad}
import distribution.ODWithSectionResult
import domain.LinePassengers
import flowdistribute.OdWithTime
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel
import org.slf4j.{Logger, LoggerFactory}
import save._

import scala.collection.JavaConverters._
import scala.util.Try

class CheckOdShare(sparkSession: SparkSession) extends Serializable {
  private val log: Logger = LoggerFactory.getLogger(this.getClass)
  val checkPointPath = s"hdfs://hacluster/checkPoint/${new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())}"
  private var openOdShare = false

  /**
    * 静态分配参数类型，启动静态分配过程
    *
    * @param controlInfo 静态分配参数类型
    */
//  def startup(controlInfo: ControlInfo, odWithTimeRdd: RDD[OdWithTime]): Unit = {
//    //    openOdShare = controlInfo.isOpenShareFunction
//    //    val odSourcePath = controlInfo.getOdSourcePath
//    //    val hDFSODLoad = new HDFSODLoad(odSourcePath)
//    //    val odWithTimeRdd = hDFSODLoad.getOdRdd(sparkSession)
//    /*
//        od是分布式的，如果进行动态分配，一定会有线程安全问题
//        并且shuffle过程会严重影响性能，故分布式应该先计算出所有的路径，对路径进行reduce,再进行单线程分配
//         */
//    /*
//    这里的路网数据应该用广播变量，否则每一个task都会拉取一个路网数据，网络开销非常大
//     */
//    val baseDataLoad = new BaseDataLoad
//    val sc = sparkSession.sparkContext
//    //    k短路搜索服务对象应该用广播变量进行封装才好
//    val kspNumber = controlInfo.getKspNumber
//    val sectionInfoMap = sc.broadcast(baseDataLoad.getSectionInfoMap)
//    val sectionTravelGraph = baseDataLoad.getSectionTravelGraph
//    val baseCalculateBroad = sc.broadcast(new BaseCalculate(baseDataLoad, sectionTravelGraph))
//    val odWithLegalPathAndResultRdd = odWithTimeRdd.map(odWithTime => {
//      val baseCalculate = baseCalculateBroad.value
//      val cost = Try(Control.tryCost(baseCalculate, kspNumber, odWithTime))
//      if (cost.isFailure) {
//        log.error(s"There is A problem in calculating the cost！ because {} and the od is $odWithTime", cost.failed.get.getMessage)
//      }
//      (odWithTime, cost)
//    }).filter(x => x._2.isSuccess).map(x => (x._1, x._2.get))
//      .map(odAndCost => {
//        val odWithTime = odAndCost._1
//        val allCost = odAndCost._2
//        val staticCost = allCost._2
//        val minCost = allCost._3
//        //        这里又要实例化BaseCalculate，消耗非常的大
//        val logitResult = Logit.logit(staticCost, minCost, odWithTime.getPassengers)
//        val lineFlowList = LineSave.lineFlow(logitResult, sectionInfoMap.value)
//        val startTime = odWithTime.getInTime
//        val timeInterval = controlInfo.getTimeInterval
//        val tempResult = Control.createDistributionResult()
//        val result = Control.createDistributionResult()
//        val baseCalculate = baseCalculateBroad.value
//        baseCalculate.distribute(logitResult, startTime, timeInterval, result, tempResult)
//        (odWithTime, lineFlowList, result)
//        //        (odWithTime, logitResult)
//      })
//      .foreach(x => println(x))
//  }
}

object CheckOdShare {
  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession.builder().master("local[*]").getOrCreate()
    val sc = sparkSession.sparkContext
    val odWithTimeRdd: RDD[OdWithTime] = sc.makeRDD(List(new OdWithTime("90", "254", "2021-02-24 16:45:00", 2.0)))
    val checkOdShare = new CheckOdShare(sparkSession)
    val controlInfo = new ControlInfo("", 4)
//    checkOdShare.startup(controlInfo, odWithTimeRdd)
  }
}