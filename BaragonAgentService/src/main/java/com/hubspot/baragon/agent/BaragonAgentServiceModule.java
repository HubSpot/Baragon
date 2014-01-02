package com.hubspot.baragon.agent;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonBaseModule;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.config.TemplateConfiguration;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.lbs.LbAdapter;
import com.hubspot.baragon.lbs.LbConfigHelper;
import com.hubspot.baragon.lbs.LocalLbAdapter;
import com.hubspot.baragon.models.Template;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;

import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class BaragonAgentServiceModule extends AbstractModule {
  private static final Log LOG = LogFactory.getLog(BaragonAgentServiceModule.class);

  public static final String LB_CLUSTER_LOCK = "baragon.cluster.lock";
  public static final String UPSTREAM_POLL_INTERVAL_PROPERTY = "baragon.upstream.poll.interval";
  public static final String POLLER_LAST_RUN = "baragon.poller.lastRun";

  @Override
  protected void configure() {
    install(new BaragonBaseModule());
    
    // the baragon agent service works on the local filesystem (as opposed to, say, via ssh)
    bind(LbConfigHelper.class).to(FilesystemConfigHelper.class).in(Scopes.SINGLETON);
    bind(LbAdapter.class).to(LocalLbAdapter.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Named(UPSTREAM_POLL_INTERVAL_PROPERTY)
  public int providesUpstreamPollInterval(BaragonAgentConfiguration configuration) {
    return configuration.getUpstreamPollIntervalMs();
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
  public LeaderLatch provideLeaderLatch(CuratorFramework curator, LoadBalancerConfiguration loadBalancerConfiguration, Configuration config, Environment environment) throws UnknownHostException {
    String path = String.format("/agent-leader/%s", loadBalancerConfiguration.getName());

    int port = -1;
    for (Connector connector : config.getServerFactory().build(environment).getConnectors()) {
      if (connector instanceof ServerConnector) {
        port = ((ServerConnector) connector).getPort();
      }
    }

    if (port == -1) {
      throw new RuntimeException("Couldn't deduce HTTP port!");
    }

    String participantId = String.format("%s:%d", InetAddress.getLocalHost().getHostName(), port);
    LOG.info("Creating LeaderLatch at " + path + " as " + participantId);
    return new LeaderLatch(curator, path, participantId);
  }

  @Provides
  @Singleton
  @Named(LB_CLUSTER_LOCK)
  public ReentrantLock providesLbClusterLock() {
    return new ReentrantLock();
  }

  @Provides
  @Singleton
  public ScheduledExecutorService providesScheduledExecutorService() {
    return Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("BaragonAgentScheduler-%d").build());
  }

  @Provides
  @Singleton
  @Named(POLLER_LAST_RUN)
  public AtomicLong providesPollerLastRun() {
    return new AtomicLong();
  }
}
