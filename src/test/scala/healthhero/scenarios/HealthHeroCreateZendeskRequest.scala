package healthhero.scenarios

import healthhero.Environment
import healthhero.utils.{HttpClientTestHelper, ReadConfProperties, StatusCodes}
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.collection.parallel.mutable.ParSet

class HealthHeroCreateZendeskRequest extends Simulation {

  val env = new Environment();
  val configs =  new ReadConfProperties().getConfig("src/test/resources/data/zendesk-api.properties");

  val parIdsSet = ParSet[Int]()

  val httpProtocol = http
    .baseUrl(env.zendeskUrl)
    .header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationJson)
    .basicAuth(configs("username") + "/token", configs("token"));

  val scn = scenario("Create Zendesk Request Scenario")
    .group("Create Zendesk Request") {
      exec(
        http("create zendesk request POST")
          .post(env.zendeskAPIVersionRequestURN + env.zendeskAPICreateRequestURN)
          //.body(StringBody("""${sendRequestBody}"""))
          .body(RawFileBody("src/test/resources/bodies/createRequest.json")).asJson
          .check(status is StatusCodes.created)
          .check(jsonPath("$.request.id").find.saveAs("requestId"))
      ).exitHereIfFailed
    }.group("Post conditions") {
    exec(
      session => {
        val requestId = session("requestId").as[Int]
        parIdsSet += requestId
        session
      }
    ).exitHereIfFailed
     .doIf(session => !"${requestId}".isEmpty) {
        exec(http("add zendesk request tag[load_testing] PUT")
          .put(env.zendeskAPIVersionRequestURN + env.zendeskAPIAddTagURN
            .replace("{}", "${requestId}"))
          //OuputHelper.outputVector("${requestIds}")))
          .body(RawFileBody("src/test/resources/bodies/addTagRequest.json")).asJson
          .check(status is StatusCodes.ok)
          .check(regex(".*\"tags\"[:].*\"load_testing\".*").exists)
        )
      }.exitHereIfFailed
  }

  setUp(
    scn.inject(atOnceUsers(env.loadUsers))
  ).protocols(httpProtocol)

  before {
    println("Simulation is about to start!")
  }

  after {
    println("Simulation is finished!");
    println("Ids created:");
    val createdRequestIds = parIdsSet.toList;
    println(createdRequestIds.mkString(","))
    HttpClientTestHelper.deleteRequests(createdRequestIds, env.zendeskAPIDeleteManyRequests, "DELETE many tickets");
    HttpClientTestHelper.deleteRequests(createdRequestIds, env.zendeskAPIDestroyManyRequests, "DESTROY many tickets");
  }

}