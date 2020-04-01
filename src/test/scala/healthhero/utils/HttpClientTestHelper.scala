package healthhero.utils

import scala.concurrent.duration._
import akka.actor.ActorSystem

import scala.util.{Failure, Success}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.jayway.jsonpath.JsonPath
import healthhero.Environment

import scala.concurrent.{Await, Future}
import scala.collection.parallel.mutable.ParSet

object HttpClientTestHelper {

  val env = new Environment();
  val configs = new ReadConfProperties().getConfig("src/test/resources/data/zendesk-api.properties");
  val authorization = Authorization(BasicHttpCredentials(configs("username") + "/token", configs("token")))

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher
  val httpClient = Http();

  def deleteRequests(listIds: List[Int], deleteUri: String, requestName: String) {
    println("Ids list size: " + listIds.size)
    //Zendesk has limitation for 100 ids at once for DELETE requests
    val groupedIdsList: List[List[Int]] = listIds.grouped(99).toList;

    println("Divided grouped ids set size: " + groupedIdsList.size)

    for (idGroupInList <- groupedIdsList) {
      val ids: String = idGroupInList.mkString(",");
      println("Performing " + requestName + " request for ids: " + ids)
      val requestDelete = HttpRequest(
        DELETE,
        uri = env.zendeskUrl + env.zendeskAPIVersionRequestURN + deleteUri + "?ids=" + ids,
        headers = List(authorization)
      );

      val deleteResponseFuture: Future[HttpResponse] = httpClient.singleRequest(requestDelete);

      deleteResponseFuture
        .onComplete {
          case Success(resp: HttpResponse) => {
            println(requestName + " request was performed")
            val responseString: Future[String] = Unmarshal(resp).to[String]
            val jobId = responseString.map(body => JsonPath.read[String](body, "$.job_status.id"))
            val jobStatusById = jobId.flatMap(id => getJobStatus(id))

            jobStatusById.onComplete {
              case Success(statusText: String) => {


                val textBody = Await.result(responseString, 10.second)
                if (!textBody.contains("job_status")) {
                  throw new Exception(requestName + " failed with body: " + textBody)
                } else {
                  println("Successfully performed " + requestName + " for ids")
                }
                Await.result(httpClient.shutdownAllConnectionPools(), 5.seconds)
                Await.result(system.terminate(), 5.seconds)
              }
            }
          }
          case Failure(ex: Exception) => {
            println(requestName + " HTTP request completed with error: " + ex.getMessage)
          }
        }
    }
  }

  def getJobStatus(jobId: String): Future[String] = {
    val requestJobStatus = HttpRequest(
      GET,
      uri = env.zendeskUrl + env.zendeskAPIJobStatusesRequests.replaceAll("[{][}]", jobId),
      headers = List(authorization)
    );
    val getResponseFuture: Future[HttpResponse] = httpClient.singleRequest(requestJobStatus);
    getResponseFuture.map(body => JsonPath.read[String](body, "$.job_status.status"))
  }

  def main(args: Array[String]): Unit = {
    val listTesting = List(161,153,168,154,169,155,170,162,156,163,164,171,165,157,172,166,158,159,160,167);
    deleteRequests(listTesting, env.zendeskAPIDeleteManyRequests, "DELETE many tickets");
    deleteRequests(listTesting, env.zendeskAPIDestroyManyRequests, "DESTROY many tickets");
  }
}
