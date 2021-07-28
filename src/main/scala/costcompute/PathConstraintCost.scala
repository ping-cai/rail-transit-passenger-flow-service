package costcompute

import java.util

import conf.DynamicConf
import kspcalculation.Path

import scala.collection.JavaConverters._

class PathConstraintCost(sectionWithDirectionMap: util.Map[Section, String]) extends CostCompose {
  /**
    *
    * @param kspResult K短路结果
    * @return 特定的费用
    */
  override def compose(kspResult: util.List[Path]): util.Map[Path, Double] = {
    //    换乘次数偏离约束
    val transferTimesDeviation = DynamicConf.transferTimesDeviation
    //    出行费用放大系数约束
    val travelExpenseAmplification = DynamicConf.travelExpenseAmplification
    //    出行费用最大值约束
    val maximumTravelCost = DynamicConf.maximumTravelCost
    val pathWithTransferAndCosts = kspResult.asScala.map(path => {
      var tempTransferTime = 0
      path.getEdges.forEach(edge => {
        //        如果是换乘
        if (null == sectionWithDirectionMap.get(Section.createSectionByEdge(edge))) {
          tempTransferTime += 1
        }
      })
      //      路径总费用
      val totalCost = path.getTotalCost
      //      存储路径，换乘次数，总费用
      PathWithTransferAndCost(path, tempTransferTime, totalCost)
    })
    val minCost = pathWithTransferAndCosts.minBy(x => x.cost).cost
    val minTransferTime = pathWithTransferAndCosts.minBy(x => x.transferTime).transferTime
    val result = new util.HashMap[Path, Double]()
    pathWithTransferAndCosts foreach (x => {
      if (x.cost <= ((1 + travelExpenseAmplification) * minCost) &&
        x.cost <= (minCost + maximumTravelCost) &&
        x.transferTime <= (minTransferTime + transferTimesDeviation)) {
        result.put(x.path, x.cost)
      }
    })
    result
  }
}
