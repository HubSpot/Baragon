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