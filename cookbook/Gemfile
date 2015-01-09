source 'https://rubygems.org'

group :test, :development do
  gem 'rake'
end

group :test do
  gem 'berkshelf',  '~> 3.2'
  gem 'foodcritic', '~> 4.0'
  gem 'rubocop',    '~> 0.27'
end

group :test, :integration do
  gem 'test-kitchen',
      github: 'test-kitchen/test-kitchen',
      tag: '2ae3e6813e8f8eb2a0fd3f1a274ac63d9a9379bf'
end

group :test, :vagrant do
  gem 'kitchen-vagrant', '~> 0.15'
end
