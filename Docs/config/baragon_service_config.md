## Example Baragon Service Config

{% raw %}
```yaml
# Configuration for Baragon Service and the corresponding request worker and ui
#
# The `server` section is the `server` configuration for a dropwizard application.
# `logging` and other dropwizard configurations can also be used
server:
  type: simple
  applicationContextPath: /baragon/v2
  connector:
    type: http
    port: 8080

zookeeper:
  quorum: localhost:2181  # comma separated list of zookeeper host:port goes here
  zkNamespace: baragon    # name of the base zk node
  sessionTimeoutMillis: 60000
  connectTimeoutMillis: 5000
  retryBaseSleepTimeMilliseconds: 1000
  retryMaxTries: 3

# Auth configuration
auth:
  key: test-key # This key must be provided as the authkey parameter on requests to Baragon Service
  enabled: true # Will use the specified key for auth if set to true
  uiWriteEnabled: true # Does this key allow edit access in BaragonUI

# Master auth key, can be used to create/destroy other auth keys
masterAuthKey: test-master-key

# Hostname, used in name during creation of leader latch
hostname: localhost

# Baragon request worker configuration, used for the worker process that executes requests
worker:
  enabled: true # Determines if the worker should start for this instance of Baragon Service
  intervalMs: 1000 # Interval between checks of the pending request queue
  initialDelayMs: 0 # Initial delay before the first check of the pending request queue

# (Optional) Format for building requests to Baragon Agents. First %s is substituted with the value created using 
# the value from the agent configs baseUrlTemplate, and the second %s is substituted with the request ID, generally
# you will not need to set this field
agentRequestUriFormat: "%s/request/%s"

# (Optional) Number of times to try and complete a successful request on an agent before failing the request
agentMaxAttempts: 5

# (Optional) Time to wait for a response from a Baragon Agent
agentRequestTimeoutMs: 60000

# (Optional) Configuration to purge old requests from zookeeper, defaults shown
history:
  purgeOldRequests: false # Should we purge old requests
  purgeOldRequestsAfterDays: 7 # delete requests older than this many days
  purgeWhenDateNotFound: false # if we can't determine when a request was last updated, should we delete it
  purgeEveryHours: 24 # how often to run the purger

# (Optional) Elb Sync configuration
elb:
  enabled: false # Determines if the ELB sync worker should start, defaults to false
  awsAccessKeyId: somekey # AWS credentials for accessing ELB api
  awsAccessKeySecret: somesecret # AWS credentials for accessing ELB api
  intervalSeconds: 120 # How oftent he sync worker should run, defaults to 120s
  initialDelaySeconds: 0 # How long to wait before starting ELB sync worker, defaults to 0
  deregisterEnabled: false # Is the sync worker allowed to remove instances from the elb, defaults to false
  removeKnownAgentEnabled: false # Is the sync worker allowed to remove an instance from the ELB if it is still in the known agents list, defaults to false
  removeKnownAgentMinutes: 30 # How long must an instance be inactive before it can be removed
  removeLastHealthyEnabled: false # If there is only one healthy instance left, can the sync worker remove it, defaults to false

# (Optional) HTTP client configuration, used by Baragon Service to comunicate with Baragon Agents
httpClient:
  maxRequestRetry: 5 # (Optional) Defaults to 5
  requestTimeoutInMs: 10000 # (Optional) Defaults to 10000
  connectionTimeoutInMs: 5000 # (Optional) Defaults to 5000
  userAgent: "Baragon/0.1 (+https://github.com/HubSpot/Baragon)" # (Optional) defaults to "Baragon/0.1 (+https://github.com/HubSpot/Baragon)"

# (Optional) Baragon UI configuration
ui:
  title: Baragon # Shown in ui nav bar
  navColor: null
  baseUrl: null # Different url to host the ui, otherwise the ui will be rechable at /[contextPath]/ui
  allowEdit: false # Allow the ui to run in edit mode (user can trigger requests and make changes), defaults to false (read-only)
  allowEditKey: test-ui-key # A separate key to enable edit mode in an otherwiser read-only ui (can also be accomplished using an auth key with uiWriteEnabled set to true)

# (Optional) enable a cors filter for this host, defaults to false
enableCorsFilter: false
```
{% endraw %}
