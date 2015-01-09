node.set[:java][:jdk_version] = 7

include_recipe 'java'
include_recipe 'baragon::build'

directory '/etc/baragon'

unless node[:baragon][:mocking]
  node.set[:baragon][:zk_hosts] =
                    search(:node,
                           "chef_environment:#{node.chef_environment} AND " \
                           'roles:zookeeper').map { |n| "#{n[:fqdn]}:2181" }

  if node[:baragon][:zk_hosts].empty?
    fail 'Search returned no Zookeeper server nodes'
  end
end
