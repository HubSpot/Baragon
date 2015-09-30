package com.hubspot.baragon.watcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.ringleader.watcher.PersistentWatcher;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;


public class DefaultBaragonStateFetcher implements BaragonStateFetcher {
  private final AtomicReference<CuratorFramework> curatorReference;
  private final ObjectMapper mapper;

  @Inject
  public DefaultBaragonStateFetcher(@Baragon PersistentWatcher watcher, ObjectMapper mapper) {
    this.curatorReference = watcher.getCuratorReference();
    this.mapper = mapper;
  }

  @Override
  public Collection<BaragonServiceState> fetchState(int version) {
    try {
      syncStateNode();
      byte[] data = curatorReference.get().getData().forPath("/state");
      return mapper.readValue(data, new TypeReference<List<BaragonServiceState>>() {});
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private void syncStateNode() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    curatorReference.get().sync().inBackground(new BackgroundCallback() {

      @Override
      public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
        latch.countDown();
      }
    }).forPath("/state");

    latch.await();
  }
}
