import dataload.base.{OracleChongQingLoad, OracleSectionLoad, OracleTransferLoad}
import org.apache.spark.sql.SparkSession

class LoadTest extends Serializable {

}

object LoadTest {
  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession.builder()
      .master("local[*]")
      .appName("LoadTest")
      .getOrCreate()
    val sectionLoad = new OracleSectionLoad()
    val transferLoad = new OracleTransferLoad()
    val oracleCHONGQINGLoad = new OracleChongQingLoad()
    val chongqingFrame = oracleCHONGQINGLoad.load()
    import sparkSession.implicits._
    val sectionFrame = chongqingFrame.rdd.flatMap(x => {
      val sections = List(SectionSave(627, x.getString(1), "天生", "Tiansheng", "6号线"))
      sections
    })
    val frame = sectionFrame.toDF()
    frame.write.csv("G:/Destop/result.csv")

  }

}

case class SectionSave(stationId: Int, Type: String, czName: String, pyName: String, lineName: String)
