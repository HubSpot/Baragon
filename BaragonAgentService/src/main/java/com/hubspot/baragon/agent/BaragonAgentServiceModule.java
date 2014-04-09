package com.hubspot.baragon.agent;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonBaseModule;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.config.TemplateConfiguration;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.lbs.LbAdapter;
import com.hubspot.baragon.lbs.LbConfigHelper;
import com.hubspot.baragon.lbs.LocalLbAdapter;
import com.hubspot.baragon.models.Template;
import com.hubspot.baragon.utils.JavaUtils;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import java.io.StringReader;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BaragonAgentServiceModule extends AbstractModule {
  public static final String HTTP_PORT_PROPERTY = "baragon.agent.http.port";
  public static final String HOSTNAME_PROPERTY = "baragon.agent.hostname";
  public static final String AGENT_LEADER_LATCH = "baragon.agent.leaderLatch";
  public static final String AGENT_LOCK = "baragon.agent.lock";

  @Override
  protected void configure() {
    install(new BaragonBaseModule());
    
    // the baragon agent service works on the local filesystem (as opposed to, say, via ssh)
    bind(LbConfigHelper.class).to(FilesystemConfigHelper.class).in(Scopes.SINGLETON);
    bind(LbAdapter.class).to(LocalLbAdapter.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  public MustacheFactory providesMustacheFactory() {
    return new DefaultMustacheFactory();
  }

  @Provides
  @Singleton
  @Named(BaragonBaseModule.AGENT_TEMPLATES)
  public Collection<Template> providesTemplates(MustacheFactory mustacheFactory,
                                                BaragonAgentConfiguration configuration) throws Exception {
    Collection<Template> templates = Lists.newArrayListWithCapacity(configuration.getTemplates().size());

    for (TemplateConfiguration templateConfiguration : configuration.getTemplates()) {
      final StringReader reader = new StringReader(templateConfiguration.getTemplate());
      templates.add(new Template(templateConfiguration.getFilename(), mustacheFactory.compile(reader, templateConfiguration.getFilename())));
    }

    return templates;
  }
  
  @Provides
  @Singleton
  public LoadBalancerConfiguration provideLoadBalancerInfo(BaragonAgentConfiguration configuration) {
    return configuration.getLoadBalancerConfiguration();
  }

  @Provides
  public ZooKeeperConfiguration provideZooKeeperConfiguration(BaragonAgentConfiguration configuration) {
    return configuration.getZooKeeperConfiguration();
  }

  @Provides
  @Singleton
  @Named(HTTP_PORT_PROPERTY)
  public int providesHttpPortProperty(BaragonAgentConfiguration config) {
    SimpleServerFactory simpleServerFactory = (SimpleServerFactory) config.getServerFactory();
    HttpConnectorFactory httpFactory = (HttpConnectorFactory) simpleServerFactory.getConnector();

    return httpFactory.getPort();
  }

  @Provides
  @Named(HOSTNAME_PROPERTY)
  public String providesHostnameProperty(BaragonAgentConfiguration config) throws Exception {
    return !Strings.isNullOrEmpty(config.getHostname()) ? config.getHostname() : JavaUtils.getHostAddress();
  }

  @Provides
  @Singleton
  @Named(AGENT_LEADER_LATCH)
  public LeaderLatch providesAgentLeaderLatch(CuratorFramework curatorFramework, BaragonAgentConfiguration config,
                                              @Named(HTTP_PORT_PROPERTY) int httpPort, @Named(HOSTNAME_PROPERTY) String hostname) {
    final String appRoot = ((SimpleServerFactory)config.getServerFactory()).getApplicationContextPath();

    return new LeaderLatch(curatorFramework, String.format("/load-balancer/%s", config.getLoadBalancerConfiguration().getName()), String.format("http://%s:%s%s", hostname, httpPort, appRoot));
  }

  @Provides
  @Singleton
  @Named(AGENT_LOCK)
  public Lock providesAgentLock() {
    return new ReentrantLock();
  }
}
