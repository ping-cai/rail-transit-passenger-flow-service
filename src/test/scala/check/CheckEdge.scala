package check

import kspcalculation.Edge

import scala.collection.mutable

class CheckEdge {

}

object CheckEdge {
  def main(args: Array[String]): Unit = {
    val edgeSet = mutable.HashSet[Edge](new Edge("1", "2", 1.01)
      , new Edge("1", "2", 1.01))
    edgeSet foreach(edge=>println(edge))
  }
}
