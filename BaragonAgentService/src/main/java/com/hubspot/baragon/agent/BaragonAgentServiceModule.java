package com.hubspot.baragon.agent;

import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
  public static final String BARAGON_AGENT_HTTP_PORT = "baragon.agent.http.port";
  public static final String BARAGON_AGENT_HOSTNAME = "baragon.agent.hostname";
  public static final String BARAGON_AGENT_DOMAIN = "baragon.agent.domain";
  public static final String AGENT_LEADER_LATCH = "baragon.agent.leaderLatch";
  public static final String AGENT_LOCK = "baragon.agent.lock";
  public static final String AGENT_TEMPLATES = "baragon.agent.templates";
  public static final String AGENT_MOST_RECENT_REQUEST_ID = "baragon.agent.mostRecentRequestId";
  public static final String AGENT_LOCK_TIMEOUT_MS = "baragon.agent.lock.timeoutMs";

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
  @Named(BARAGON_AGENT_HTTP_PORT)
  public int providesHttpPortProperty(BaragonAgentConfiguration config) {
    SimpleServerFactory simpleServerFactory = (SimpleServerFactory) config.getServerFactory();
    HttpConnectorFactory httpFactory = (HttpConnectorFactory) simpleServerFactory.getConnector();

    return httpFactory.getPort();
  }

  @Provides
  @Named(BARAGON_AGENT_HOSTNAME)
  public String providesHostnameProperty(BaragonAgentConfiguration config) throws Exception {
    return config.getHostname().or(JavaUtils.getHostAddress());
  }

  @Provides
  @Named(BARAGON_AGENT_DOMAIN)
  public Optional<String> providesAgentDomain(LoadBalancerConfiguration loadBalancerConfiguration) {
    return loadBalancerConfiguration.getDomain();
  }

  @Provides
  @Singleton
  @Named(AGENT_LEADER_LATCH)
  public LeaderLatch providesAgentLeaderLatch(BaragonLoadBalancerDatastore loadBalancerDatastore,
                                              BaragonAgentConfiguration config,
                                              @Named(BARAGON_AGENT_HTTP_PORT) int httpPort,
                                              @Named(BARAGON_AGENT_HOSTNAME) String hostname,
                                              @Named(BARAGON_AGENT_DOMAIN) Optional<String> domain) {
    final String appRoot = ((SimpleServerFactory)config.getServerFactory()).getApplicationContextPath();
    final String baseAgentUri = String.format(config.getBaseUrlTemplate(), hostname, httpPort, appRoot);

    return loadBalancerDatastore.createLeaderLatch(config.getLoadBalancerConfiguration().getName(), new BaragonAgentMetadata(baseAgentUri, domain));
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
}
