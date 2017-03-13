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
Alternatively you can provision your Baragon instances via chef using the cookbook provided in this repository. The `baragon` cookbook is available in the public chef supermarket.

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

Integration with Singularity is simple! In your `Singularity` configuration, under the `loadBalancerUri` field, enter the full path to `BaragonService`'s `/request` endpoint. For example:

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
