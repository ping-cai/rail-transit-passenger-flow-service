import java.text.SimpleDateFormat
import java.util.Date

class TestCheckPoint {

}

object TestCheckPoint {
  def main(args: Array[String]): Unit = {
    println(new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()))
  }
}