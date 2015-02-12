# Baragon [![Build Status](https://travis-ci.org/HubSpot/Baragon.svg?branch=master)](https://travis-ci.org/HubSpot/Baragon)

![Behold the mighty Baragon's roar](http://i.imgur.com/mCbkbcZ.jpg)

Baragon is a system for automating load balancer configuration updates. It pairs well with the [Singularity](https://github.com/HubSpot/Singularity) Mesos framework.

## Concept

Baragon is made up of two services:

- BaragonService -- coordination service

- BaragonAgentService -- applies changes on the actual load balancer

When a web service changes (i.e. upstreams added / removed), POST a [BaragonRequest](BaragonCore/src/main/java/com/hubspot/baragon/models/BaragonRequest.java) JSON object to BaragonService's `/[contextname]/request` endpoint like this one:

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

BaragonService will then fan out this change to all affected BaragonAgentServices. Polling the request status url (`/[contextname]/request/{requestId}`) will indicate the current status of the request:

- `WAITING`: waiting for request to be applied on all agents
- `SUCCESS`: request was successfully applied on all agents
- `FAILED`: request was not successfully applied on all agents, and was rolled back
- `CANCELING`: request is in the process of being cancelled (rolled back)
- `CANCELED`: request was cancelled (rolled back) on all agents
- `UNKNOWN`: unknown.

## Resources

* `/auth` - 
* `/keys` - 
* `/keys/{key}` - 
* `/keys` - 
* `/load-balancer` - Array of currently configured Baragon clusters
* `/load-balancer/{clusterName}/hosts` - Array of URLs for Baragon agents
* `/load-balancer/{clusterName}/agents` - Array of all known Baragon agent objects
* `/load-balancer/{clusterName}/known-agents` - Array of recently seen Baragon agent objects
* `/load-balancer/{clusterName}/known-agents/{agentId}` - 
* `/load-balancer/{clusterName}/base-path/all` - 
* `/load-balancer/{clusterName}/base-path` - 
* `/load-balancer/{clusterName}/base-path` - 
* `/request` - Array of currently pending requests
* `/request/{requestId}` - 
* `/state` - Master list of services and upstreams
* `/state/{serviceId}` - Supports `GET` and `DELETE`
* `/status` - Show full status of this load balancer cluster
* `/workers` - List members of this Baragon server cluster

## Setup

Prerequisite: A working ZooKeeper cluster.

1. Build JARs via `mvn clean package`.

2. Create a configuration file for the main service. An example lives [here](docs/example_service_config.yaml). This will be referenced as `$SERVICE_CONFIG_YAML`.

3. Create a configuration file for agent service. An example lives [here](docs/example_agent_config.yaml). This will be referenced as `$AGENT_CONFIG_YAML`.

4. Copy BaragonService*.jar and `$SERVICE_CONFIG_YAML` onto one or more hosts, and start the service via `java -jar BaragonService-*-SNAPSHOT.jar server $SERVICE_CONFIG_YAML`.

5. Copy BaragonAgentService*.jar and `$AGENT_CONFIG_YAML` onto each of your load balancer hosts. Start the BaragonAgent service via `java -jar BaragonAgentService-*-SNAPSHOT.jar server $AGENT_CONFIG_YAML`.

## Quickstart with Vagrant

Baragon comes with Vagrant boxes for easy local development and integration testing. Ensure Virtualbox and Vagrant is installed on your machine, and then run `vagrant up` inside the `vagrant` folder of the git repo to spin up a Baragon cluster for testing.

###Example Requests Using Vagrant Boxes and Curl
The Baragon Agent and Baragon Service can be tested using the example curl requests below

- add a config for ‘app1’
```sh
curl -H “Content-Type: application/json” --data ‘{
  “loadBalancerRequestId”: “5”,
  “loadBalancerService”: {
    “serviceId”: “app1”,
    “owners”: [“foo”],
    ”serviceBasePath”:“/basepath”,
    “loadBalancerGroups”: [“vagrant”],
    “options”:{”nginxExtraConfigs”:[“rewrite ^/basepath/(\\+d) /basepath/$1 last;”]},
  },
“addUpstreams”: [“1.1.1.1:80”],
“removeUpstreams”: []
}’ 192.168.33.20:8080/baragon/v1/request
```

- remove ‘app1’ from the load balancer
```sh
curl -H “Content-Type: application/json” --data ‘{
  “loadBalancerRequestId”: “5”,
  “loadBalancerService”: {
    “serviceId”: “app1”,
    “owners”: [“foo”],
    ”serviceBasePath”:“/basepath”,
    “loadBalancerGroups”: [“vagrant”],
    “options”:{”nginxExtraConfigs”:[“rewrite ^/basepath/(\\+d) /basepath/$1 last;”]},
  },
“addUpstreams”: [],
“removeUpstreams”: [“1.1.1.1:80”]
}’ 192.168.33.20:8080/baragon/v1/request
```




