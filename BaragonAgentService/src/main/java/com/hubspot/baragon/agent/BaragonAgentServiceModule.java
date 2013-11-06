package com.hubspot.baragon.agent;

import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.*;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.lbs.*;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hubspot.baragon.BaragonBaseModule;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;

public class BaragonAgentServiceModule extends AbstractModule {
  private static final Log LOG = LogFactory.getLog(BaragonAgentServiceModule.class);

  public static final String LB_CLUSTER_LOCK = "baragon.cluster.lock";

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
  @Named(BaragonBaseModule.LB_PROXY_TEMPLATE)
  public Mustache providesProxyTemplate(MustacheFactory mustacheFactory, LoadBalancerConfiguration configuration) throws Exception {
    return mustacheFactory.compile(new StringReader(configuration.getProxyTemplate()), "proxy-template");
  }

  @Provides
  @Singleton
  @Named(BaragonBaseModule.LB_UPSTREAM_TEMPLATE)
  public Mustache providesUpstreamTemplate(MustacheFactory mustacheFactory, LoadBalancerConfiguration configuration) throws Exception {
    return mustacheFactory.compile(new StringReader(configuration.getUpstreamTemplate()), "upstream-template");
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
    return Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("BaragonUpstreamPoller-%d").build());
  }
}
