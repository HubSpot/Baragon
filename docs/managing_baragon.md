Baragon Detailed Management Guide
=================================

## Contents
- [Terminology](#terminology)
- [Launching a Cluster](#launch)
  - [Baragon Service Setup and Configuration](#servicesetup)
  - [Baragon Agent Setup and Configuration](#agentsetup)
    - [Setup With Nginx](#nginx)
    - [Setup With Haproxy](#haproxy)
  - [Integration with Singularity](#singularity)
  - [Data Storage in Zookeeper](#zookeeper)
- [Making Requests to Baragon Service](#requests)
  - [Statuses and Request Flow](#requestflow)
  - [Common Exceptions](#exceptions)
- [BasePath Locking and Updating](#basepaths)
- [ELB Sync](#elb)

<a id="terminology"></a>
## Terminology
- `BaragonService` - The api and accompanying worker that is central to all agents. BaragonService manages all requests, provides information about running services, and also serves up the `BaragonUI`
- `BaragonAgent` or `BaragonAgentService` - The process running on each load balancer host. The agent is responsible for actually applying requests as resulting configuration files using templates
- `BaragonUI` - A backbone.js user interface with the same capabilities as most `BaragonService` endpoints
- `cluster` - All `BaragonService` and `BaragonAgent` instances in the same zookeeper quorum and namespace
- `loadBalancerService` - An ID unique across a cluster which is the primary key for storing all information (paths, upstreams, etc) once a request has been successfuly applied. Associated with the `BaragonService`/`BaragonServiceState` objects
- `loadBalancerRequest` - A `BaragonRequest` object that can be posted to `BaragonService` to either update or create a `loadBalancerService`
- `loadBalancerGroup` - A group of agents that should all have identical configuration, grouped by a name. For example, several nginx load balancers behind the same ELB, or serving content for the same domain
- `basePath` - The subpath on a loadbalancer at which the applied service should be available, must be unique within a `loadBalancerGroup`

<a id="launch"></a>
## Launching A Cluster

Three key components are needed for a working Baragon cluster:
- A working zookeeper cluster
- A `BaragonService` instance
- A `BaragonAgentService` instance running on the same host as you load balancer program (nginx/haproxy/etc)

#### Building JARs
To get started, build the needed `BaragonService` and `BaragonAgentService` JARs by running 
```bash
mvn clean package
```
in the base of this project. The JARs are also publicly available in the maven repository.

**Hint: You can customize the name of your JARs using the `-Dbaragon.jar.name.format=(your format)` option. The default JAR name format is `${project.artifactId}-${project.version}`

#### Setup with Chef
Alternatively you can provision your Baragon instances via chef using the cookbook provided in this repository. The `baragon` cookbook is available in the public chef supermarket. Check out the [cookbook readme](../cookbook/README.md) for more details on provisioning with chef.

<a id="servicesetup"></a>
### Baragon Service Setup and Configuration

1. Build JARs via `mvn clean package`.

2. Create a [configuration file for Baragon Service](baragon_service_config.yaml) (`$SERVICE_CONFIG_YAML`)

3. Copy `BaragonService-*-SNAPSHOT.jar` and `$SERVICE_CONFIG_YAML` onto one or more hosts, and start the service via `java -jar BaragonService-*-SNAPSHOT.jar server $SERVICE_CONFIG_YAML`.
  - Note that you can specify arguments to the java vm in this command that will override values from the configuration file. These can be specified in the format `-Ddw.(path_to_config_name)=(value)`
    - ex. setting the port for Baragon Service to use via an environment variable `-Ddw.server.connector.port=$PORT`
    - ex. set an auth key `-Ddw.auth.key=my-auth-key`

####Configuration Specifics
An `auth` section and `masterAuthKey` must be provided in the configuration for Baragon Service to either disable auth for that instance, or enable auth and specify a key. 
- If auth is enabled, then all requests to Baragon Service (Except to the `/ui` path) will need to container an `authkey` parameter with the valid key.
- Requests to the `/auth` and related endpoints must use the `masterAuthKey` as the `authkey` param in the request

The `worker` configuration section of the configuration does not need to be provided, but can be used ot tune the request worker or shut it off, creating a read-only instance. By default the request worker will be running. 
***Note: if no request worker is running, the instance will be able to enqueue but not process requests

The `ui` section of the configuration is also optional. By default the ui will be available at `/[contextPath]/ui` and will prompt you for an auth key in case one is needed to communicate with Baragon Service's API. By default the ui, will be in a read-only mode.
- If you need to host the ui on a different basePath, you can provide the `ui` => `basePath` confugration option to change where the ui will be available
  - If hosting the ui on a different domain than the Baragon API, a CORS filter is available in the configuration
- To enable edit mode for the ui, you must do one of the following:
  - Set `allowEdit` in the ui configuration to true - This will display the ui in edit mode by default
  - Specify an `auth` => `key` with `uiWriteEabled` set to true - If a user enters this key when accessing the UI, it will display in edit mode
  - Specify an `allowEditKey` in the ui configuration - A user can click the `Enable Edit` button in the top right of the ui and enter this key to enabled edit mode for the rest of their current session


####Startup
`BaragonService` first needs to be able to connect to zookeeper. On first launch it will create a base node for all [Baragon related zookeeper data](#zookeeper) at the location specified by `zookeeper` => `zkNamespace` in the [Baragon Service configuration](baragon_service_config.yaml).

Once connected to zookeeper, if the instance is a leader, the Baragon Request Worker will start checking the pending request queue and processing requests. Note that only the instance that is currently the leader will process requests from the queue in teh case where there is more than one Baragon Service instance in the same cluster.

<a id="agentsetup"></a>
### Baragon Agent Setup and Configuration

The `BaragonAgentService` can be run similarly to Baragon Service.

1. Build JARs via `mvn clean package`.

2. Create a [configuration file for Baragon Agent](baragon_agent_config.yaml) (`$AGENT_CONFIG_YAML`)

3. Copy `BaragonAgentService-*-SNAPSHOT.jar` and `$AGENT_CONFIG_YAML` onto one or more hosts, and start the service via `java -jar BaragonAgentService-*-SNAPSHOT.jar server $AGENT_CONFIG_YAML`.
  - Note that you can specify arguments to the java vm in this command that will override values from the configuration file. These can be specified in the format `-Ddw.(path_to_config_name)=(value)`
    - ex. setting the port for Baragon Service to use via an environment variable `-Ddw.server.connector.port=$PORT`
    - ex. set the load balancer group name the isntance belongs to `-Ddw.loadBalancerConfig.name=name-of-group`

####Configuration
The `hostname` field of the Agent configuration must be accurate in order for the Agent to receive requests. Baragon Service must to be able to contact the agent at `hostname`:`port`/`(agent context path)` (or an alternative url format speciified in the `baseUrlTemplate` field) in order to trigger a request on the agent.

Templates are the key factor for laying down the configuration from the data in a Baragon Request. Baragon uses handlebars as a templating engine. Templates in the configuration contain a few key elements:
- `filename`: The resulting configuration after processing via handlebars will be written to `loadBalancerConfig` => `rootPath`/`filename`
  - Note that you must have a `%s` or other `String.format` compatible field in you filename, as this will be replaced with the `loadBalancerService` name from the Baragon Request.
- `template`: The default template to render for this file path
- `namedTemplates`: Alternate templates specified as `name`:`(template content)` which will be render if the `BaragonRequest` being applied has a `templateName` field of the same value.
  - Note that if a request is made for a `templateName` that does not match any `namedTemplates` present on the agent, the request will fail and not be applied. An empty `templateName` field or a value of `default` will cause the default `template` to be rendered.
  - A namedTemplate does not have to exist for all `filename` fields. As long as one template of the requested name is found, the request can be applied. For example, if a certain type fo configuration only needs a `proxy/%s.conf` file written and not an `upstreams/%s.conf` file. Specifying a namedTemplate under only the `proxy/%s.conf` `filename` will cause only that file to be written for a request with a matching `templateName` field.

You can specify as many file paths as you need in the Agent configuration.

####Startup

On startup the Baragon Agent instance will do the following:

- Apply current requests:
  - Because the current state of all requests to Baragon is stored in zookeeper, an agent will first make sure it's written load balancer configuration matches that of other agents already running in the same load balancer group. The agent will do this by replaying the most recent successful request for each active `loadBalancerService` in its group after checking that an identical configuration file doesn't already exist.
  - Note that errors during the bootstrap apply process will cause configuration for that service not to be written (if it is written and the configuration is invalid the change will be rolled back). You can submit a requests for the same service to try rewriting the configuration
- Create leader latch
  - At this point, the agent is ready to start processing requests and will appear in the list of active agents when the Baragon Request Worker looks for agents in a particular load balancer group
- Add to Known Agents List
  - while the leader latch entry will disappear if the agent stops running, an additional entry (identical except for an added timestamp) is also added that will persist. This known agents list helps keep track of every agent Baragon has seen at one point or another and can be helpful for debugging.
- Notify BaragonService
  - Once the agent is ready to process updates, it will notify BaragonService via an http call. This is mostly important when ELB sync is used, because BaragonService will use this to trigger the initial addition of that instance to the ELB

<a id="nginx"></a>
#### Setting up Baragon Agent with Nginx

Baragon was originally built around use with nginx, although it can be adapted for other uses as well. A basic setup with Baragon and nginx can be accomplished using the following example.

First we need to set up a few basic nginx configs. In your `nginx.conf` file (most likely located in `/etc/nginx`), you should add the following line:
```
include /etc/nginx/conf.d/*.conf;
```

This will allow us to add a few more files in the `/etc/nginx/conf.d` folder (or another location of your choice)

In `/etc/nginx/conf.d/vhost.conf` we will add the entry that defines our main `server` block

```
server {
    listen 80 backlog=4096;
    listen 443 ssl default_server backlog=4096;
    root /var/www/html/;
    location /error/ {
        alias /var/www/error/;
    }
    error_page 404 /error/404.html;
    error_page 500 /error/500.html;
    include conf.d/proxy/*.conf;
}
```

With this configuration, nginx will include all files in the `conf.d/proxy/` folder. If we define one of our `BaragonAgent` templates to have a filename of `proxy/%s.conf` (assumign the `rootPath` is `/etc/nginx/conf.d`), each proxy configuration written by Baragon will be included.

In the example configuration, the proxy configuration defines a few rules, but mainly serves to proxy the request to an upstream, which we define in the next section...

In `/etc/nginx/conf.d/baragon.conf` we have the following:
```
include /etc/nginx/conf.d/upstreams/*.conf;
```

This will include all of the upstream files, defined in our example `BaragonAgent` config under the template with filename `upstreams/%s.conf`


Looking at our example configurations, a simple request might result in something like the following for a service with id `testService`:

- in our proxy configuration:
```
location /test {
  proxy_pass http://baragon_testService;
}
```

- in our upstream configuration:
```
upstream baragon_testService {
  server myupstream.com:80;
  server myupstream2.com:80;
}
```

The resulting configuration will cause the added upstream's running applications to be available on the `/test` path for the current load balancer. Subsequent requests can then add or remove upstreams when applications are stopped or started on different hosts.

<a id="haproxy"></a>
#### Setting up Baragon Agent with Haproxy

Baragon can be used to manage Haproxy configuration as well. While Haproxy does not currently support an `include` type statement, you can pass multiple `-f` options for configuration files to parse on startup. These will then all be stored in memory as one larger configuration.

One possible strategy for implementing with Haproxy is have a line in a modified `/etc/init.d/haproxy` similar to the following:

```
for FILE in \`find /etc/haproxy/conf.d -type l | sort -n\`; do
  CONFIGS="$CONFIGS -f $FILE"
done

# Pass $CONFIGS as an argument when starting haproxy
```
This will include all configs in the conf.d folder when restarting haproxy.

<a id="singularity"></a>
### Integration with Singularity

Integration will singularity is simple! In you `Singularity` configuration under the `loadBalancerUri` field, enter the full path to `BaragonService`'s `/request` enpoint. For example:

`"loadBalancerUri":"(Baragon Service host):(Baragon Service port)/(context path)/request"`

When creating the `Singularity` request:
- set `loadBalanced` to true when creating your singularity request

When creating a `Singularity` deploy for the load balanced request:
- set the `serviceBasePath` field
- set the `loadBalancerGroups` field
- If extra options are needed (coressponding to the options field in the `loadBalancerService`), add these in the `loadBalancerOptions` field

```json
"serviceBasePath": "/content",
"loadBalancerGroups": [
    "group"
],
"loadBalancerOptions": {
    "nginxExtraConfigs": [
        "rewrite ^/test$ / permanent;"
    ],
    "nginxExtraUpstreamConfigs": [
        "least_conn;"
    ]
}
```

Singularity will send all needed requests to add running tasks as upstreams to the load balancer configuration and remove them when the task is no longer running.

<a id="zookeeper"></a>
### Data Storage in Zookeeper

- Coming Soon

<a id="requests"></a>
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

####Posting the request
When a request is first posted to Baragon's `/request` endpoint, BaragonService will attempt to enqueue the request. This can have three outcomes:
- If the request is new (unique requestId), it is added to the pending request queue
- If the request is already present (requestId already used), but the request content matches the original request, the response to the original request will be returned (ie. the same request won't be enqueued again)
- If the request is already present, but the request content does not match, an error will be thrown

####Initial processing of the request (`PENDING` internal state / `WAITING` status)
Once the request is in the pending request queue, it will be picked up by the request worker. The request will initially be in the `PENDING` state and the request worker will do the following:
- Check that there are no base path conflicts, moving the request to `INVALID_REQUEST_NOOP` if any are found
- Check that no non-existent load balancer groups have been requested, moving the request to `INVALID_REQUEST_NOOP` if any are found
- set the lock for the base paths in the request if it has not already been set to prevent other operations on the base path while the request is being processed
- If all checks have passed, move the request to the internal `SEND_APPLY_REQUESTS` status

####Sending apply requests to agents (`SEND_APPLY_REQUESTS` internal status / `WAITING` status)
The request worker will send async requests to each agent in any load balancer groups that need to be updated during this step, then internally move the state to `CHECK_APPLY_RESPONSES` while waiting for requests to complete. The responses code of the agent post requests sent will be used to determine the status

####Check agent responses (`CHECK_APPLY_RESPONSES` internal state, `WAITING` status)
The request worker will gather the responses from all agents it sent requests to and translate those to one of the following statuses:
- `WAITING`: Some requests are still being processed, none have failed yet, stay in the `CHECK_APPLY_RESPONSES` state
- `RETRY`: Some requests have failed, but the limit for number of retries was not yet reached. Send the requests again (`SEND_APPLY_REQUESTS`) and move back to `CHECK_APPLY_RESPONSES`
- `CANCELLED_SEND_REVERT_REQUESTS`: If the request was cancelled but some changes have been made, send new requests agents to revert the changes (status of `CANCELLING`)
  - `CANCELLED_CHECK_REVERT_RESPONSES`: similar to `CHECK_APPLY_RESPONSES`, a success produces the `CANCELLED` state/status and a failure will produce the `FAILED_CANCEL_FAILED` state(`FAILED` status)
- `FAILURE`: One or more of the requests has failed and the retry limit has been reached, move to `FAILED_SEND_REVERT_REQUESTS` state, this will cause new requests to be sent (similar to step 3), but the applied data will be from the most recent successful request (data stored in Baragon state node)
  - `FAILED_SEND_REVERT_REQUESTS`: Send requests to apply previous data and revert the load balancer changes
  - `FAILED_CHECK_REVERT_RESPONSES`: Similar to `CHECK_APPLY_RESPONSES`, but a failure will cause `FAILED_REVERT_FAILED` state and an overall status of `FAILED`, while a success will cause the `FAILED_REVERTED` state (still overall status of `FAILED` for the request)
- `INVALID_REQUEST_NOOP`: A failure has occurred but no action was taken on any load balancer. A revert is not needed, so undo any base path lock changes and return the `INVALID_REQUEST_NOOP` status

####Commit the request if successful (`SUCCESS` state/status)
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

<a id="basepaths"></a>
## BasePath Locking and Updating

When adding a service to Baragon, the service must have an associated `basePath`. Originally built around nginx, this was to ensure that no two services would attempt to be hosted on the same path on the same load balancer (causing an invalid configuration). The base path lock is always associated with a serviceId. Some notes on locking and changing base paths for a service:

- base paths are locked at the beginning of request processing to avoid conflicts
- If a service is moved off of a load balancer group (ie. changed from group A to group B) the base path lock is released on the old group
- If a base path changes the lock on the old one is released
- To have a new serviceId take over a base path that is currently locked, use the `replaceServiceId` request field. (The alternative to this is to make a request to remove the previous service than a request to add the new service. This is not an instant switch and could cause down time)
  - Example:
    - Service `A` is at base path `/test`
    - Make a request for service `B` with `replaceServiceId` set to `A`
    - Service `A`'s configs are removed, service `B`'s configs are added, base path `/test` is now associated with service `B`

<a id="elb"></a>
## ELB Sync

`BaragonService` has the ability to keep you instances in sync with a traffic source (Right now only Amazon ELB). You can specify the following under `elb` in you `BaragonService` config to enable the ELB Sync and configure several safeguards.

- `enabled`: Must be `true` for the elb sync worker to run, defaults to `false`
- `awsAccessKeyId`, `awsAccessKeySecret`: AWS access key and secret. The IAM credentials require the following permissions
  - `DeregisterInstancesFromLoadBalancer`
  - `RegisterInstancesWithLoadBalancer`
  - `DescribeLoadBalancers`
  - `DescribeInstanceHealth`

  *Adding AWS keys will enable a visual ELBs page in the UI, even if the ELB Sync worker is not enabled*

- ElbSyncWorker Configuration
  - `intervalSeconds`: The interval at which to run the elb sync worker, default is `120` seconds, can be no shorter than a minute
  - `initialDelaySeconds`: Initial delay after startup before trying to start the elb sycn worker, default is `0`
  - `removeLastHealthyEnabled`: If this is `false`, Baragon will not be allowed to deregister a healthy agent from the ELB if it is the last healthy instance registered with that ELB. Defaults to `false`
  - `removeKnownAgentEnabled`: If this is `false`, Baragon will not be allowed to deregister an agent if it is still in the known agents list. Defaults to `false`
  - `removeKnownAgentMinutes`: Number of minutes after which Baragon is allowed to deregister an agent that is still in the knownAgents list. (ie. has not been active in x minutes). This has no effect if `removeKnownAgentEnabled` is false. Default is 30 minutes.
  - `deregisterEnabled`: If this is `false`, Baragon will only be allowed to register new agents, not deregister. Defaults to `false`
  
BaragonService will also use `PathChildrenCacheListener`s to watch for the addition of new agents to the cluster. This way, new agents will be in the elb as soon as they have applied their configuration successfully.

Once BaragonService is running with valid AWS credentials, you can add a `trafficSource` to a load balancer group. This corressponds to the ELB name associated with that group. You can add multiple traffic sources. Baragon will then attempt to keep the active agents in its datastore in sync with the agents registered with the ELB.