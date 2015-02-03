package com.hubspot.baragon.auth;

import io.dropwizard.lifecycle.Managed;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.data.BaragonAuthDatastore;
import com.hubspot.baragon.models.BaragonAuthKey;

public class BaragonAuthUpdater implements Managed, PathChildrenCacheListener {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonAuthUpdater.class);

  private final BaragonAuthDatastore datastore;
  private final PathChildrenCache pathChildrenCache;
  private final AtomicReference<Map<String, BaragonAuthKey>> authKeys;

  @Inject
  public BaragonAuthUpdater(BaragonAuthDatastore datastore,
                            @Named(BaragonDataModule.BARAGON_AUTH_PATH_CACHE) PathChildrenCache pathChildrenCache,
                            @Named(BaragonDataModule.BARAGON_AUTH_KEY_MAP) AtomicReference<Map<String, BaragonAuthKey>> authKeys) {
    this.datastore = datastore;
    this.pathChildrenCache = pathChildrenCache;
    this.authKeys = authKeys;
  }


  @Override
  public void start() throws Exception {
    pathChildrenCache.start(StartMode.BUILD_INITIAL_CACHE);

    pathChildrenCache.getListenable().addListener(this);
  }

  @Override
  public void stop() throws Exception {
    Closeables.close(pathChildrenCache, true);
  }

  @Override
  public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
    switch (event.getType()) {
      case CHILD_UPDATED:
      case CHILD_ADDED:
      case CHILD_REMOVED:
        LOG.info("Auth keys changed, updating map...");
        authKeys.set(datastore.getAuthKeyMap());
    }
  }
}
