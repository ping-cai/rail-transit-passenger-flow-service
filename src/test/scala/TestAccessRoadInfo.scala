import calculate.AccessRoadInfo
import dataload.base.{OracleChongQingLoad, OracleSectionLoad, OracleTransferLoad, SectionTravelTimeLoad}
import flowdistribute.OdWithTime
import kspcalculation.PathComputeService
import org.apache.spark.sql.SparkSession

class TestAccessRoadInfo {

}

object TestAccessRoadInfo {
  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession.builder().master("local[*]").getOrCreate()
    val accessRoadInfo = new AccessRoadInfo()
    val sectionLoad = new OracleSectionLoad()
    val transferLoad = new OracleTransferLoad()
    val oracleCHONGQINGLoad = new OracleChongQingLoad()
    val sectionTravelTimeLoad = new SectionTravelTimeLoad()
    val association = accessRoadInfo.sectionLoad(sectionLoad, transferLoad, sectionTravelTimeLoad)
    val graph = association.getGraph
    val odTime = new OdWithTime("100", "125", "2021-01-02 15:17:35", "2021-01-02 15:16:35", 3.0)
    val paths = new PathComputeService(10).fixedPathOdCompute(graph, odTime)
    paths.forEach(x => println(x))
  }
}