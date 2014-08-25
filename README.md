# Baragon [![Build Status](https://travis-ci.org/HubSpot/Baragon.svg?branch=master)](https://travis-ci.org/HubSpot/Baragon)

![Behold the mighty Baragon's roar](http://i.imgur.com/mCbkbcZ.jpg)

Baragon is a system for automating load balancer configuration updates. It pairs well with the [Singularity](https://github.com/HubSpot/Singularity) Mesos framework.

## Concept

Baragon is made up of two services:

- BaragonService -- coordination service

- BaragonAgentService -- applies changes on the actual load balancer

When a web service changes (i.e. upstreams added / removed), POST a [BaragonRequest](BaragonCore/src/main/java/com/hubspot/baragon/models/BaragonRequest.java) JSON object to BaragonService's `request` endpoint. BaragonService will then fan out this change to all affected BaragonAgentServices. Polling the request status url (`request/{requestId}`) will indicate the current status of the request:

- `WAITING`: waiting for request to be applied on all agents
- `SUCCESS`: request was successfully applied on all agents
- `FAILED`: request was not successfully applied on all agents, and was rolled back
- `CANCELING`: request is in the process of being cancelled (rolled back)
- `CANCELED`: request was cancelled (rolled back) on all agents
- `UNKNOWN`: unknown.

## Setup

Prerequisite: A working ZooKeeper cluster.

1. Build JARs via `mvn clean package`.

2. Create a configuration file for the main service. An example lives [here](docs/example_service_config.yaml). This will be referenced as `$SERVICE_CONFIG_YAML`.

3. Create a configuration file for agent service. An example lives [here](docs/example_agent_config.yaml). This will be referenced as `$AGENT_CONFIG_YAML`.

4. Copy BaragonService*.jar and `$SERVICE_CONFIG_YAML` onto one or more hosts, and start the service via `java -jar BaragonService-*-SNAPSHOT.jar server $SERVICE_CONFIG_YAML`.

5. Copy BaragonAgentService*.jar and `$AGENT_CONFIG_YAML` onto each of your load balancer hosts. Start the BaragonAgent service via `java -jar BaragonAgentService-*-SNAPSHOT.jar server $AGENT_CONFIG_YAML`.
