## Terminology
- `BaragonService` - The api and accompanying worker that is central to all agents. BaragonService manages all requests, provides information about running services, and also serves up the `BaragonUI`
- `BaragonAgent` or `BaragonAgentService` - The process running on each load balancer host. The agent is responsible for actually applying requests as resulting configuration files using templates
- `BaragonUI` - A backbone.js user interface with the same capabilities as most `BaragonService` endpoints
- `cluster` - All `BaragonService` and `BaragonAgent` instances in the same zookeeper quorum and namespace
- `loadBalancerService` - An ID unique across a cluster which is the primary key for storing all information (paths, upstreams, etc) once a request has been successfuly applied. Associated with the `BaragonService`/`BaragonServiceState` objects
- `loadBalancerRequest` - A `BaragonRequest` object that can be posted to `BaragonService` to either update or create a `loadBalancerService`
- `loadBalancerGroup` - A group of agents that should all have identical configuration, grouped by a name. For example, several nginx load balancers behind the same ELB, or serving content for the same domain
- `basePath` - The subpath on a loadbalancer at which the applied service should be available, must be unique within a `loadBalancerGroup`