package com.hubspot.baragon.agent.lbs;

import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
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

  private static final Pattern FORMAT_PATTERN = Pattern.compile("[^%]%([+-]?\\d*.?\\d*)?[sdf]");

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
        final String filename = getFilename(template.getFilename(), snapshot.getService());

        final StringWriter sw = new StringWriter();
        final Context context = Context.newBuilder(snapshot).combine("agentProperties", agentMetadata).build();
        try {
          template.getTemplate().apply(context, sw);
        } catch (Exception e) {
          throw Throwables.propagate(e);
        }

        files.add(new BaragonConfigFile(String.format("%s/%s", loadBalancerConfiguration.getRootPath(), filename), sw.toString()));
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
        final String filename = String.format(template.getFilename(), service.getServiceId());
        if (!paths.contains(filename)) {
          paths.add(String.format("%s/%s", loadBalancerConfiguration.getRootPath(), filename));
        }
      }
    }
    return paths;
  }

  private String getFilename(String filenameFormat, BaragonService service) {
    Matcher m = FORMAT_PATTERN.matcher(filenameFormat);
    int count = 0;
    while(m.find()) {
      count ++;
    }

    if (count > 1) {
      return String.format(filenameFormat, service.getDomain().or(agentMetadata.getDomain()).or(""), service.getServiceId());
    } else {
      return String.format(filenameFormat, service.getServiceId());
    }
  }

}
