package com.hubspot.baragon.lbs;

import java.io.StringWriter;
import java.util.Collection;

import com.github.mustachejava.Mustache;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonBaseModule;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.models.ServiceInfo;
import com.hubspot.baragon.models.ServiceInfoAndUpstreams;

public class LbConfigGenerator {

  private final Mustache proxyTemplate;
  private final Mustache upstreamTemplate;
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  
  @Inject
  public LbConfigGenerator(LoadBalancerConfiguration loadBalancerConfiguration, @Named(BaragonBaseModule.LB_PROXY_TEMPLATE) Mustache proxyTemplate, @Named(BaragonBaseModule.LB_UPSTREAM_TEMPLATE) Mustache upstreamTemplate) {
    this.proxyTemplate = proxyTemplate;
    this.upstreamTemplate = upstreamTemplate;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }

  public Collection<LbConfigFile> generateConfigsForProject(ServiceInfo serviceInfo, Collection<String> upstreams) {
    if (upstreams.size() == 0) {
      return ImmutableList.of();  // nginx doesnt take kindly to zero upstreams.
    }

    final ServiceInfoAndUpstreams serviceInfoAndUpstreams = new ServiceInfoAndUpstreams(serviceInfo, upstreams);

    final StringWriter proxyContent = new StringWriter();
    final StringWriter upstreamContent = new StringWriter();

    proxyTemplate.execute(proxyContent, serviceInfoAndUpstreams);
    upstreamTemplate.execute(upstreamContent, serviceInfoAndUpstreams);

    return ImmutableList.of(
          new LbConfigFile(String.format("%s/proxy/%s.conf", loadBalancerConfiguration.getRootPath(), serviceInfo.getName()), proxyContent.toString()),
          new LbConfigFile(String.format("%s/upstreams/%s.conf", loadBalancerConfiguration.getRootPath(), serviceInfo.getName()), upstreamContent.toString()));
  }

  public Collection<String> getConfigPathsForProject(ServiceInfo serviceInfo) {
    return ImmutableList.of(
        String.format("%s/proxy/%s.conf", loadBalancerConfiguration.getRootPath(), serviceInfo.getName()),
        String.format("%s/upstreams/%s.conf", loadBalancerConfiguration.getRootPath(), serviceInfo.getName())
    );
  }

}
