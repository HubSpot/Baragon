include_recipe 'baragon::common'

baragon_server_jar = "BaragonService-#{node[:baragon][:version]}.jar"

remote_file "/usr/share/java/#{baragon_server_jar}" do
  action   :create
  backup   5
  owner    'root'
  group    'root'
  mode     0644
  source   "file://#{Chef::Config[:file_cache_path]}/Baragon/BaragonService" \
           "/target/#{baragon_server_jar}"
end

node.set[:baragon][:service_yaml][:zookeeper][:quorum] =
  node[:baragon][:zk_hosts].join(',')
node.set[:baragon][:service_yaml][:zookeeper][:zkNamespace] =
  node[:baragon][:zk_namespace]

file '/etc/baragon/service.yml' do
  action   :create
  owner    'root'
  group    'root'
  mode     0644
  content  YAML.dump(JSON.parse(node[:baragon][:service_yaml].to_hash.to_json))
  notifies :restart, 'service[baragon-server]'
end

template '/etc/init/baragon-server.conf' do
  source    'baragon-server.init.erb'
  owner     'root'
  group     'root'
  mode      0644
  notifies  :restart, 'service[baragon-server]'
  variables baragon_jar: baragon_server_jar,
            config_yaml: '/etc/baragon/service.yml'
end

service 'baragon-server' do
  provider Chef::Provider::Service::Upstart
  supports status: true,
           restart: true
  action   [:enable, :start]
end
