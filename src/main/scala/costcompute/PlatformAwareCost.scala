package costcompute

import domain.StationFlow

abstract class PlatformAwareCost extends CostCompose {
  private var platformFlow: java.util.List[StationFlow] = _

  /**
    *
    * @param kspResult K短路结果
    * @return 特定的费用
    */

}
