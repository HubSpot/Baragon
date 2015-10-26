package com.hubspot.baragon.migrations;


import java.util.Collection;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.UpstreamInfo;

public class UpstreamsMigration extends ZkDataMigration {

  private final BaragonStateDatastore baragonStateDatastore;
  private final CuratorFramework curatorFramework;

  @Inject
  public UpstreamsMigration(BaragonStateDatastore baragonStateDatastore,
                            CuratorFramework curatorFramework) {
    super(1);
    this.baragonStateDatastore = baragonStateDatastore;
    this.curatorFramework = curatorFramework;
  }

  @Override
  public void applyMigration() {
    try {
      for (String service : baragonStateDatastore.getServices()) {
        Collection<String> upstreams = curatorFramework.getChildren().forPath(ZKPaths.makePath(BaragonStateDatastore.SERVICES_FORMAT, service));
        for (String upstream : upstreams) {
          Optional<UpstreamInfo> maybeUpstream = baragonStateDatastore.getUpstreamInfo(service, upstream);

          UpstreamInfo mergedInfo;
          if (maybeUpstream.isPresent()) {
            UpstreamInfo fromPath = UpstreamInfo.fromString(upstream);
            mergedInfo = new UpstreamInfo(fromPath.getUpstream(), maybeUpstream.get().getRequestId().or(fromPath.getRequestId()), maybeUpstream.get().getRackId().or(fromPath.getRackId()));
          } else {
            mergedInfo = UpstreamInfo.fromString(upstream);
          }

          curatorFramework.inTransaction()
              .delete().forPath(String.format(BaragonStateDatastore.UPSTREAM_FORMAT, service, upstream)).and()
              .create().forPath(String.format(BaragonStateDatastore.UPSTREAM_FORMAT, service, mergedInfo.toPath())).and()
              .commit();
        }
      }
    } catch (Exception e) {
      Throwables.propagate(e);
    }
  }
}
