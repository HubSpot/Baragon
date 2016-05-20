## Baragon Basics

Baragon is made up of two services:

- BaragonService -- coordination service

- BaragonAgentService -- applies changes on the actual load balancer

When a web service changes (i.e. upstreams added / removed), POST a [BaragonRequest](../api.md#requests) JSON object to BaragonService's `/[contextPath]/request` endpoint like this one:

```json
{
  "loadBalancerRequestId": "4",
  "loadBalancerService": {
    "serviceId": "1",
    "owners": ["foo"],
    "serviceBasePath": "/basepath",
    "loadBalancerGroups": ["loadBalancerGroupName"]
  },
  "addUpstreams": ["1.1.1.1:80"],
  "removeUpstreams": []
}
```

- `BaragonService` will fan out the update to all `BaragonAgent`s in the specified `loadBalancerGroups`
- `BaragonAgent`s will apply the changes on the load balancer using templates provided in its configuration and report back a Success or Failure to `BaragonService`
- Polling the `BaragonService` request status url (`/[contextPath]/request/{loadBalancerRequestId}`) will indicate the current status of the request

Check out the [API Docs](../api.md) for additional `BaragonRequest` fields and returned values.