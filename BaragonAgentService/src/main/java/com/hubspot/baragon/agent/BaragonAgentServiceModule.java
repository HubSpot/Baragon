package com.hubspot.baragon.agent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.locks.ReentrantLock;

import com.google.inject.*;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.name.Names;
import com.hubspot.baragon.config.BaragonBaseModule;
import com.hubspot.baragon.lbs.LbAdapter;
import com.hubspot.baragon.lbs.LbConfigGenerator;
import com.hubspot.baragon.lbs.LbConfigHelper;
import com.hubspot.baragon.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.nginx.NginxConfigGenerator;
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

    // load balancer config generators
    bind(Key.get(LbConfigGenerator.class, Names.named("nginx"))).to(NginxConfigGenerator.class);
    
    // the baragon agent service works on the local filesystem (as opposed to, say, via ssh)
    bind(LbConfigHelper.class).to(FilesystemConfigHelper.class).in(Scopes.SINGLETON);
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
  public LbConfigGenerator provideLbConfigGenerator(Injector injector, LoadBalancerConfiguration loadBalancerConfiguration) {
    return injector.getInstance(Key.get(LbConfigGenerator.class, Names.named(loadBalancerConfiguration.getType())));
  }
  
  @Provides
  @Singleton
  public LbAdapter provideLbAdapter(Injector injector, LoadBalancerConfiguration loadBalancerConfiguration) {
    return injector.getInstance(Key.get(LbAdapter.class, Names.named(loadBalancerConfiguration.getType())));
  }

  @Provides
  @Singleton
  @Named(LB_CLUSTER_LOCK)
  public ReentrantLock providesLbClusterLock() {
    return new ReentrantLock();
  }
}
