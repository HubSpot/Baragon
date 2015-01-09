node.set[:java][:jdk_version] = 7

include_recipe 'java'
include_recipe 'baragon::build'

directory '/etc/baragon'
