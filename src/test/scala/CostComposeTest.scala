import java.util

import costcompute.{MinGeneralizedCost, SectionMoveCost, SectionTravelGraph, StopTimeCost}
import dataload.BaseDataLoad
import kspcalculation.{Graph, Path, PathComputeService}
import org.apache.spark.sql.SparkSession

class CostComposeTest {

}

object CostComposeTest {
  def testSectionMoveCost(): Unit = {
    val sparkSession = SparkSession.builder().master("local[*]").getOrCreate()
    val baseDataLoad = new BaseDataLoad()
    val graph: Graph = baseDataLoad.getGraph
    val paths = new PathComputeService(10).fixedKPathCompute(graph, "1", "26")
    val sectionTravelGraph = baseDataLoad.getSectionTravelGraph
    val sectionMoveCost = new SectionMoveCost(sectionTravelGraph)
    val pathWithSectionMoveCost: util.Map[Path, Double] = sectionMoveCost.compose(paths)
    val stopTimeCost = new StopTimeCost
    val pathWithStopTime = stopTimeCost.compose(paths)
    pathWithStopTime.forEach((x, y) => { 
      println(s"路径为$x 总费用为 $y")
    })
    //    对照试验，看是否总费用有所增加
    pathWithStopTime.forEach((x, y) => {
      val stopTimeAndMoveCost = pathWithSectionMoveCost.get(x) + y
      pathWithStopTime.put(x, stopTimeAndMoveCost)
    })
    pathWithStopTime.forEach((x, y) => {
      println(s"路径为$x 总费用为 $y")
    })

    val minGeneralizedCost = new MinGeneralizedCost(pathWithStopTime)
    println(minGeneralizedCost.compose())
    //    结果如下，事实证明路径的总费用已经增加，测试成功
    /*
    路径为94.30499969422817: [100+99+98+97+96+95+94+93+92+91+90+108+89+88+87+86+85+84+83+107+1+26+5+6+2+7+3+8+199+200] 总费用为 21.75
    路径为99.07499979436398: [100+99+98+97+96+95+94+93+92+81+75+66+65+199+200] 总费用为 10.5
    路径为78.52499954402447: [100+99+98+97+96+95+94+93+92+81+58+48+203+202+201+200] 总费用为 11.25
    路径为109.84499998390675: [100+99+98+97+96+95+94+93+92+91+90+108+89+88+87+157+152+5+6+2+7+3+8+199+200] 总费用为 18.0
    路径为98.41499988734722: [100+99+98+97+96+95+94+93+92+91+90+108+2+7+3+8+65+199+200] 总费用为 13.5
    路径为102.52499954402447: [100+99+98+97+96+95+94+93+92+81+58+48+60+203+202+201+200] 总费用为 12.0
    路径为74.41499988734722: [100+99+98+97+96+95+94+93+92+91+90+108+2+7+3+8+199+200] 总费用为 12.75
    路径为86.80499969422817: [100+99+98+97+96+95+94+93+92+91+287+288+289+290+291+292+261+203+202+201+200] 总费用为 15.0
    路径为102.52499954402447: [100+99+98+97+96+95+94+93+92+81+58+48+261+203+202+201+200] 总费用为 12.0
    路径为110.80499969422817: [100+99+98+97+96+95+94+93+92+91+287+288+289+290+291+292+261+60+203+202+201+200] 总费用为 15.75
//    这里是分界点
    路径为94.30499969422817: [100+99+98+97+96+95+94+93+92+91+90+108+89+88+87+86+85+84+83+107+1+26+5+6+2+7+3+8+199+200] 总费用为 65.41666666666666
    路径为99.07499979436398: [100+99+98+97+96+95+94+93+92+81+75+66+65+199+200] 总费用为 33.06666666666666
    路径为78.52499954402447: [100+99+98+97+96+95+94+93+92+81+58+48+203+202+201+200] 总费用为 36.166666666666664
    路径为109.84499998390675: [100+99+98+97+96+95+94+93+92+91+90+108+89+88+87+157+152+5+6+2+7+3+8+199+200] 总费用为 56.166666666666664
    路径为98.41499988734722: [100+99+98+97+96+95+94+93+92+91+90+108+2+7+3+8+65+199+200] 总费用为 41.16666666666667
    路径为102.52499954402447: [100+99+98+97+96+95+94+93+92+81+58+48+60+203+202+201+200] 总费用为 36.916666666666664
    路径为74.41499988734722: [100+99+98+97+96+95+94+93+92+91+90+108+2+7+3+8+199+200] 总费用为 40.41666666666667
    路径为86.80499969422817: [100+99+98+97+96+95+94+93+92+91+287+288+289+290+291+292+261+203+202+201+200] 总费用为 47.199999999999996
    路径为102.52499954402447: [100+99+98+97+96+95+94+93+92+81+58+48+261+203+202+201+200] 总费用为 36.916666666666664
    路径为110.80499969422817: [100+99+98+97+96+95+94+93+92+91+287+288+289+290+291+292+261+60+203+202+201+200] 总费用为 47.949999999999996
     */
  }

  def main(args: Array[String]): Unit = {
    testSectionMoveCost()
  }
}
