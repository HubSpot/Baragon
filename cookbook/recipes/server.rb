node.set['zookeeper']['service_style'] = 'upstart'

include_recipe 'zookeeper'
include_recipe 'zookeeper::service'
include_recipe 'baragon::common'

creds = Chef::EncryptedDataBagItem.load('secrets',
                                        'aws_credentials')['Baragon']

baragon_server_jar = 'BaragonService-0.1.5-SNAPSHOT.jar'

s3_file "/usr/share/java/#{baragon_server_jar}" do
  aws_access_key_id     creds['access_key_id']
  aws_secret_access_key creds['secret_access_key']
  bucket                'ops.evertrue.com'
  remote_path           "/pkgs/#{baragon_server_jar}"
  owner                 'root'
  group                 'root'
  mode                  0644
end

template '/etc/baragon/service.yml' do
  source 'service.yml.erb'
  owner  'root'
  group  'root'
  mode   0644
  notifies :restart, 'service[baragon-server]'
end

template '/etc/init/baragon-server.conf' do
  source 'baragon-server.init.erb'
  owner  'root'
  group  'root'
  mode   0644
  notifies :restart, 'service[baragon-server]'
  variables baragon_jar: baragon_server_jar,
            config_yaml: '/etc/baragon/service.yml'
end

service 'baragon-server' do
  provider Chef::Provider::Service::Upstart
  supports status: true,
           restart: true
  action   [:enable, :start]
end
