import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.types._

class ReadCsvToOracle {

}

object ReadCsvToOracle {
  private var url: String = "jdbc:oracle:thin:@localhost:1521:ORCL"

  def main(args: Array[String]): Unit = {
    loadOD(20210201,20210207,"KALMAN_OD_TEST")
  }

  def loadStatic(): Unit = {
    val sparkSession: SparkSession = SparkSession.builder()
      .master("local[*]")
      .appName("loadStatic")
      .getOrCreate()
    val schema: StructType = StructType(
      Array(
        StructField("START_TIME", StringType, nullable = true),
        StructField("END_TIME", StringType, nullable = true),
        StructField("SECTION_ID", IntegerType, nullable = true),
        StructField("IN_ID", StringType, nullable = true),
        StructField("OUT_ID", StringType, nullable = true),
        StructField("PASSENGERS", DoubleType, nullable = true),
      )
    )
    val dataFrame: DataFrame = sparkSession.
      read
      .schema(schema)
      .csv("G:/Destop/Section-result-15.csv")
    val prop = new java.util.Properties()
    prop.put("user", "scott")
    prop.put("password", "tiger")
    dataFrame.write.mode("append").jdbc(url, "SECTION_FLOW", prop)
    sparkSession.stop()
  }

  def loadErrorOd(): Unit = {
    val sparkSession: SparkSession = SparkSession.builder()
      .master("local[*]")
      .appName("loadErrorOd")
      .getOrCreate()
    val schema: StructType = StructType(
      Array(
        StructField("IN_ID", StringType, nullable = true),
        StructField("OUT_ID", StringType, nullable = true),
      )
    )
    val dataFrame: DataFrame = sparkSession.
      read
      .schema(schema)
      .csv("G:/Destop/line-result-15.csvErrorOd")
    val prop = new java.util.Properties()
    prop.put("user", "scott")
    prop.put("password", "tiger")
    dataFrame.write.mode("append").jdbc(url, "ERROR_OD", prop)
    sparkSession.stop()
  }

  def loadOD(startInt: Int, endInt: Int, targetTable: String): Unit = {
    val sparkSession: SparkSession = SparkSession.builder()
      .master("local[*]")
      .appName("loadOD")
      .getOrCreate()
    val schema: StructType = StructType(
      Array(
        StructField("IN_ID", StringType, nullable = true),
        StructField("START_TIME", StringType, nullable = true),
        StructField("OUT_ID", StringType, nullable = true),
        StructField("END_TIME", StringType, nullable = true),
        StructField("PASSENGERS", DoubleType, nullable = true)
      )
    )
    val prop = new java.util.Properties()
    prop.put("user", "scott")
    prop.put("password", "tiger")
    val interval = endInt - startInt
    for (i <- 0 to interval) {
      val tempInt = startInt + i
      val dataFrame: DataFrame = sparkSession.
        read
        .schema(schema)
        .csv(s"G:/Destop/od-$tempInt-Aggregation-15.csv")
      dataFrame.write.mode("append").jdbc(url, targetTable, prop)
    }
    sparkSession.stop()
  }
}