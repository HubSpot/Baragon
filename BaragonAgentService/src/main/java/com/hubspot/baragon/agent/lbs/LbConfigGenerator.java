package com.hubspot.baragon.agent.lbs;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.models.LbConfigFile;
import com.hubspot.baragon.models.Service;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.baragon.models.Template;

import java.io.StringWriter;
import java.util.Collection;

@Singleton
public class LbConfigGenerator {
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final Collection<Template> templates;
  
  @Inject
  public LbConfigGenerator(LoadBalancerConfiguration loadBalancerConfiguration,
                           @Named(BaragonAgentServiceModule.AGENT_TEMPLATES) Collection<Template> templates) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.templates = templates;
  }

  public Collection<LbConfigFile> generateConfigsForProject(ServiceContext snapshot) {
    final Collection<LbConfigFile> files = Lists.newArrayListWithCapacity(templates.size());

    for (Template template : templates) {
      final String filename = String.format(template.getFilename(), snapshot.getService().getServiceId());
      final StringWriter sw = new StringWriter();
      template.getTemplate().execute(sw, snapshot);
      final String content = sw.toString();
      files.add(new LbConfigFile(String.format("%s/%s", loadBalancerConfiguration.getRootPath(), filename), content));
    }

    return files;
  }

  public Collection<String> getConfigPathsForProject(Service service) {
    final Collection<String> paths = Lists.newArrayListWithCapacity(templates.size());

    for (Template template : templates) {
      final String filename = String.format(template.getFilename(), service.getServiceId());
      paths.add(String.format("%s/%s", loadBalancerConfiguration.getRootPath(), filename));
    }

    return paths;
  }

}
