directory "#{Chef::Config[:file_cache_path]}/Baragon" do
  owner node[:baragon][:user]
end

execute 'build_baragon' do
  action  :nothing
  # Maven (or rather npm) has issues with
  # being run as root.
  user    node[:baragon][:user]
  environment('HOME' => '/home/baragon')
  command '/usr/bin/mvn clean package -DskipTests'
  cwd     "#{Chef::Config[:file_cache_path]}/Baragon"
end

package 'maven'

include_recipe 'git'

git "#{Chef::Config[:file_cache_path]}/Baragon" do
  repository 'https://github.com/HubSpot/Baragon.git'
  reference  node[:baragon][:git_ref]
  user       node[:baragon][:user]
  notifies   :run, 'execute[build_baragon]', :immediately
end
