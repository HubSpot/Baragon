<a id="top"></a>
# Baragon [![Build Status](https://travis-ci.org/HubSpot/Baragon.svg?branch=master)](https://travis-ci.org/HubSpot/Baragon)

![Behold the mighty Baragon's roar](http://i.imgur.com/mCbkbcZ.jpg)

Baragon is a system for automating load balancer configuration updates. It pairs well with the [Singularity](https://github.com/HubSpot/Singularity) Mesos framework.

## Contents

- [Baragon Basics](#basics)
- [Getting Started](#start)
  - [Quick Start](#start)
  - [Detailed Setup Guide](Docs/managing_baragon.md)
  - [Example Baragon Service Configuration](Docs/config/baragon_service_config.md)
  - [Example Baragon Agent Configuration](Docs/config/baragon_agent_config.md)
- [Quick Start With Docker Compose](#docker)
- [BaragonUI](#ui)
- [API Docs](Docs/api.md)
- [Developing](Docs/development.md)

<a id="basics"></a>
## Baragon Basics

Baragon is made up of two services:

- BaragonService -- coordination service

- BaragonAgentService -- applies changes on the actual load balancer

When a web service changes (i.e. upstreams added / removed), POST a [BaragonRequest](Docs/api.md#requests) JSON object to BaragonService's `/[contextPath]/request` endpoint like this one:

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

Check out the [API Docs](Docs/api.md) for additional `BaragonRequest` fields and returned values.

<a id="start"></a>
## Getting Started

For more details on configuring and using Baragon, check out the [detailed setup and management guide](Docs/managing_baragon.md)

** Prerequisite: A working ZooKeeper cluster **

1. Build JARs via `mvn clean package`.

2. Create a configuration file for Baragon Service and Baragon Agent. These are an extended version of a Dropwizard configuration file. Details on configurable fields can be found in the example configs below and in the [detailed setup and management guide](Docs/managing_baragon.md) 
  - [Example Baragon Service Configuration](Docs/config/baragon_service_config.md). This will be referenced as `$SERVICE_CONFIG_YAML`.
  - [Example Baragon Agent Configuration](Docs/config/baragon_agent_config.md). This will be referenced as `$AGENT_CONFIG_YAML`.

3. Copy `BaragonService-*-SNAPSHOT.jar` and `$SERVICE_CONFIG_YAML` onto one or more hosts, and start the service via `java -jar BaragonService-*-SNAPSHOT.jar server $SERVICE_CONFIG_YAML`.

4. Copy `BaragonAgentService-*-SNAPSHOT.jar` and `$AGENT_CONFIG_YAML` onto each of your load balancer hosts. Start the BaragonAgent service via `java -jar BaragonAgentService-*-SNAPSHOT.jar server $AGENT_CONFIG_YAML`.

<a id="docker"></a>
## Quickstart with Docker Compose

To get an example cluster up and running, you can install [docker](https://docs.docker.com/installation/) and [docker-compose](https://docs.docker.com/compose/#installation-and-set-up).

Simply run `docker-compose up` to bring up:
- zookeper container
- Baragon Service container
- Two Baragon Agent + Nginx containers

The Baragon UI will be available at [localhost:8080](http://localhost:8080) and nginx at [localhost:80](http://localhost:80).

***If using boot2docker replace localhost with the `boot2docker ip`***

Nginx's config directories that BaragonAgent writes to will also be mounted as volumes in the `docker/configs` folder on your local machine.

<a id="ui"></a>
## BaragonUI
 
Baragon comes with a UI for visualization and easier management of load balancer paths and upstreams. By default it will be available in a read-only mode at `/[contextPath]/ui` see the [Example Baragon Service Configuration](Docs/config/baragon_service_config.md) or [detailed setup and management guide](Docs/managing_baragon.md) for more details on configuring BaragonUI behavior.

## Baragon API Docs

Full documentation on the Baragon Service API can be found [here](Docs/api.md)
