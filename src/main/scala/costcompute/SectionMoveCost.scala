package costcompute

import java.util

import domain.param.CostParam
import kspcalculation.Path

class SectionMoveCost(sectionMoveGraph: SectionTravelGraph) extends CostCompose {
  private var TRANSFER_PENALTY = CostParam.TRANSFER_PENALTY

  /**
    *
    * @param kspResult K短路结果
    * @return 特定的费用
    *         待解决的问题：列车运行时间也应该是中位数
    */
  override def compose(kspResult: util.List[Path]): java.util.Map[Path, Double] = {
    val sectionMoveCost = new java.util.HashMap[Path, Double]
    kspResult.forEach(
      //      x => {
      //      val edges = x.getEdges
      //      var pathMoveCost = 0.0
      //      edges.forEach(edge => {
      //        //        得到区间运行时间
      //        val travelSeconds = sectionMoveGraph.getTravelTime(Section.createSectionByEdge(edge))
      //        if (travelSeconds != null) {
      //          pathMoveCost += travelSeconds / 60.0
      //        } else {
      //          //          得不到区间运行时间就是换乘
      //          pathMoveCost += TRANSFER_PENALTY
      //        }
      //      })
      //      if (pathMoveCost != 0) {
      //        //        运行时间一定不会为零，如果为零，那么应该是上述的获取时间出现问题
      //        sectionMoveCost.put(x, pathMoveCost)
      //      } else {
      //        sectionMoveCost.put(x, x.getTotalCost)
      //      }
      x => {
        sectionMoveCost.put(x, x.getTotalCost)
      })
    sectionMoveCost
  }
}
