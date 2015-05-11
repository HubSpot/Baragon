# Encoding: utf-8
require 'spec_helper'

describe 'Services' do
  describe port(8882) do
    it { is_expected.to be_listening.with('tcp') }
  end
  describe file('/etc/baragon/agent-default.yml') do
    it { is_expected.to be_file }

    describe '#content' do
      subject { super().content }
      it do
        is_expected.to match('zookeeper-1.vagrantup.com:2181,' \
                             'zookeeper-2.vagrantup.com:2181')
      end
    end
  end
end
