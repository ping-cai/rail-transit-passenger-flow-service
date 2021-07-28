import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{StringType, StructField, StructType, TimestampType}

class TestOd {

}

object TestOd {
  def main(args: Array[String]): Unit = {
    checkOdIntegrity()
  }
  def checkOdIntegrity(): Unit = {
    val sparkSession: SparkSession = SparkSession.builder()
      .master("local[*]")
      .appName("TestOd").getOrCreate()
    val schema: StructType  = StructType(
      Array(
        StructField("id", StringType, nullable = true),
        StructField("type", StringType, nullable = true),
        StructField("in_id", StringType, nullable = true),
        StructField("in_time", TimestampType, nullable = true),
        StructField("out_id", StringType, nullable = true),
        StructField("out_time", TimestampType, nullable = true)
      )
    )
    val odFrame = sparkSession.read.schema(schema).csv("G:/Destop/od-20210131-result.csv")
    odFrame.createTempView("od")
    val distinctId = "select in_id from od group by in_id"
    val onlyIdFrame = sparkSession.sql(distinctId)
    onlyIdFrame.createTempView("onlyId")
    val countSql = "select count(*) from onlyId"
    val countFrame = sparkSession.sql(countSql)
    countFrame.show()
//    onlyIdFrame.show(1000)
  }

}