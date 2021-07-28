package costcompute

import java.util

import conf.DynamicConf
import kspcalculation.Path

class StaticCost(sectionWithDirectionMap: util.Map[Section, String]) extends Cost {
  def costCompute(legalPath: java.util.List[Path], sectionTravelGraph: SectionTravelGraph): java.util.Map[Path, Double] = {
    var sectionMovePathCost: util.Map[Path, Double] = new util.HashMap[Path, Double]()
    if (DynamicConf.openPathConstraint) {
      val pathConstraintCost = new PathConstraintCost(sectionWithDirectionMap)
      sectionMovePathCost = pathConstraintCost.compose(legalPath)
    } else {
      //    区间运行费用
      val sectionMoveCost = new SectionMoveCost(sectionTravelGraph)
      sectionMovePathCost = sectionMoveCost.compose(legalPath)
    }
    //    列车停站费用
    val stopTimeCost = new StopTimeCost()
    val stopTimePathCost = stopTimeCost.compose(legalPath)
    /*
      * 三者聚合，应当创建一个新的集合对象进行存储
      * 否则将改变原有三者集合中的数据，破坏其原有的语义
      */
    val allPathCost = new util.HashMap[Path, Double]()
    //    随机做某个集合的循环即可，因为元素都是一致的
    sectionMovePathCost.forEach((path, cost) => {
      val allCost = cost + stopTimePathCost.get(path)
      allPathCost.put(path, allCost)
    })
    sectionMovePathCost
  }
}
