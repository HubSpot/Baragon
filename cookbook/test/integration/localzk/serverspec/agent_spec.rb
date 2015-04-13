# Encoding: utf-8
require 'spec_helper'

describe 'Services' do
  [8088, 8882].each do |p|
    describe port(p) do
      it { is_expected.to be_listening.with('tcp') }
    end
  end
  describe port(2181) do
    it { is_expected.to be_listening.with('tcp6') }
  end
  it 'has a running and enabled baragon-agent service' do
    expect(service('baragon-agent-default')).to be_enabled
    expect(service('baragon-agent-default')).to be_running
  end
  it 'has a running and enabled baragon-server service' do
    expect(service('baragon-server-default')).to be_enabled
    expect(service('baragon-server-default')).to be_running
  end
end
