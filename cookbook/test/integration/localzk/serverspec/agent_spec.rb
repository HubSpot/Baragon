# Encoding: utf-8
require 'spec_helper'

describe 'Services' do
  [8088, 8882].each do |p|
    describe port(p) do
      it { should be_listening.with('tcp') }
    end
  end
  describe port(2181) do
    it { should be_listening.with('tcp6') }
  end
end
