# Baragon [![Build Status](https://travis-ci.org/HubSpot/Baragon.svg?branch=master)](https://travis-ci.org/HubSpot/Baragon)

![Baragon](http://i.imgur.com/mCbkbcZ.jpg)

Baragon is a system for automating load balancer configuration updates. It pairs well with the [Singularity](https://github.com/HubSpot/Singularity) Mesos framework.

## Contents

- About Baragon
  - [Basics](Docs/about/basics.md)
  - [Getting Started](Docs/about/getting_started.md)
- [Baragon API Docs](Docs/api.md)
- [Managing Baragon](Docs/managing_baragon.md)
  - [Terminology](Docs/managing/terminology.md)
  - [Launching a Cluster](Docs/managing/launch.md)
    - [Baragon Service Setup and Configuration](Docs/managing/launch.md#servicesetup)
    - [Baragon Agent Setup and Configuration](Docs/managing/launch.md#agentsetup)
      - [Setup With Nginx](Docs/managing/launch.md#nginx)
      - [Setup With Haproxy](Docs/managing/launch.md#haproxy)
    - [Integration with Singularity](Docs/managing/launch.md#singularity)
    - [Data Storage in Zookeeper](Docs/managing/launch.md#zookeeper)
  - [Making Requests to Baragon Service](Docs/managing/requests.md)
    - [Statuses and Request Flow](Docs/managing/requests.md)
    - [Common Exceptions](Docs/managing/requests.md)
  - [BasePath Locking and Updating](Docs/managing/basepaths.md)
  - [ELB Sync](Docs/managing/elb_sync.md)
- [Developing](Docs/development.md)
- [Example Configuration](Docs/config/config.md)