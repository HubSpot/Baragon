default[:baragon][:service_log] = '/var/log/baragon/baragon_service.log'

default[:baragon][:service_yaml] = {
  'server' => {
    'type' => 'simple',
    'applicationContextPath' => '/baragon/v2',
    'connector' => {
      'type' => 'http',
      'port' => 8088
    }
  },
  'zookeeper' => {
    'sessionTimeoutMillis' => 60_000,
    'connectTimeoutMillis' => 5_000,
    'retryBaseSleepTimeMilliseconds' => 1_000,
    'retryMaxTries' => 3
  }
}
