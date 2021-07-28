package collectionTest

class MapTest {

}

object MapTest {
  def main(args: Array[String]): Unit = {
    println(testMap())
  }

  def testMap(): Map[String, Int] = {
    var map = Map("1" -> 1)
    map += ("2" -> 2)
    println(map)
    println(map)
    map
  }
}
