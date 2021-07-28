package collectionTest

case class ListTest(var list: List[Int]) {

}

object ListTest {
  def main(args: Array[String]): Unit = {
    val list = List(1, 2, 3, 4)
    val listTest = new ListTest(list)
    val list2 = listTest.list
    listTest.list = list :+ 2
    println(listTest.list)
  }
}