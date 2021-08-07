import java.sql.Timestamp

class DateUtilTest {
  def testTimestamp2String(): Unit = {
    val dateString = "2021-09-06 14:06:05"
    val timestamp = Timestamp.valueOf(dateString)
    val timeLong = timestamp.getTime
    val currentTime = timeLong / 60000.0 /15
    println(new Timestamp((currentTime.toLong * 60000 * 15)))
  }
}

object DateUtilTest {
  def main(args: Array[String]): Unit = {

  }
}
