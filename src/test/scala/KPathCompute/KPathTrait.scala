package KPathCompute

import model.{ODModel, PathModel}

trait KPathTrait extends Serializable {

  def compute(ODModel: ODModel): List[PathModel]
}
