package com.hubspot.baragon.lbs;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.models.Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class FilesystemConfigHelper extends LbConfigHelper {
  private static final Log LOG = LogFactory.getLog(FilesystemConfigHelper.class);

  @Inject
  public FilesystemConfigHelper(LbAdapter adapter, LbConfigGenerator configGenerator, LoadBalancerConfiguration loadBalancerConfiguration) {
    super(adapter, configGenerator, loadBalancerConfiguration);
  }
  
  @Override
  protected Collection<LbConfigFile> readConfigs(Service service) {
    Collection<LbConfigFile> files = Lists.newLinkedList();
    
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      try {
        File file = new File(filename);
        final String content = Files.toString(file, Charsets.UTF_8);
        if (file.exists()) {
          files.add(new LbConfigFile(filename, content));
        }
      } catch (Exception e) {
        LOG.error("Failed to back up " + filename, e);
        throw new RuntimeException("Failed backing up " + filename, e);
      }
    }
    
    return files;
  }
  
  @Override
  protected void writeConfigs(Collection<LbConfigFile> files) {
    for (LbConfigFile file : files) {
      try {
        Files.write(file.getContent().getBytes(), new File(file.getFullPath()));
      } catch (IOException e) {
        LOG.error("Failed writing " + file.getFullPath(), e);
        throw new RuntimeException("Failed writing " + file.getFullPath(), e);
      }
    }
  }

  @Override
  public void backupConfigs(Service service) {
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      try {
        File src = new File(filename);
        if (!src.exists()) {
          continue;
        }
        File dest = new File(filename + ".old");
        Files.copy(src, dest);
      } catch (IOException e) {
        LOG.error("Failed to backup " + filename, e);
        throw new RuntimeException("Failed to backup " + filename);
      }
    }
  }

  @Override
  public void restoreConfigs(Service service) {
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      try {
        File src = new File(filename + ".old");
        if (!src.exists()) {
          continue;
        }
        File dest = new File(filename);
        Files.copy(src, dest);
      } catch (IOException e) {
        LOG.error("Failed to restore " + filename, e);
        throw new RuntimeException("Failed to restore " + filename);
      }
    }
  }
}
