package stream

import java.sql.Date

class TestOdPairStream {

}
object TestOdPairStream{
  def testDate(): Unit ={

    val date = new Date(1999999999)
    val localDate = date.toLocalDate
    println(localDate.getYear)
    println(localDate.getMonthValue)
    println(localDate.getDayOfMonth/7)
    println(date)
  }

  def main(args: Array[String]): Unit = {
    testDate()
  }
}
