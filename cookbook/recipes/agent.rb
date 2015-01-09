include_recipe 'baragon::common'

["#{node[:baragon][:proxy_conf_dir]}/proxy",
 "#{node[:baragon][:upstream_conf_dir]}/upstreams"].each do |dir|
  directory dir do
    recursive true
  end
end

baragon_agent_jar = "BaragonAgentService-#{node[:baragon][:version]}.jar"

remote_file "/usr/share/java/#{baragon_agent_jar}" do
  action   :create
  backup   5
  owner    'root'
  group    'root'
  mode     0644
  source   "file://#{Chef::Config[:file_cache_path]}/Baragon/" \
           "BaragonAgentService/target/#{baragon_agent_jar}"
end

template '/etc/baragon/agent.yml' do
  source   'agent.yml.erb'
  owner    'root'
  group    'root'
  mode     0644
  notifies :restart, 'service[baragon-agent]'
end

template '/etc/init/baragon-agent.conf' do
  source    'baragon-agent.init.erb'
  owner     'root'
  group     'root'
  mode      0644
  notifies  :restart, 'service[baragon-agent]'
  variables baragon_jar: baragon_agent_jar,
            config_yaml: '/etc/baragon/agent.yml'
end

service 'baragon-agent' do
  provider Chef::Provider::Service::Upstart
  supports status: true,
           restart: true
  action   [:enable, :start]
end
