package KPathCompute

import java.util.Properties

import batch.back.KPathTrait
import conf.DynamicConf
import costcompute.Section
import dataload.{BaseDataLoad, Load}
import kspcalculation.PathService
import model.back._
import org.apache.spark.sql._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Try

class KPathSave(baseDataLoad: BaseDataLoad, pathNum: Int, sparkSession: SparkSession) extends KPathTrait {
  private val pathService: PathService = new PathService(pathNum, baseDataLoad.getGraph, baseDataLoad.getSectionWithDirectionMap)

  override def compute(ODModel: ODModel): List[PathModel] = {
    val legalKPath = pathService.getLegalKPath(ODModel.inId, ODModel.outId)
    legalKPath.asScala.flatMap(path => {
      val edges = path.getEdges
      var totalCost = 0.0
      val edgeModelList = edges.asScala.flatMap(edge => {
        totalCost += edge.getWeight
        List(EdgeModel(edge.getFromNode, edge.getToNode, edge.getWeight))
      }).toList
      List(PathModel(edgeModelList, totalCost))
    }).toList
  }

  def compute2PathList(odModel: ODModel): List[PathDetails] = {
    val sectionWithDirectionMap = baseDataLoad.getSectionWithDirectionMap
    val legalKPathTry = Try(pathService.getLegalKPath(odModel.inId, odModel.outId))
    if (legalKPathTry.isSuccess) {
      val legalKPath = legalKPathTry.get
      //    换乘次数偏离约束
      val transferTimesDeviation = DynamicConf.transferTimesDeviation
      //    出行费用放大系数约束
      val travelExpenseAmplification = DynamicConf.travelExpenseAmplification
      //    出行费用最大值约束
      val maximumTravelCost = DynamicConf.maximumTravelCost

      val sectionInfoMap = baseDataLoad.getSectionInfoMap
      val pathInfoBuffer = legalKPath.asScala.map(path => {
        val lineSet = mutable.HashSet[String]()
        var tempTransferTime = 0
        val edgeModels = ListBuffer[EdgeModel]()
        path.getEdges.forEach(edge => {
          val section = Section.createSectionByEdge(edge)
          if (null == sectionWithDirectionMap.get(section)) {
            tempTransferTime += 1
          } else {
            val sectionInfo = sectionInfoMap.get(section)
            val line = sectionInfo.getLine
            lineSet.add(line)
          }
          edgeModels append EdgeModel(edge.getFromNode, edge.getToNode, edge.getWeight)
        })
        val totalCost = path.getTotalCost
        val lineSetString = lineSet.mkString("->")
        val pathModel = PathModel(edgeModels.toList, totalCost)
        PathInfo(pathModel, totalCost, tempTransferTime, lineSetString)
      })
      val minCost = pathInfoBuffer.minBy(x => x.cost).cost
      val minTransferTime = pathInfoBuffer.minBy(x => x.transferCount).transferCount
      val tempResult = ListBuffer[PathInfo]()
      pathInfoBuffer foreach (x => {
        if (x.cost <= ((1 + travelExpenseAmplification) * minCost) &&
          x.cost <= (minCost + maximumTravelCost) &&
          x.transferCount <= (minTransferTime + transferTimesDeviation)) {
          tempResult append x
        }
      })
      val pathInfoSorted = tempResult.sortBy(x => x.cost)
      val pathDetailsBuffer = pathInfoSorted.indices.map(x => {
        val inId = (odModel.inId.toInt + 1000).toString
        val outId = (odModel.outId.toInt + 1000).toString
        val pathInfo = pathInfoSorted(x)
        val pathId = s"$inId$outId$x"
        PathDetails(pathId, odModel.inId, odModel.outId, pathInfo)
      })
      pathDetailsBuffer.toList
    } else {
      List[PathDetails]()
    }
  }

  def path2Edges(pathId: String, pathInfo: PathInfo): List[EdgeDetails] = {
    val edgeModelList = pathInfo.path.edgeModel
    val sectionWithDirectionMap = baseDataLoad.getSectionWithDirectionMap
    val result = ListBuffer[EdgeDetails]()
    var tempWeight = 0.0
    val firstEdgeModel = edgeModelList.head
    var firstNode = firstEdgeModel.fromNode
    val lastEdgeModel = edgeModelList.last
    val lastNode = lastEdgeModel.toNode
    edgeModelList.indices.foreach(index => {
      val edgeModel = edgeModelList(index)
      val section = new Section(edgeModel.fromNode, edgeModel.toNode)
      if (null == sectionWithDirectionMap.get(section)) {
        result append EdgeDetails(pathId, result.size, EdgeModel(firstNode, edgeModel.fromNode, tempWeight))
        firstNode = edgeModel.toNode
        tempWeight = 0.0
      } else {
        tempWeight += edgeModel.weight
      }
    })
    result append EdgeDetails(pathId, result.size, EdgeModel(firstNode, lastNode, tempWeight))
    result.toList
  }

  def savePathResult(odModel: ODModel): Unit = {
    val pathList = compute(odModel)
    val pathResult = pathList.indices.map(pathId => {
      ODWithPath(odModel.inId, odModel.outId, pathId, pathList(pathId).toString)
    })
    val url = DynamicConf.localhostUrl
    val prop = new Properties()
    prop.put("user", DynamicConf.localhostUser)
    prop.put("password", DynamicConf.localhostPassword)
    import sparkSession.implicits._
    val dataFrame = pathResult.toDF()
    dataFrame.printSchema()
    dataFrame.write.mode(SaveMode.Append).jdbc(url, "OD_PATH", prop)
  }

  def saveFrame(resultFrame: DataFrame, saveTable: String): Unit = {
    val url = DynamicConf.localhostUrl
    val prop = new Properties()
    prop.put("user", DynamicConf.localhostUser)
    prop.put("password", DynamicConf.localhostPassword)
    resultFrame.write.mode(SaveMode.Append).jdbc(url, saveTable, prop)
  }
}

object KPathSave {
  def main(args: Array[String]): Unit = {
  }

  def pathSave(): Unit = {
    val sparkSession = SparkSession.builder().appName("KPathSave").master("local[*]")
      .config("spark.sql.shuffle.partitions", 100) //设置并行度100
      .getOrCreate()
    val baseDataLoad = new BaseDataLoad
    val pathNum = 6
    val kPathSave = new KPathSave(baseDataLoad, pathNum, sparkSession)

    //    加载需要计算的数据
    //    需要加载的表
    val stationTable = DynamicConf.stationTable
    //    需要加载的数据库连接
    val url = DynamicConf.localhostUrl
    //    需要加载的账户密码
    val prop = Load.prop
    val stationFrame = sparkSession.read.jdbc(url, stationTable, prop)
    stationFrame.createOrReplaceTempView("station")
    val allOdFrame = sparkSession.sql(
      """
        |SELECT
        |	origin.inId,
        |	target.inId outId
        | FROM
        | (SELECT CZ_ID inId FROM station WHERE LJM <> '成都' AND CZ_XZ IS NULL OR CZ_XZ = '折返站') origin,
        |	(SELECT CZ_ID inId FROM station WHERE LJM <> '成都' AND CZ_XZ IS NULL OR CZ_XZ = '折返站') target
        |WHERE
        |	origin.inId <> target.inId
      """.stripMargin)
    import sparkSession.implicits._
    //    得到所有的OD集合
    val odWithTimeArray = allOdFrame.map(od => {
      val inId = od.getDecimal(0).intValue().toString
      val outId = od.getDecimal(1).intValue().toString
      ODWithTimeModel(inId, outId, "", "")
    })
    //    调整并行度
    //    先进行广播变量
    val sc = sparkSession.sparkContext

    val kPathSaveBroad = sc.broadcast(kPathSave)

    //计算所有OD的K短路集合
    //    需要对K短路集合进行编号,先按照费用从小到大排序
    val pathDetailDataSet = odWithTimeArray.flatMap(od => {

      val pathDetailList = kPathSaveBroad.value.compute2PathList(od)
      pathDetailList
    })
    pathDetailDataSet.persist()
    //    val failedResult = pathDetailTry.filter(x=>x.isFailure).map(x=>x.failed.get.getMessage)
    //    failedResult.foreach(x=>println(x))
    //    val pathDetailRdd = pathDetailTry.filter(x => x.isSuccess)
    //      .flatMap(x => x.get)
    val pathInfoFrame = pathDetailDataSet.toDF().select("pathId", "inId", "outId", "pathInfo.cost", "pathInfo.transferCount", "pathInfo.LineSet")
    //    PathDetails(pathId: String, inId: String, outId: String, pathInfo: PathInfo)
    //    PathInfo(path: PathModel, cost: Double, transferCount: Int, LineSet: String)
    //     EdgeDetails(pathId: String, edgeSerial: Int, edgeModel: EdgeModel)
    //    EdgeModel(fromNode: String, toNode: String, weigh: Double)
    //    pathInfoFrame.show(1000)
    val edgeDetailFrame = pathDetailDataSet.flatMap(pathDetails => {
      kPathSaveBroad.value.path2Edges(pathDetails.pathId, pathDetails.pathInfo)
    }).toDF().select("pathId", "edgeSerial", "edgeModel.fromNode", "edgeModel.toNode", "edgeModel.weigh")
    println("开始计算K短路！")
    val startTime = System.currentTimeMillis()
    kPathSave.saveFrame(pathInfoFrame, "K_PATH_DETAIL")
    val endTime = System.currentTimeMillis()
    kPathSave.saveFrame(edgeDetailFrame, "K_EDGE_INFO_DETAIL")
    println(s"存储K短路成功！整个耗时${(endTime - startTime) / 1000}秒")
  }

//  def testPath(): Unit = {
//    val sparkSession = SparkSession.builder().appName("KPathSave").master("local[*]")
//      .config("spark.sql.shuffle.partitions", 100) //设置并行度100
//      .getOrCreate()
//    val baseDataLoad = new BaseDataLoad
//    val pathNum = 6
//    val kPathSave = new KPathSave(baseDataLoad, pathNum, sparkSession)
//
//    //    加载需要计算的数据
//    //    需要加载的表
//    val stationTable = DynamicConf.stationTable
//    //    需要加载的数据库连接
//    val url = DynamicConf.localhostUrl
//    //    需要加载的账户密码
//    val prop = Load.prop
//    val stationFrame = sparkSession.read.jdbc(url, stationTable, prop)
//    stationFrame.createOrReplaceTempView("station")
//    val allOdFrame = List(ODWithTimeModel("1", "25", "", ""))
//    import sparkSession.implicits._
//    //    调整并行度
//    //    先进行广播变量
//    val sc = sparkSession.sparkContext
//
//    val kPathSaveBroad = sc.broadcast(kPathSave)
//
//    //计算所有OD的K短路集合
//    //    需要对K短路集合进行编号,先按照费用从小到大排序
//    val pathDetailDataSet = allOdFrame.flatMap(od => {
//      val pathDetailList = kPathSaveBroad.value.compute2PathList(od)
//      pathDetailList
//    })
//    pathDetailDataSet
//    pathDetailDataSet.foreach(x => println(x))
//  }
}
