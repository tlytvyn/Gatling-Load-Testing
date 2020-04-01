package healthhero

class Environment {

  val zendeskUrl = "https://lohikasupport.zendesk.com";
  val zendeskAPIVersionRequestURN = "/api/v2";

  val zendeskAPICreateRequestURN = "/requests.json";
  val zendeskAPIAddTagURN = "/tickets/{}/tags.json";

  val zendeskAPIDeleteManyRequests = "/tickets/destroy_many.json";
  val zendeskAPIDestroyManyRequests = "/deleted_tickets/destroy_many.json"
  val zendeskAPIJobStatusesRequests = "/job_statuses/{}.json"

  val loadUsers = 20;

}
