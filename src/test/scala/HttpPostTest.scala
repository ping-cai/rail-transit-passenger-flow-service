import domain.ODInfo
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.codehaus.jackson.map.ObjectMapper

class HttpPostTest {

}

object HttpPostTest {
  def main(args: Array[String]): Unit = {
    val httpClient = HttpClients.createDefault()
    val post = new HttpPost("http://localhost:12123/modularSWJTU/tripPlan.do")
    post.addHeader("Content-Type", "application/json")
    val mapper = new ObjectMapper()
    val odInfo = new ODInfo("10", "10", "鱼洞", "马家岩")
    val jsonObject = mapper.writeValueAsString(odInfo)
    post.setEntity(new StringEntity(jsonObject, "UTF-8"))
    val response = httpClient.execute(post)
    val entity = response.getEntity
    val result = EntityUtils.toString(entity, "UTF-8")
    println(result)
  }
}