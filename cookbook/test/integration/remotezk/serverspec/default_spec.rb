# Encoding: utf-8
require 'spec_helper'

describe 'Services' do
  describe port(8882) do
    it { should be_listening.with('tcp') }
  end
  describe file('/etc/baragon/agent.yml') do
    it { should be_file }
    its(:content) do
      should match('zookeeper-1.vagrantup.com:2181,' \
                   'zookeeper-2.vagrantup.com:2181')
    end
  end
end
