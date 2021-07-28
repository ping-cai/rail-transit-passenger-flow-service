import java.util

import kspcalculation.{Edge, Path}

class TestEdgeAndPath {
  def testPath(): Unit = {
    val edge1 = new Edge("1", "2", 3.0)
    val edge2 = new Edge("1", "2", 3.0)
    val edge3 = new Edge("1", "3", 3.0)
    val edges1 = new util.LinkedList[Edge]()
    edges1.add(edge1)
    edges1.add(edge3)
    val edges2 = new util.LinkedList[Edge]()
    edges2.add(edge2)
    edges1.remove(edge3)
    val path1 = new Path(edges1, 3.0)
    val path2 = new Path(edges2, 3.0)
    println(edge1.equals(edge2))
    println(edges1.equals(edges2))
    println(path1.equals(path2))
    val hashSet = new util.HashSet[Path]()
    hashSet.add(path1)
    hashSet.add(path2)
    hashSet.forEach(x => println(x))
  }
}

object TestEdgeAndPath {
  def main(args: Array[String]): Unit = {
    val testEdgeAndPath = new TestEdgeAndPath
    testEdgeAndPath.testPath()
  }
}
