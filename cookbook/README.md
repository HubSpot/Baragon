# baragon-cookbook

Installs Baragon (service or agent) on a node. Baragon is the loadbalancer management service for [HubSpot's Singularity cluster management system](https://github.com/HubSpot/Singularity).

## Supported Platforms

Tested on an extensive range of platforms including Ubuntu 14.04 and nothing else.

## Requirements

### Zookeeper

Baragon depends on ZooKeeper to store internal state, and perform coordination between multiple `BaragonService` instances if high availability is desired. Using an existing Zookeeper cluster is totally fine if you happen to have one. Separate Baragon installations can also share the same Zookeeper cluster, provided they use different `zkNamespace` values.

### Nginx

Baragon writes nginx-style config files by default. In theory other load balancers could be used, but the tool is designed around the style of config files used by nginx.

The main requirement is that each application and upstream has its own file containing all necessary configuration directives (as opposed to, say, haproxy, where all configuration is parsed in order from top to bottom from a single, monolithic file).

## Attributes

<table>
  <tr>
    <th>Key</th>
    <th>Type</th>
    <th>Description</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><tt>[:baragon][:group_name]</tt></td>
    <td>String</td>
    <td>Load Balancer Group Name</td>
    <td><tt>default</tt></td>
  </tr>
  <tr>
    <td><tt>[:baragon][:proxy_conf_dir]</tt></td>
    <td>String</td>
    <td>Directory where the proxy config files are placed</td>
    <td><tt>/tmp</tt></td>
  </tr>
  <tr>
    <td><tt>[:baragon][:upstream_conf_dir]</tt></td>
    <td>String</td>
    <td>Directory where the upstream config files are placed</td>
    <td>Value of <tt>node[:baragon][:proxy_conf_dir]</tt></td>
  </tr>
  <tr>
    <td><tt>[:baragon][:server_port]</tt></td>
    <td>Integer</td>
    <td>Baragon service listener port</td>
    <td><tt>8080</tt></td>
  </tr>
</table>

## Usage

### baragon::server

Include `baragon::server` in your node's `run_list`:

```json
{
  "run_list": [
    "recipe[baragon::server]"
  ]
}
```

…or in a wrapper cookbook:

```ruby
include_recipe 'baragon::server'
```

### baragon::agent

Include `baragon::agent` in your node's `run_list`:

```json
{
  "run_list": [
    "recipe[baragon::agent]"
  ]
}
```

…or in a wrapper cookbook:

```ruby
include_recipe 'baragon::agent'
```

## License and Authors

Author:: EverTrue, Inc. (<devops@evertrue.com>)
