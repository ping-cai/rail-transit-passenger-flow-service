package check

import dataload.base.OracleSectionLoad
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.types._
import save.Save

class CheckSection {
  def checkSection(path: String, sparkSession: SparkSession): DataFrame = {
    val schema: StructType = StructType(
      Array(
        StructField("START_TIME", StringType, nullable = true),
        StructField("END_TIME", StringType, nullable = true),
        StructField("SECTION_ID", IntegerType, nullable = true),
        StructField("IN_ID", IntegerType, nullable = true),
        StructField("OUT_ID", IntegerType, nullable = true),
        StructField("PASSENGERS", DoubleType, nullable = true),
      )
    )
    val sectionFrame = sparkSession.read.schema(schema).csv(path)
    sectionFrame.createOrReplaceTempView("section_result")
    sectionFrame
  }

  def addLine(sectionResultFrame: DataFrame): Unit = {
    val sparkSession = sectionResultFrame.sparkSession
    val sectionLoad = new OracleSectionLoad()
    val sectionFrame = sectionLoad.load()
    sectionFrame.show()
    sectionFrame.createOrReplaceTempView("section")
    sectionResultFrame.createOrReplaceTempView("section_result")
    val addLineSql = "select START_TIME,END_TIME,SECTION_ID,IN_ID,CZ1_NAME,OUT_ID,CZ2_NAME,QJ_LJM LINE,PASSENGERS " +
      "from section_result join section on section_result.IN_ID=section.CZ1_ID " +
      "and section_result.OUT_ID=section.CZ2_ID where PASSENGERS >0.001 order by START_TIME,END_TIME,QJ_LJM,IN_ID,OUT_ID"
    val resultFrame = sparkSession.sql(addLineSql)
    resultFrame.show()
    resultFrame
      .write.jdbc(Save.url, "SECTION_ORDER_FLOW", Save.prop)
  }

}

object CheckSection {
  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession.builder().master("local[*]").getOrCreate()
    val sectionPath = "G:/Destop/sectionResult0513"
    val checkSection = new CheckSection()
    val sectionResultFrame = checkSection.checkSection(sectionPath, sparkSession)
    checkSection.addLine(sectionResultFrame)
  }
}
