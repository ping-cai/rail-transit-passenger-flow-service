import java.util.Properties

import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, SparkSession}
import service.ODDivision

object DivideTime {
  var url = "jdbc:oracle:thin:@10.11.27.144:1521:ORCL"

  def main(args: Array[String]): Unit = {
    transferTest()
  }

  def getOd(): DataFrame = {
    val sparkSession = SparkSession.builder()
      .master("local[*]")
      .getOrCreate()
    val schema: StructType = StructType(
      Array(
        StructField("id", StringType, nullable = true),
        StructField("kind", IntegerType, nullable = true),
        StructField("in_number", IntegerType, nullable = true),
        StructField("in_time", TimestampType, nullable = true),
        StructField("out_number", IntegerType, nullable = true),
        StructField("out_time", TimestampType, nullable = true)
      )
    )
    val filePath = "G:/Destop/od-20210101-result.csv"
    val dataFrame = sparkSession.read.schema(schema).csv(filePath)
    dataFrame
  }

  def divideTimeExample(): Unit = {
    import org.apache.spark.sql.functions.round
    val sparkSession = SparkSession.builder()
      .master("local[*]")
      .getOrCreate()
    import sparkSession.implicits._
    val df = Seq(
      ("001", "event1", 10, "2016-05-01 10:50:51"),
      ("002", "event2", 100, "2016-05-01 10:50:53"),
      ("001", "event3", 20, "2016-05-01 10:50:55"),
      ("001", "event1", 15, "2016-05-01 10:51:50"),
      ("003", "event1", 13, "2016-05-01 10:55:30"),
      ("001", "event2", 12, "2016-05-01 10:57:00"),
      ("001", "event3", 11, "2016-05-01 11:00:01")
    ).toDF("KEY", "Event_Type", "metric", "Time")
    // cast string to timestamp
    val ts = $"Time".cast("timestamp").cast("long")

    // Round to 300 seconds interval
    val interval = (round(ts / 300L) * 300.0).cast("timestamp").alias("interval")
    df.groupBy($"KEY", interval).sum("metric").show()
  }

  def divide(data: DataFrame): DataFrame = {
    val interval = 15
    data.createOrReplaceTempView("od")
    val divideTime = interval * 60
    val sparkSession = data.sparkSession
    val divideSql = s"select in_number,cast(round(cast(in_time as bigint)/$divideTime)*$divideTime as timestamp) in_time," +
      s"out_number,cast(round(cast(out_time as bigint)/$divideTime)*$divideTime as timestamp) out_time " +
      s"from od"
    val divideTimeFrame = sparkSession.sql(divideSql)
    divideTimeFrame.createOrReplaceTempView("od")
    val groupPassenger = "select *,count(*) passengers from od group by in_number,in_time,out_number,out_time"
    val frameWithPassenger = sparkSession.sql(groupPassenger)
    frameWithPassenger
  }

  def getLoad(): (DataFrame, DataFrame, DataFrame) = {
    val sparkSession = SparkSession.builder()
      .master("local[*]")
      .getOrCreate()
    val properties = new Properties()
    properties.put("user", "scott")
    properties.put("password", "tiger")
    val chongQingTable = "\"chongqing_stations_nm\""
    val chongQingFrame = sparkSession.read.jdbc(url, chongQingTable, properties)
    val stationTable = "\"2-yxtStation\""
    val stationFrame = sparkSession.read.jdbc(url, stationTable, properties)
    val odFrame = divide(getOd())
    (chongQingFrame, stationFrame, odFrame)
  }

  def transferTest(): Unit = {
    val loadTuple = getLoad()
    val chongQingData = loadTuple._1
    val stationData = loadTuple._2
    val od = loadTuple._3
    val sparkSession = od.sparkSession
    stationData.createOrReplaceTempView("station")
    chongQingData.createOrReplaceTempView("chongqing_stations_nm")
    od.createOrReplaceTempView("od")
    val equalTransfer = "select origin.CZ_ID,target.STATIONID FROM station origin join " +
      "chongqing_stations_nm target on origin.CZ_NAME=target.CZNAME " +
      "and origin.LJM=target.LINENAME"
    val equalIdTable = sparkSession.sql(equalTransfer)
    equalIdTable.createOrReplaceTempView("transfer_id")
    val transferEndOdSql = "select target1.CZ_ID,in_time,target2.CZ_ID,out_time,passengers " +
      "from od origin " +
      "join transfer_id target1 on origin.in_number=target1.CZ_ID " +
      "join transfer_id target2 on origin.in_number=target2.CZ_ID "
    val transferEndOd = sparkSession.sql(transferEndOdSql)
    val startTime = "2021-01-01 17:15:00"
    val endTime="2021-01-01 15:15:00"
    val specifiedTimeData = ODDivision.getSpecifiedTimeData(startTime,transferEndOd)
    specifiedTimeData.show()
  }
}
