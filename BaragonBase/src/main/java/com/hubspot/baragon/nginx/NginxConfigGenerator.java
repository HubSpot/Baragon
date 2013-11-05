package com.hubspot.baragon.nginx;

import java.io.StringWriter;
import java.util.Collection;

import com.github.mustachejava.Mustache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.lbs.LbConfigFile;
import com.hubspot.baragon.lbs.LbConfigGenerator;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.models.ServiceInfo;

public class NginxConfigGenerator implements LbConfigGenerator {

  private final Mustache proxyTemplate;
  private final Mustache upstreamTemplate;
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  
  @Inject
  public NginxConfigGenerator(LoadBalancerConfiguration loadBalancerConfiguration, @Named("nginx-proxy") Mustache proxyTemplate, @Named("nginx-upstreams") Mustache upstreamTemplate) {
    this.proxyTemplate = proxyTemplate;
    this.upstreamTemplate = upstreamTemplate;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }

  @Override
  public Collection<LbConfigFile> generateConfigsForProject(ServiceInfo serviceInfo, Collection<String> upstreams) {
    if (upstreams.size() == 0) {
      return ImmutableList.of();  // nginx doesnt take kindly to zero upstreams.
    }

    final StringWriter proxyContent = new StringWriter();
    final StringWriter upstreamContent = new StringWriter();

    proxyTemplate.execute(proxyContent, serviceInfo);
    upstreamTemplate.execute(upstreamContent, ImmutableMap.of("serviceInfo", serviceInfo, "upstreams", upstreams));

    return ImmutableList.of(
          new LbConfigFile(String.format("%s/proxy/%s.conf", loadBalancerConfiguration.getRootPath(), serviceInfo.getName()), proxyContent.toString()),
          new LbConfigFile(String.format("%s/upstreams/%s.conf", loadBalancerConfiguration.getRootPath(), serviceInfo.getName()), upstreamContent.toString()));
  }

  @Override
  public Collection<String> getConfigPathsForProject(ServiceInfo serviceInfo) {
    return ImmutableList.of(
        String.format("%s/proxy/%s.conf", loadBalancerConfiguration.getRootPath(), serviceInfo.getName()),
        String.format("%s/upstreams/%s.conf", loadBalancerConfiguration.getRootPath(), serviceInfo.getName())
    );
  }

}
