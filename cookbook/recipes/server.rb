include_recipe 'baragon::common'

case node[:baragon][:install_type]
when 'source'
  include_recipe 'baragon::build'

  remote_file "/usr/share/java/BaragonService-#{node[:baragon][:version]}-shaded.jar" do
    action   :create
    backup   5
    owner    'root'
    group    'root'
    mode     0644
    source   "file://#{Chef::Config[:file_cache_path]}/Baragon/BaragonService" \
             '/target/' \
             "BaragonService-#{node[:baragon][:version]}-SNAPSHOT-shaded.jar"
  end
when 'package'
  include_recipe 'maven'

  maven 'BaragonService' do
    group_id 'com.hubspot'
    classifier 'shaded'
    version node['baragon']['version']
    dest '/usr/share/java'
  end
else
  fail "Unsupported install type: #{node[:baragon][:install_type]}"
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
  variables config_yaml: '/etc/baragon/service.yml'
end

service 'baragon-server' do
  provider Chef::Provider::Service::Upstart
  supports status: true,
           restart: true
  action   [:enable, :start]
end
