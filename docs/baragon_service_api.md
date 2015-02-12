BaragonService API
==================

<a id="top"></a>
Listed below are the various endpoints for BaragonService along with example request payloads (where applicable) and responses. All example requests are pointed at the baragon vagrants, and all request examples are written in python.

| [State](#state) | [Workers](#workers) | [Status](#status) | [Requests](#requests) | [Auth](#auth)  | [Load Balancer](#load-balancer) |
<a id="state"></a>
#State
##GET `/state`

Returns the current list of services that Baragon is keeping track of.

###Example Request
```python
requests.get("192.168.33.20:8080/baragon/v2/state")
```

###Example Response
```json
[
   {
      "service":{
         "serviceId":"test1",
         "owners":["someone@example.com"],
         "serviceBasePath":"/test1",
         "loadBalancerGroups":["vagrant"],
         "options":{
             "nginxExtraConfigs":["rewrite ^/test(.*) /test/path$1 last;"]
         }
      },
      "upstreams":[{
            "upstream":"example.com:80",
            "requestId":"test1",
            "rackId":"us_east_1a"
       }]
   },
      {
      "service":{
         "serviceId":"test2",
         "owners":["someone@example.com"],
         "serviceBasePath":"/test2",
         "loadBalancerGroups":["vagrant"],
         "options":{
            "nginxExtraConfigs":["rewrite ^/test2(.*) /test2/path$1 last;"]
         }
      },
      "upstreams":[{
            "upstream":"something.com:80",
            "requestId":"test2",
            "rackId":"us_east_1a"
       }]
   },
]
```

##GET `/state/{serviceId}`

Returns the details for a specific service ID.

###Example Request
```python
requests.get("192.168.33.20:8080/baragon/v2/state/test1")
```

###Example Response
```json
{
   "service":{
      "serviceId":"test1",
      "owners":["someone@example.com"],
      "serviceBasePath":"/test1",
      "loadBalancerGroups":["vagrant"],
      "options":{
          "nginxExtraConfigs":["rewrite ^/test(.*) /test/path$1 last;"]
      }
   },
   "upstreams":[{
       "upstream":"example.com:80",
       "requestId":"test1",
       "rackId":"us_east_1a"
    }]
}
```

##DELETE `/state/{serviceId}`

Removes the service from the current state and clears any associated base paths.

###Example Request
```python
requests.delete("192.168.33.20:8080/baragon/v2/state/test1")
```

###Example Response
```json
{
   "service":{
      "serviceId":"test1",
      "owners":["someone@example.com"],
      "serviceBasePath":"/test1",
      "loadBalancerGroups":["vagrant"],
      "options":{
          "nginxExtraConfigs":["rewrite ^/test(.*) /test/path$1 last;"]
      }
   },
   "upstreams":[{
       "upstream":"example.com:80",
       "requestId":"test1",
       "rackId":"us_east_1a"
    }]
}
```

<a id="workers"></a>
#Workers
| [Top](#top) | [State](#state) | [Workers](#workers) | [Status](#status) | [Requests](#requests) | [Auth](#auth)  | [Load Balancer](#load-balancer) |
##GET `/workers`

Returns the addresses of the currently active baragon workers

###Example Request
```python
requests.get("192.168.33.20:8080/baragon/v2/workers")
```

###Example Response
```json
["http://192.168.33.20:8080/baragon/v2"]
```

<a id="status"></a>
#Status
| [Top](#top) | [State](#state) | [Workers](#workers) | [Status](#status) | [Requests](#requests) | [Auth](#auth)  | [Load Balancer](#load-balancer) |
##GET `/status`

Returns the current BaragonService status.

###Example Request
```python
requests.get("192.168.33.20:8080/baragon/v2/status")
```

###Example Response
```
{
  "leader": true,                 # Is this BaragonService instance currently the leader?
  "pendingRequestCount": 0,       # Requests in the queue
  "workerLagMs": 1423,            # Time since worker last start
  "zookeeperState": "CONNECTED",  # Zookeeper state
  "globalStateNodeSize": 1304     # Size of the /state zk node in bytes (limit 1MB in zk)
}
```

<a id="requests"></a>
#Requests
| [Top](#top) | [State](#state) | [Workers](#workers) | [Status](#status) | [Requests](#requests) | [Auth](#auth)  | [Load Balancer](#load-balancer) |
##GET `/request`

Returns the current pending requests

###Example Request
```python
requests.get("192.168.33.20:8080/baragon/v2/request")
```

###Example Response
```
[
   {
      "serviceId":"test1",       # Service associated with the request
      "requestId":"requestId1",  # Unique reqeust identifier
      "index":5                  # (ie,. fifth request Baragon has seen)
   }
]
```

##POST `/request`

Add a request to the queue with the given data.

###Example Request
```python
data = {
  "loadBalancerRequestId":"requestId", # Unique request id
  "loadBalancerService":{
    "serviceId":"testService",
    "owners":["owner@example.com"],
    "serviceBasePath":"/basepath",
    "loadBalancerGroups":["vagrant"],
    "options":{
		"nginxExtraConfigs": [
			"rewrite ^/test(.*) /test/v1/$1 last;"
		]
    }
  },
  "addUpstreams":[
    {"upstream":"example.com:80","requestId":"requestId","rack":"us_east_1a"}
  ],
  "removeUpstreams":[]
}
headers = {'Content-type': 'application/json'}
requests.post("http://192.168.33.20:8080/baragon/v2/request", data=data, headers=headers)
```

###Example Response
- PENDING
```json
{
   "loadBalancerRequestId": "requestId",
   "loadBalancerState": "PENDING",
   "message": "Queued as QueuedRequestId{serviceId=testService, requestId=requestId, index=2164}",
   "agentResponses": {
      "APPLY": [] 
   }
}
```
- SUCCESS
```json
{
   "loadBalancerRequestId": "requestId",
   "loadBalancerState": "SUCCESS",
   "message": "Queued as QueuedRequestId{serviceId=testService, requestId=requestId, index=2164}",
   "agentResponses": {
      "APPLY": [
         {
            "url": "http://192.168.33.22:8081/baragon-agent/v2/request/requestId",
            "attempt": 0,
            "statusCode": 200,
            "content": null,
            "exception": null
         }
      ] 
   }
}
```

##GET `/request/{requestId}`

Returns the status of a particular request.

###Example Request
```python
requests.get("192.168.33.20:8080/baragon/v2/request/requestId")
```

###Example Response
```json
{
   "loadBalancerRequestId": "requestId",
   "loadBalancerState": "SUCCESS",
   "message": "Queued as QueuedRequestId{serviceId=testService, requestId=requestId, index=2164}",
   "agentResponses": {
      "APPLY": [
         {
            "url": "http://192.168.33.22:8081/baragon-agent/v2/request/MDSrequestId",
            "attempt": 0,
            "statusCode": 200,
            "content": null,
            "exception": null
         }
      ] 
   }
}
```

<a id="auth"></a>
#Auth
| [Top](#top) | [State](#state) | [Workers](#workers) | [Status](#status) | [Requests](#requests) | [Auth](#auth)  | [Load Balancer](#load-balancer) |
##GET `/auth/keys`

Returns the current list of auth keys (Must use the master auth key to access this)

###Example Request
```python
params = {"authkey":"my-master-key"}
requests.get("192.168.33.20:8080/baragon/v2/auth/keys", params=params)
```

###Example Response
```json
[
   {
      "value": "my-regular-key",
      "owner": "owner@example.com",
      "createdAt": 1409435929246,
      "expiredAt": null
   }
]
```

##DELETE `/auth/keys/{key}`

Delete {key} from the list of valid auth keys (Must use the master auth key to access this)

###Example Request
```python
params = {"authkey":"my-master-key"}
requests.delete("192.168.33.20:8080/baragon/v2/auth/keys/expiringauthkey", params=params)
```

###Example Response
```json
{
   "value": "expiringauthkey",
   "owner": "owner@example.com",
   "createdAt": 1409435929246,
   "expiredAt": null
}
```

##POST `/auth/keys/{key}`

Add a new valid auth key (Must use the master auth key to access this)

###Example Request
```python
params = {"authkey":"my-master-key"}
headers = {'Content-type': 'application/json'}
data = {
   "value": "newauthkey",
   "owner": "owner@example.com"
}

requests.post("192.168.33.20:8080/baragon/v2/auth/keys", data=data, params=params, headers=headers)
```

###Example Response
```json
{
   "value": "expiringauthkey",
   "owner": "owner@example.com",
   "createdAt": 1409435929246,
   "expiredAt": null
}
```

<a id="load-balancer"></a>
#Load Balancer
| [Top](#top) | [State](#state) | [Workers](#workers) | [Status](#status) | [Requests](#requests) | [Auth](#auth)  | [Load Balancer](#load-balancer) |
##GET `/load-balancer`

Returns a list of the current load balancer clusters/groups

###Example Request
```python
requests.get("192.168.33.20:8080/baragon/v2/load-balancer")
```

###Example Response
```json
[
   "vagrant",
   "vagrant2"
]
```

##GET `/load-balancer/{cluster}/agents`

Returns the currently active baragon agents for the specified load balancer cluster

###Example Request
```python
requests.get("192.168.33.20:8080/baragon/v2/load-balancer/vagrant/agents")
```

###Example Response
```json
[
   {
      "baseAgentUri": "http://192.168.33.21:8081/baragon/v2",
      "agentId": "192.168.33.21:8081",
      "domain": "vagrant.baragon.biz"
   }
]
```

##GET `/load-balancer/{cluster}/known-agents`

Returns all Baragon agents that Baragon service has seen for the specified load balancer cluster

###Example Request
```python
requests.get("192.168.33.20:8080/baragon/v2/load-balancer/vagrant/known-agents")
```

###Example Response
```json
[
   {
      "baseAgentUri": "http://192.168.33.21:8081/baragon/v2",
      "agentId": "192.168.33.21:8081",
      "domain": "vagrant.baragon.biz",
      "lastSeenAt": 1423693860906
   }
]
```

##DELETE `/load-balancer/{cluster}/known-agents`

Remove the specified agent from the list of known agents. This will NOT remove if from the active agents if it is still connected in zookeeper

###Example Request
```python
agentId = "192.168.33.21:8081"
requests.delete("192.168.33.20:8080/baragon/v2/load-balancer/vagrant/known-agents/{0}".format(agentId))
```

##GET `/load-balancer/{cluster}/base-path/all`

Get the list of base paths for the specified cluster

###Example Request
```python
requests.get("192.168.33.20:8080/baragon/v2/load-balancer/vagrant/base-path/all")
```

###Example Response
```json
[
   "/test1",
   "/test2"
]
```

##DELETE `/load-balancer/{cluster}/base-path`

Remove the lock on a base path for the specified cluster

###Example Request
```python
params = {"basePath":"/test"}
requests.delete("192.168.33.20:8080/baragon/v2/load-balancer/vagrant/base-path", params=params)
```

##GET `/load-balancer/{cluster}/base-path`

Return the service associated with the specified base path

###Example Request
```python
params = {"basePath":"/test"}
requests.get("192.168.33.20:8080/baragon/v2/load-balancer/vagrant/base-path", params=params)
```

###Example Response
```json
{
   "service":{
      "serviceId":"test",
      "owners":["someone@example.com"],
      "serviceBasePath":"/test",
      "loadBalancerGroups":["vagrant"],
      "options":{
          "nginxExtraConfigs":["rewrite ^/test(.*) /test/path$1 last;"]
      }
   },
   "upstreams":[{
       "upstream":"example.com:80",
       "requestId":"testrequest",
       "rackId":"us_east_1a"
    }]
}
```

| [Top](#top) | [State](#state) | [Workers](#workers) | [Status](#status) | [Requests](#requests) | [Auth](#auth)  | [Load Balancer](#load-balancer) |
