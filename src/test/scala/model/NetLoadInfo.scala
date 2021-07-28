package model

import model.dto.{SectionLoadDTO, StationLoadDTO}

case class NetLoadInfo(recordTime: String,
                       var stationLoads: List[StationLoadDTO], var sectionLoads: List[SectionLoad]) {

}
