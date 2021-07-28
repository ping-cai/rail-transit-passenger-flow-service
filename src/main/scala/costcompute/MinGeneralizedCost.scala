package costcompute

import java.util

import kspcalculation.Path

import scala.collection.JavaConverters._

/**
  *
  */
class MinGeneralizedCost(var sectionMoveCost: util.Map[Path, Double]) extends MinCost {
  /**
    *
    * @return 特定的最小费用
    */
  def compose(): (Path, Double) = {
    sectionMoveCost.asScala.toList.min((x: (Path, Double), y: (Path, Double)) => {
      (x._2 - y._2).toInt
    })
  }
}
