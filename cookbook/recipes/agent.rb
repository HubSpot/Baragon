include_recipe 'baragon::common'

["#{node[:baragon][:proxy_conf_dir]}/proxy",
 "#{node[:baragon][:upstream_conf_dir]}/upstreams"].each do |dir|
  directory dir do
    recursive true
  end
end

creds = Chef::EncryptedDataBagItem.load('secrets',
                                        'aws_credentials')['Baragon']

baragon_agent_jar = 'BaragonAgentService-0.1.5-SNAPSHOT.jar'

s3_file "/usr/share/java/#{baragon_agent_jar}" do
  aws_access_key_id     creds['access_key_id']
  aws_secret_access_key creds['secret_access_key']
  bucket                'ops.evertrue.com'
  remote_path           "/pkgs/#{baragon_agent_jar}"
  owner                 'root'
  group                 'root'
  mode                  0644
end

baragon_server = search(:node,
                             "chef_environment:#{node.chef_environment} AND " \
                             'recipes:baragon\:\:server').first

fail 'Search returned no Baragon server nodes' if baragon_server.nil?

template '/etc/baragon/agent.yml' do
  source 'agent.yml.erb'
  owner  'root'
  group  'root'
  mode   0644
  variables(baragon_server_host: baragon_server[:fqdn])
  notifies :restart, 'service[baragon-agent]'
end

template '/etc/init/baragon-agent.conf' do
  source 'baragon-agent.init.erb'
  owner  'root'
  group  'root'
  mode   0644
  notifies :restart, 'service[baragon-agent]'
  variables baragon_jar: baragon_agent_jar,
            config_yaml: '/etc/baragon/agent.yml'
end

service 'baragon-agent' do
  provider Chef::Provider::Service::Upstart
  supports status: true,
           restart: true
  action   [:enable, :start]
end
