package calculate

import java.util

import distribution.LogitDistribute
import kspcalculation.Path

object Logit {
  def logit(pathDynamicCost: java.util.Map[Path, Double],
            minPathCost: (Path, Double), passengers: Double): java.util.Map[Path, Double] = {
    val exp = Math.E
    var pathTotalCost = 0.0
    val minCost = minPathCost._2
    val pathWithLogitFlow = new util.HashMap[Path, Double]()
    pathDynamicCost.forEach((path, cost) => {
      pathTotalCost += Math.pow(exp, (-LogitDistribute.theta) * (cost / minCost))
    })
    //    防止除0异常
    if (pathTotalCost == 0.0) {
      throw new RuntimeException(s"Divide by zero exception!check the pathTotalCost why it is zero")
    }
    pathDynamicCost.forEach((path, cost) => {
      val distributionPower = Math.pow(exp, (-LogitDistribute.theta) * (cost / minCost)) / pathTotalCost
      pathWithLogitFlow.put(path, distributionPower * passengers)
    })
    pathWithLogitFlow
  }
}
