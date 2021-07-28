package model

case class ODWithTimeModel(override val inId: String, override val outId: String, inTime: String, outTime: String) extends ODModel(inId: String, outId: String) {

}
