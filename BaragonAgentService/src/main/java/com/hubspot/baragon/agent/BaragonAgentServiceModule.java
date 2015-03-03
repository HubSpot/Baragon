package com.hubspot.baragon.agent;

import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.github.jknack.handlebars.Handlebars;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.config.TemplateConfiguration;
import com.hubspot.baragon.agent.config.TestingConfiguration;
import com.hubspot.baragon.agent.handlebars.FirstOfHelper;
import com.hubspot.baragon.agent.handlebars.FormatTimestampHelper;
import com.hubspot.baragon.agent.models.LbConfigTemplate;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.utils.JavaUtils;

public class BaragonAgentServiceModule extends AbstractModule {
  public static final String AGENT_LEADER_LATCH = "baragon.agent.leaderLatch";
  public static final String AGENT_LOCK = "baragon.agent.lock";
  public static final String AGENT_TEMPLATES = "baragon.agent.templates";
  public static final String AGENT_MOST_RECENT_REQUEST_ID = "baragon.agent.mostRecentRequestId";
  public static final String AGENT_LOCK_TIMEOUT_MS = "baragon.agent.lock.timeoutMs";
  public static final String AGENT_INSTANCE_ID = "baragon.agent.instanceid";

  @Override
  protected void configure() {
    install(new BaragonDataModule());
  }

  @Provides
  @Singleton
  public Handlebars providesHandlebars(BaragonAgentConfiguration config) {
    final Handlebars handlebars = new Handlebars();

    handlebars.registerHelper("formatTimestamp", new FormatTimestampHelper(config.getDefaultDateFormat()));
    handlebars.registerHelper("firstOf", new FirstOfHelper(""));

    return handlebars;
  }

  @Provides
  @Singleton
  @Named(AGENT_TEMPLATES)
  public Collection<LbConfigTemplate> providesTemplates(Handlebars handlebars,
                                                BaragonAgentConfiguration configuration) throws Exception {
    Collection<LbConfigTemplate> templates = Lists.newArrayListWithCapacity(configuration.getTemplates().size());

    for (TemplateConfiguration templateConfiguration : configuration.getTemplates()) {
      templates.add(new LbConfigTemplate(templateConfiguration.getFilename(), handlebars.compileInline(templateConfiguration.getTemplate())));
    }

    return templates;
  }
  
  @Provides
  public LoadBalancerConfiguration provideLoadBalancerInfo(BaragonAgentConfiguration configuration) {
    return configuration.getLoadBalancerConfiguration();
  }

  @Provides
  public ZooKeeperConfiguration provideZooKeeperConfiguration(BaragonAgentConfiguration configuration) {
    return configuration.getZooKeeperConfiguration();
  }

  @Provides
  @Named(AGENT_LOCK_TIMEOUT_MS)
  public long provideAgentLockTimeoutMs(BaragonAgentConfiguration configuration) {
    return configuration.getAgentLockTimeoutMs();
  }

  @Provides
  public AuthConfiguration providesAuthConfiguration(BaragonAgentConfiguration configuration) {
    return configuration.getAuthConfiguration();
  }

  @Provides
  @Singleton
  public BaragonAgentMetadata providesAgentMetadata(BaragonAgentConfiguration config) throws Exception {
    final SimpleServerFactory simpleServerFactory = (SimpleServerFactory) config.getServerFactory();
    final HttpConnectorFactory httpFactory = (HttpConnectorFactory) simpleServerFactory.getConnector();

    final int httpPort = httpFactory.getPort();
    final String hostname = config.getHostname().or(JavaUtils.getHostAddress());
    final Optional<String> domain = config.getLoadBalancerConfiguration().getDomain();
    final String appRoot = simpleServerFactory.getApplicationContextPath();

    final String baseAgentUri = String.format(config.getBaseUrlTemplate(), hostname, httpPort, appRoot);
    final String agentId = String.format("%s:%s", hostname, httpPort);

    return new BaragonAgentMetadata(baseAgentUri, agentId, domain, config.getElbConfiguration());
  }

  @Provides
  @Singleton
  @Named(AGENT_LEADER_LATCH)
  public LeaderLatch providesAgentLeaderLatch(BaragonLoadBalancerDatastore loadBalancerDatastore,
                                              BaragonAgentConfiguration config,
                                              BaragonAgentMetadata baragonAgentMetadata) {
    return loadBalancerDatastore.createLeaderLatch(config.getLoadBalancerConfiguration().getName(), baragonAgentMetadata);
  }

  @Provides
  @Singleton
  public Optional<TestingConfiguration> providesTestingConfiguration(BaragonAgentConfiguration configuration) {
    return Optional.fromNullable(configuration.getTestingConfiguration());
  }

  @Provides
  @Singleton
  @Named(AGENT_LOCK)
  public Lock providesAgentLock() {
    return new ReentrantLock();
  }

  @Provides
  @Singleton
  @Named(AGENT_MOST_RECENT_REQUEST_ID)
  public AtomicReference<String> providesMostRecentRequestId() {
    return new AtomicReference<>();
  }

  private String getInstanceId() {
    try {
      String instanceId = null;
      String inputLine;
      URL ec2MetaData = new URL("http://169.254.169.254/latest/meta-data/instance-id");
      URLConnection ec2Conn = ec2MetaData.openConnection();
      BufferedReader in = new BufferedReader(new InputStreamReader(ec2Conn.getInputStream()));
      while ((inputLine = in.readLine()) != null) {
        instanceId = inputLine;
      }
      in.close();
      return instanceId;
    } catch (IOException e) {
      return "";
    }
  }
}
