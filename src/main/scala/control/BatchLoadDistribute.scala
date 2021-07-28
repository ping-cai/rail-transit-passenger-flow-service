package control

import java.util

import distribution.DistributionResult
import flowdistribute.OdWithTime
import kspcalculation.Path

class BatchLoadDistribute extends BatchLoad {
  override def batchDistribute(odWithTime: OdWithTime, pathDynamicCost: util.Map[Path, Double], minPathCost: (Path, Double)): DistributionResult = {
    ???
  }
}

object BatchLoadDistribute {

}
