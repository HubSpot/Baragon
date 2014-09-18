package com.hubspot.baragon.agent.lbs;

import java.io.StringWriter;
import java.util.Collection;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.models.LbConfigTemplate;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.ServiceContext;

@Singleton
public class LbConfigGenerator {
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final Collection<LbConfigTemplate> templates;
  
  @Inject
  public LbConfigGenerator(LoadBalancerConfiguration loadBalancerConfiguration,
                           @Named(BaragonAgentServiceModule.AGENT_TEMPLATES) Collection<LbConfigTemplate> templates) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.templates = templates;
  }

  public Collection<BaragonConfigFile> generateConfigsForProject(ServiceContext snapshot) {
    final Collection<BaragonConfigFile> files = Lists.newArrayListWithCapacity(templates.size());

    for (LbConfigTemplate template : templates) {
      final String filename = String.format(template.getFilename(), snapshot.getService().getServiceId());

      final StringWriter sw = new StringWriter();
      try {
        template.getTemplate().apply(snapshot, sw);
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }

      files.add(new BaragonConfigFile(String.format("%s/%s", loadBalancerConfiguration.getRootPath(), filename), sw.toString()));
    }

    return files;
  }

  public Collection<String> getConfigPathsForProject(String serviceId) {
    final Collection<String> paths = Lists.newArrayListWithCapacity(templates.size());

    for (LbConfigTemplate template : templates) {
      final String filename = String.format(template.getFilename(), serviceId);
      paths.add(String.format("%s/%s", loadBalancerConfiguration.getRootPath(), filename));
    }

    return paths;
  }

}
