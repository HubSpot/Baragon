default[:baragon][:group_name] = 'default'
default[:baragon][:proxy_conf_dir] = '/tmp'
default[:baragon][:upstream_conf_dir] = node[:baragon][:proxy_conf_dir]
default[:baragon][:agent_port] = 8882
default[:baragon][:agent_app_context_path] = '/baragon-agent/v2'
default[:baragon][:domain] = 'vagrant.baragon.biz'
default[:baragon][:agent_log] = '/var/log/baragon/baragon_agent.log'
