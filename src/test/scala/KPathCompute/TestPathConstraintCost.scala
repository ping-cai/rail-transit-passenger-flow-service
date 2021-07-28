package KPathCompute

import dataload.BaseDataLoad
import model.ODWithTimeModel

class TestPathConstraintCost {
  def test(): Unit ={
    val baseDataLoad = new BaseDataLoad
    val sectionWithDirectionMap = baseDataLoad.getSectionWithDirectionMap
    val pathService = new KPathService(baseDataLoad,6)
//    pathService.compute(new ODWithTimeModel())
  }
}
