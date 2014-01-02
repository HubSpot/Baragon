package com.hubspot.baragon.lbs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonBaseModule;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.models.ServiceInfo;
import com.hubspot.baragon.models.ServiceSnapshot;
import com.hubspot.baragon.models.Template;

import java.io.StringWriter;
import java.util.Collection;

@Singleton
public class LbConfigGenerator {
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final Collection<Template> templates;
  
  @Inject
  public LbConfigGenerator(LoadBalancerConfiguration loadBalancerConfiguration,
                           @Named(BaragonBaseModule.AGENT_TEMPLATES) Collection<Template> templates) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.templates = templates;
  }

  public Collection<LbConfigFile> generateConfigsForProject(ServiceSnapshot snapshot) {
    if (snapshot.getHealthyUpstreams().isEmpty()) {
      return ImmutableList.of();  // nginx doesnt take kindly to zero upstreams.
    }

    final Collection<LbConfigFile> files = Lists.newArrayListWithCapacity(templates.size());

    for (Template template : templates) {
      final String filename = String.format(template.getFilename(), snapshot.getServiceInfo().getName());
      final StringWriter content = new StringWriter();
      template.getTemplate().execute(content, snapshot);
      files.add(new LbConfigFile(String.format("%s/%s", loadBalancerConfiguration.getRootPath(), filename), content.toString()));
    }

    return files;
  }

  public Collection<String> getConfigPathsForProject(ServiceInfo serviceInfo) {
    final Collection<String> paths = Lists.newArrayListWithCapacity(templates.size());

    for (Template template : templates) {
      final String filename = String.format(template.getFilename(), serviceInfo.getName());
      paths.add(String.format("%s/%s", loadBalancerConfiguration.getRootPath(), filename));
    }

    return paths;
  }

}
