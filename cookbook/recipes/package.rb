include_recipe 'maven'

maven 'BaragonAgent' do
  group_id 'com.hubspot'
  classifier 'shaded'
  version node['baragon']['version']
  dest '/usr/share/java'
end
