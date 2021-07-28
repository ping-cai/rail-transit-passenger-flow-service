package costcompute

import kspcalculation.Path

trait MinCost extends Serializable {
  def compose(): (Path, Double)
}
