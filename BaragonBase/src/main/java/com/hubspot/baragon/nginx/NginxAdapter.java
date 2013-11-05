package com.hubspot.baragon.nginx;

import org.apache.commons.exec.CommandLine;

import com.hubspot.baragon.lbs.LocalLbAdapter;

public class NginxAdapter extends LocalLbAdapter {
  @Override
  protected CommandLine getCheckConfigCommand() {
    return new CommandLine("/usr/sbin/nginx")
      .addArgument("-t");
  }

  @Override
  protected CommandLine getReloadConfigCommand() {
    return new CommandLine("/usr/sbin/nginx")
      .addArgument("-s")
      .addArgument("reload");
  }
}
