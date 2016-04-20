package com.hubspot.baragon.agent.lbs;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.models.LbConfigTemplate;
import com.hubspot.baragon.exceptions.MissingTemplateException;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.ServiceContext;
import com.github.jknack.handlebars.Context;

@Singleton
public class LbConfigGenerator {
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final Map<String, List<LbConfigTemplate>> templates;
  private final BaragonAgentMetadata agentMetadata;

  @Inject
  public LbConfigGenerator(LoadBalancerConfiguration loadBalancerConfiguration,
                           BaragonAgentMetadata agentMetadata,
                           @Named(BaragonAgentServiceModule.AGENT_TEMPLATES) Map<String, List<LbConfigTemplate>> templates) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.agentMetadata = agentMetadata;
    this.templates = templates;
  }

  public Collection<BaragonConfigFile> generateConfigsForProject(ServiceContext snapshot) throws MissingTemplateException {
    final Collection<BaragonConfigFile> files = Lists.newArrayList();
    String templateName = snapshot.getService().getTemplateName().or(BaragonAgentServiceModule.DEFAULT_TEMPLATE_NAME);

    List<LbConfigTemplate> matchingTemplates = templates.get(templateName);

    if (templates.get(templateName) != null) {
      for (LbConfigTemplate template : matchingTemplates) {
        final List<String> filenames = getFilenames(template, snapshot.getService());

        final StringWriter sw = new StringWriter();
        final Context context = Context.newBuilder(snapshot).combine("agentProperties", agentMetadata).build();
        try {
          template.getTemplate().apply(context, sw);
        } catch (Exception e) {
          throw Throwables.propagate(e);
        }

        for (String filename : filenames) {
          files.add(new BaragonConfigFile(String.format("%s/%s", loadBalancerConfiguration.getRootPath(), filename), sw.toString()));
        }
      }
    } else {
      throw new MissingTemplateException(String.format("MissingTemplateException : Template %s could not be found", templateName));
    }

    return files;
  }

  public Set<String> getConfigPathsForProject(BaragonService service) {
    final Set<String> paths = new HashSet<>();
    for (Map.Entry<String,List<LbConfigTemplate>> entry : templates.entrySet()) {
      for (LbConfigTemplate template : entry.getValue()) {
        final List<String> filenames = getFilenames(template, service);
        for (String filename : filenames) {
          paths.add(String.format("%s/%s", loadBalancerConfiguration.getRootPath(), filename));
        }
      }
    }
    return paths;
  }

  private List<String> getFilenames(LbConfigTemplate template, BaragonService service) {
    switch (template.getFormatType()) {
      case NONE:
        return Collections.singletonList(template.getFilename());
      case SERVICE:
        return Collections.singletonList(String.format(template.getFilename(), service.getServiceId()));
      case DOMAIN_SERVICE:
      default:
        List<String> filenames = new ArrayList<>();
        if (!service.getDomains().isEmpty() && (!loadBalancerConfiguration.getDomains().isEmpty() || loadBalancerConfiguration.getDefaultDomain().isPresent())) {
          for (String domain : service.getDomains()) {
            if (isDomainServed(domain)) {
              filenames.add(String.format(template.getFilename(), domain, service.getServiceId()));
            }
          }
          if (filenames.isEmpty()) {
            if (loadBalancerConfiguration.getDefaultDomain().isPresent()) {
              filenames.add(String.format(template.getFilename(), loadBalancerConfiguration.getDefaultDomain().get(), service.getServiceId()));
            } else {
              throw new IllegalStateException("No domain served for template file that requires domain");
            }
          }
        } else if (loadBalancerConfiguration.getDefaultDomain().isPresent()){
          filenames.add(String.format(template.getFilename(), loadBalancerConfiguration.getDefaultDomain().get(), service.getServiceId()));
        } else {
          throw new IllegalStateException("No domain present for template file that requires domain");
        }
        return filenames;
    }
  }

  private boolean isDomainServed(String domain) {
    return loadBalancerConfiguration.getDomains().contains(domain) || (loadBalancerConfiguration.getDefaultDomain().isPresent() && domain.equals(loadBalancerConfiguration.getDefaultDomain().get()));
  }

}
