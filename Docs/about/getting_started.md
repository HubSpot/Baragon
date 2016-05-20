## Getting Started

For more details on configuring and using Baragon, check out the [detailed setup and management guide](../managing_baragon.md)

** Prerequisite: A working ZooKeeper cluster **

1. Build JARs via `mvn clean package`.

2. Create a configuration file for Baragon Service and Baragon Agent. These are an extended version of a Dropwizard configuration file. Details on configurable fields can be found in the example configs below and in the [detailed setup and management guide](../managing_baragon.md) 
  - [Example Baragon Service Configuration](../config/baragon_service_config.md). This will be referenced as `$SERVICE_CONFIG_YAML`.
  - [Example Baragon Agent Configuration](../config/baragon_agent_config.md). This will be referenced as `$AGENT_CONFIG_YAML`.

3. Copy `BaragonService-*-SNAPSHOT.jar` and `$SERVICE_CONFIG_YAML` onto one or more hosts, and start the service via `java -jar BaragonService-*-SNAPSHOT.jar server $SERVICE_CONFIG_YAML`.

4. Copy `BaragonAgentService-*-SNAPSHOT.jar` and `$AGENT_CONFIG_YAML` onto each of your load balancer hosts. Start the BaragonAgent service via `java -jar BaragonAgentService-*-SNAPSHOT.jar server $AGENT_CONFIG_YAML`.

## Quickstart with Docker Compose

To get an example cluster up and running, you can install [docker](https://docs.docker.com/installation/) and [docker-compose](https://docs.docker.com/compose/#installation-and-set-up).

Simply run `docker-compose up` to bring up:
- zookeper container
- Baragon Service container
- Two Baragon Agent + Nginx containers

The Baragon UI will be available at [localhost:8080](http://localhost:8080) and nginx at [localhost:80](http://localhost:80).

***If using boot2docker replace localhost with the `boot2docker ip`***

Nginx's config directories that BaragonAgent writes to will also be mounted as volumes in the `docker/configs` folder on your local machine.

## BaragonUI
 
Baragon comes with a UI for visualization and easier management of load balancer paths and upstreams. By default it will be available in a read-only mode at `/[contextPath]/ui` see the [Example Baragon Service Configuration](../Docs/baragon_service_config.yaml) or [detailed setup and management guide](../managing_baragon.md) for more details on configuring BaragonUI behavior.
