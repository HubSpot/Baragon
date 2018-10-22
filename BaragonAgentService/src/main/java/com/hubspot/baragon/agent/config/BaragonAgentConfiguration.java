package com.hubspot.baragon.agent.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.config.GraphiteConfiguration;
import com.hubspot.baragon.config.HttpClientConfiguration;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.models.BaragonAgentGcloudMetadata;

import io.dropwizard.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaragonAgentConfiguration extends Configuration {
  public static final long DEFAULT_AGENT_LOCK_TIMEOUT_MS = 5000;
  public static final String DEFAULT_AGENT_BASE_URL_TEMPLATE = "http://%s:%d%s";
  public static final String DEFAULT_DATE_FORAMT = "yyyy-MM-dd hh:mm a";

  @JsonProperty("zookeeper")
  @NotNull
  private ZooKeeperConfiguration zooKeeperConfiguration;

  @JsonProperty("loadBalancerConfig")
  @NotNull
  private LoadBalancerConfiguration loadBalancerConfiguration;

  @JsonProperty("templates")
  @NotNull
  private List<TemplateConfiguration> templates = Collections.emptyList();

  @JsonProperty("hostname")
  private String hostname;

  @JsonProperty("agentLockTimeoutMs")
  @Min(0)
  private long agentLockTimeoutMs = DEFAULT_AGENT_LOCK_TIMEOUT_MS;

  @JsonProperty("baseUrlTemplate")
  @NotEmpty
  private String baseUrlTemplate = DEFAULT_AGENT_BASE_URL_TEMPLATE;

  @JsonProperty("defaultDateFormat")
  @NotEmpty
  private String defaultDateFormat = DEFAULT_DATE_FORAMT;

  @JsonProperty("testing")
  private TestingConfiguration testingConfiguration;

  @JsonProperty("auth")
  @Valid
  private AuthConfiguration authConfiguration = new AuthConfiguration();

  @JsonProperty("enableCorsFilter")
  private boolean enableCorsFilter = false;

  @JsonProperty("heartbeatIntervalSeconds")
  private int heartbeatIntervalSeconds = 15;

  @JsonProperty("httpClient")
  @NotNull
  @Valid
  private HttpClientConfiguration httpClientConfiguration = new HttpClientConfiguration();

  @JsonProperty("visibleToBaragonService")
  private boolean visibleToBaragonService = true;

  @JsonProperty("registerOnStartup")
  private boolean registerOnStartup = true;

  @JsonProperty("deregisterOnGracefulShutdown")
  private boolean deregisterOnGracefulShutdown = false;

  @JsonProperty("exitOnStartupError")
  private boolean exitOnStartupError = false;

  @JsonProperty("extraAgentData")
  private Map<String, String> extraAgentData = Collections.emptyMap();

  @JsonProperty("maxNotifyServiceAttempts")
  private int maxNotifyServiceAttempts = 3;

  @JsonProperty("agentCheckInTimeoutMs")
  private long agentCheckInTimeoutMs = 60000;

  @JsonProperty("stateFile")
  private Optional<String> stateFile = Optional.absent();

  @JsonProperty("maxReapplyConfigAttempts")
  private int maxReapplyConfigAttempts = 3;

  @JsonProperty("configCheckIntervalSecs")
  private int configCheckIntervalSecs = 60;

  @JsonProperty("saveFailedConfigs")
  private boolean saveFailedConfigs = false;

  @JsonProperty("graphite")
  private GraphiteConfiguration graphiteConfiguration = new GraphiteConfiguration();

  @JsonProperty("maxGetGloablStateAttempts")
  private int maxGetGloablStateAttempts = 3;

  @JsonProperty("zeroWeightString")
  private String zeroWeightString = "backup";

  @JsonProperty("sameRackMultiplier")
  private int sameRackMultiplier = 2;

  @JsonProperty("weightingFormat")
  private String weightingFormat = "weight=%s";

  @JsonProperty("gcloud")
  private Optional<BaragonAgentGcloudMetadata> gcloudMetadata = Optional.absent();

  @JsonProperty("removeFileOnShutdown")
  private Optional<String> removeFileOnShutdown = Optional.absent();

  public HttpClientConfiguration getHttpClientConfiguration() {
    return httpClientConfiguration;
  }

  public void setHttpClientConfiguration(HttpClientConfiguration httpClientConfiguration) {
    this.httpClientConfiguration = httpClientConfiguration;
  }

  public boolean isEnableCorsFilter() {
    return enableCorsFilter;
  }

  public AuthConfiguration getAuthConfiguration() {
    return authConfiguration;
  }

  public void setAuthConfiguration(AuthConfiguration authConfiguration) {
    this.authConfiguration = authConfiguration;
  }

  public ZooKeeperConfiguration getZooKeeperConfiguration() {
    return zooKeeperConfiguration;
  }

  public void setZooKeeperConfiguration(ZooKeeperConfiguration zooKeeperConfiguration) {
    this.zooKeeperConfiguration = zooKeeperConfiguration;
  }

  public LoadBalancerConfiguration getLoadBalancerConfiguration() {
    return loadBalancerConfiguration;
  }

  public void setLoadBalancerConfiguration(LoadBalancerConfiguration loadBalancerConfiguration) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }

  public List<TemplateConfiguration> getTemplates() {
    return templates;
  }

  public void setTemplates(List<TemplateConfiguration> templates) {
    this.templates = templates;
  }

  public Optional<String> getHostname() {
    return Optional.fromNullable(Strings.emptyToNull(hostname));
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public long getAgentLockTimeoutMs() {
    return agentLockTimeoutMs;
  }

  public void setAgentLockTimeoutMs(long agentLockTimeoutMs) {
    this.agentLockTimeoutMs = agentLockTimeoutMs;
  }

  public TestingConfiguration getTestingConfiguration() {
    return testingConfiguration;
  }

  public void setTestingConfiguration(TestingConfiguration testingConfiguration) {
    this.testingConfiguration = testingConfiguration;
  }

  public String getBaseUrlTemplate() {
    return baseUrlTemplate;
  }

  public void setBaseUrlTemplate(String baseUrlTemplate) {
    this.baseUrlTemplate = baseUrlTemplate;
  }

  public String getDefaultDateFormat() {
    return defaultDateFormat;
  }

  public void setDefaultDateFormat(String defaultDateFormat) {
    this.defaultDateFormat = defaultDateFormat;
  }

  public int getHeartbeatIntervalSeconds() {
    return heartbeatIntervalSeconds;
  }

  public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
    this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
  }

  public boolean isVisibleToBaragonService() {
    return visibleToBaragonService;
  }

  public void setVisibleToBaragonService(boolean visibleToBaragonService) {
    this.visibleToBaragonService = visibleToBaragonService;
  }

  public boolean isRegisterOnStartup() {
    return registerOnStartup;
  }

  public void setRegisterOnStartup(boolean registerOnStartup) {
    this.registerOnStartup = registerOnStartup;
  }

  public boolean isDeregisterOnGracefulShutdown() {
    return deregisterOnGracefulShutdown;
  }

  public void setDeregisterOnGracefulShutdown(boolean deregisterOnGracefulShutdown) {
    this.deregisterOnGracefulShutdown = deregisterOnGracefulShutdown;
  }

  public boolean isExitOnStartupError() {
    return exitOnStartupError;
  }

  public void setExitOnStartupError(boolean exitOnStartupError) {
    this.exitOnStartupError = exitOnStartupError;
  }

  public Map<String, String> getExtraAgentData() {
    return extraAgentData;
  }

  public void setExtraAgentData(Map<String, String> extraAgentData) {
    this.extraAgentData = extraAgentData;
  }

  public int getMaxNotifyServiceAttempts() {
    return maxNotifyServiceAttempts;
  }

  public void setMaxNotifyServiceAttempts(int maxNotifyServiceAttempts) {
    this.maxNotifyServiceAttempts = maxNotifyServiceAttempts;
  }

  public Optional<String> getStateFile() {
    return stateFile;
  }

  public void setStateFile(Optional<String> stateFile) {
    this.stateFile = stateFile;
  }

  public int getMaxReapplyConfigAttempts() {
    return maxReapplyConfigAttempts;
  }

  public void setMaxReapplyConfigAttempts(int maxReapplyConfigAttempts) {
    this.maxReapplyConfigAttempts = maxReapplyConfigAttempts;
  }

  public int getConfigCheckIntervalSecs() {
    return configCheckIntervalSecs;
  }

  public void setConfigCheckIntervalSecs(int configCheckIntervalSecs) {
    this.configCheckIntervalSecs = configCheckIntervalSecs;
  }

  public boolean isSaveFailedConfigs() {
    return saveFailedConfigs;
  }

  public void setSaveFailedConfigs(boolean saveFailedConfigs) {
    this.saveFailedConfigs = saveFailedConfigs;
  }

  public GraphiteConfiguration getGraphiteConfiguration() {
    return graphiteConfiguration;
  }

  public void setGraphiteConfiguration(GraphiteConfiguration graphiteConfiguration) {
    this.graphiteConfiguration = graphiteConfiguration;
  }

  public int getMaxGetGloablStateAttempts() {
    return maxGetGloablStateAttempts;
  }

  public void setMaxGetGloablStateAttempts(int maxGetGloablStateAttempts) {
    this.maxGetGloablStateAttempts = maxGetGloablStateAttempts;
  }

  public String getZeroWeightString() {
    return zeroWeightString;
  }

  public void setZeroWeightString(String zeroWeightString) {
    this.zeroWeightString = zeroWeightString;
  }

  public int getSameRackMultiplier() {
    return sameRackMultiplier;
  }

  public void setSameRackMultiplier(int sameRackMultiplier) {
    this.sameRackMultiplier = sameRackMultiplier;
  }

  public String getWeightingFormat() {
    return weightingFormat;
  }

  public void setWeightingFormat(String weightingFormat) {
    this.weightingFormat = weightingFormat;
  }

  public long getAgentCheckInTimeoutMs() {
    return agentCheckInTimeoutMs;
  }

  public void setAgentCheckInTimeoutMs(long agentCheckInTimeoutMs) {
    this.agentCheckInTimeoutMs = agentCheckInTimeoutMs;
  }

  public Optional<BaragonAgentGcloudMetadata> getGcloudMetadata() {
    return gcloudMetadata;
  }

  public void setGcloudMetadata(Optional<BaragonAgentGcloudMetadata> gcloudMetadata) {
    this.gcloudMetadata = gcloudMetadata;
  }

  public Optional<String> getRemoveFileOnShutdown() {
    return removeFileOnShutdown;
  }

  public void setRemoveFileOnShutdown(Optional<String> removeFileOnShutdown) {
    this.removeFileOnShutdown = removeFileOnShutdown;
  }
}
