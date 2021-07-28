import java.util
import java.util.Properties

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
class LoadDataToOracle {
  var url="jdbc:oracle:thin:@10.11.27.144:1521:ORCL"
}
object LoadDataToOracle{
  var url="jdbc:oracle:thin:@10.11.27.144:1521:ORCL"
  def main(args: Array[String]): Unit = {
//    初始化spark
    val sparkSession = SparkSession.builder()
      .appName("LoadDataToOracle")
      .master("local[*]")
      .getOrCreate()
   val schema: StructType = StructType(
      Array(
        StructField("id", IntegerType, nullable = true),
        StructField("type", IntegerType, nullable = true),
        StructField("in_id", IntegerType, nullable = true),
        StructField("in_time", TimestampType, nullable = true),
        StructField("out_id", IntegerType, nullable = true),
        StructField("out_time", TimestampType, nullable = true),
      ))
    val filePath="G:/Destop/od-20210101-result.csv"
    val dataFrame = sparkSession.read.schema(schema).csv(filePath)
    val insertTable="TEST_OD"
    val prop = new Properties()
    prop.put("user","scott")
    prop.put("password","tiger")
    val ODFrame = sparkSession.read.jdbc(url,insertTable,prop)
    dataFrame.write.mode("append")jdbc(url,insertTable,prop)
    ODFrame.show()
    val list = Range(0,100000000)
    val javaList = new util.ArrayList[Int]()

  }
}
