node.set[:java][:jdk_version] = 7

%w(java
   zookeeper
   zookeeper::service
   baragon::server
   baragon::agent).each do |cb|
  include_recipe cb
end
