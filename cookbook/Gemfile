source 'https://rubygems.org'

group :test, :development do
  gem 'rake'
  gem 'coveralls', require: false
end

group :test do
  gem 'berkshelf',  '~> 3.2'
  gem 'chefspec',   '~> 4.1'
  gem 'foodcritic', '~> 4.0'
  gem 'rubocop',    '~> 0.27'

  gem 'fog', '~> 1.25'
end

group :test, :integration do
  gem 'test-kitchen',
      github: 'test-kitchen/test-kitchen',
      tag: '2ae3e6813e8f8eb2a0fd3f1a274ac63d9a9379bf'
  gem 'kitchen-ec2',
      github: 'test-kitchen/kitchen-ec2',
      tag: 'e7f840f927518b0f9e29914205c048a463de654e'
  gem 'serverspec', '~> 2.7'
end

group :test, :vagrant do
  gem 'kitchen-vagrant', '~> 0.15'
end
