## Making Requests to BaragonService

A request can be made to Baragon by posting a `BaragonRequest` object to `BaragonService`'s `/request` endpoint. The Baragon request can have the following structure (specified in json below)

```json
{
  "loadBalancerRequestId":"123456", 
  "loadBalancerService":{
    "serviceId":"testService",
    "owners":["owner@example.com"],
    "serviceBasePath":"/basepath",
    "loadBalancerGroups":["test"],
    "options":{
      "nginxExtraConfigs": [
        "rewrite ^/test(.*) /test/v1/$1 last;"
      ],
    },
    "templateName":"name"
  },
  "addUpstreams":[
    {"upstream":"example.com:80","requestId":"requestId","rack":"us_east_1a"}
  ],
  "removeUpstreams":[],
  "replaceServiceId":"otherServiceId",
  "action":"UPDATE"
}
```

- `loadBalancerRequestId`: Alphanumeric, must be unique for each request, posting to an existing requestId will get you the response for that previous request
- `laodBalancerService`: Container for information about specific configuration for the service
  - `serviceId`: Must be unique within the Baragon cluster
  - `serviceBasePath`: Must be unique within the `loadBalancerGroup` (ie. cannot host two services at the same location), must be specified with a leading `/`
  - `loadBalancerGroups`: The request will be applied on agents in these groups. If a group is removed from the list (compared to a previously applied request), configuration files for the service will be removed via agents in that group and the `basePath` will become available again within that group
  - `options`: Any valid json object is permittable here. These values will be available under `service.options` in the handlebars templates. These fields can be used for custom configuration specific to an individual service (ie. not appropriate to include in the template for all services)
  - `templateName`: an optional field used to specify a custom template to render for this service. Specifying a `templateName` that does not exist in the `namedTemplates` on the agent will result in an error
- `addUpstreams`/`removeUpstreams`: A list of `UpstreamInfo` objects. Data from these objects will be available under `upstreams` in teh handlebars templates. Generally used to specify valid application hosts connected to the load balancer (ie. post a request to connect a healthy application host)
- `replaceServiceId`: An optional field used to rename a service or transfer a `basePath` to a different service. If specified, the configuration for the service in `replaceServiceId` will be swapped out for configuration belonging to the new serviceId and the basePath registered as belonging to the new serviceId(if the `replaceServiceId` exists). If it does not previously exist, the request will be applied as normal.
- `action`: An optional field. You can specify `UPDATE`, `DELETE`, or `RELOAD`. `UPDATE` is the default action if left blank and will update configs if they exist or create new ones if they don't (ie. a normal request). The `DELETE` action will remove the service from the state datastore as well as removing any associated config files form all associated load balancers. `RELOAD` will trigger the agents to check configs for validity and reload/restart 

<a id="requestflow"></a>
### Request Flow and Statuses

Baragon keeps track of an internal state and produces an externally visible status. Mentions of state below reference the internal state, while status references the status visible in the `BaragonResponse`

#### Posting the request
When a request is first posted to Baragon's `/request` endpoint, BaragonService will attempt to enqueue the request. This can have three outcomes:
- If the request is new (unique requestId), it is added to the pending request queue
- If the request is already present (requestId already used), but the request content matches the original request, the response to the original request will be returned (ie. the same request won't be enqueued again)
- If the request is already present, but the request content does not match, an error will be thrown

#### Initial processing of the request (`PENDING` internal state / `WAITING` status)
Once the request is in the pending request queue, it will be picked up by the request worker. The request will initially be in the `PENDING` state and the request worker will do the following:
- Check that there are no base path conflicts, moving the request to `INVALID_REQUEST_NOOP` if any are found
- Check that no non-existent load balancer groups have been requested, moving the request to `INVALID_REQUEST_NOOP` if any are found
- set the lock for the base paths in the request if it has not already been set to prevent other operations on the base path while the request is being processed
- If all checks have passed, move the request to the internal `SEND_APPLY_REQUESTS` status

#### Sending apply requests to agents (`SEND_APPLY_REQUESTS` internal status / `WAITING` status)
The request worker will send async requests to each agent in any load balancer groups that need to be updated during this step, then internally move the state to `CHECK_APPLY_RESPONSES` while waiting for requests to complete. The responses code of the agent post requests sent will be used to determine the status

#### Check agent responses (`CHECK_APPLY_RESPONSES` internal state, `WAITING` status)
The request worker will gather the responses from all agents it sent requests to and translate those to one of the following statuses:
- `WAITING`: Some requests are still being processed, none have failed yet, stay in the `CHECK_APPLY_RESPONSES` state
- `RETRY`: Some requests have failed, but the limit for number of retries was not yet reached. Send the requests again (`SEND_APPLY_REQUESTS`) and move back to `CHECK_APPLY_RESPONSES`
- `CANCELLED_SEND_REVERT_REQUESTS`: If the request was cancelled but some changes have been made, send new requests agents to revert the changes (status of `CANCELLING`)
  - `CANCELLED_CHECK_REVERT_RESPONSES`: similar to `CHECK_APPLY_RESPONSES`, a success produces the `CANCELLED` state/status and a failure will produce the `FAILED_CANCEL_FAILED` state(`FAILED` status)
- `FAILURE`: One or more of the requests has failed and the retry limit has been reached, move to `FAILED_SEND_REVERT_REQUESTS` state, this will cause new requests to be sent (similar to step 3), but the applied data will be from the most recent successful request (data stored in Baragon state node)
  - `FAILED_SEND_REVERT_REQUESTS`: Send requests to apply previous data and revert the load balancer changes
  - `FAILED_CHECK_REVERT_RESPONSES`: Similar to `CHECK_APPLY_RESPONSES`, but a failure will cause `FAILED_REVERT_FAILED` state and an overall status of `FAILED`, while a success will cause the `FAILED_REVERTED` state (still overall status of `FAILED` for the request)
- `INVALID_REQUEST_NOOP`: A failure has occurred but no action was taken on any load balancer. A revert is not needed, so undo any base path lock changes and return the `INVALID_REQUEST_NOOP` status

#### Commit the request if successful (`SUCCESS` state/status)
Once the request has been determined to be successful, the worker will commit the applied changes. This includes:
- Clearing invalid base path locks if:
  - base path has changed
  - service has moved off of that load balancer group
  - there are no remaining upstreams for the service
- If the serviceId has changed, remove the old service from the state node
- Add/update the new information about this service in the state node
- update the global state node to include this new data


<a id="exceptions"></a>
### Common Exceptions

- `InvalidConfigException`: `BaragonAgent` will throw this when it has encountered an error during the configuration check (config is checked for validity before reloading). If a message was produced by the config check command, it will be included in the exception message
- `LbAdapterExecuteException`: This is a more general exception that can occur on the agent when an error occurs while executing one of the check or reload config commands. If a message was produced by the command, it will be included in the exception message.
- `MissingTemplateException`: This exception will occur on the `BaragonAgent` when a `templateName` is requested that does not match any `namedTemplates` known to that agent instance. No action will be taken on the load balancer for that request and the name of the unknown template will be included in the request message.
- `RequestAlreadyEnqueuedException`: This exception can occur on `BaragonService` when a request with an already existing `loadBalancerRequestId` but different data (service, options, etc) is posted to the `/request` endpoint.
