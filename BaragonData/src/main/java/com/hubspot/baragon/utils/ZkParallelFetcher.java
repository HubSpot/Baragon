package com.hubspot.baragon.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Function;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.utils.ZKPaths;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;

public class ZkParallelFetcher {
  private static final Logger LOG = Logger.getLogger(ZkParallelFetcher.class);
  private static final int TIMEOUT_SECONDS = 10;

  private final CuratorFramework curatorFramework;

  @Inject
  public ZkParallelFetcher(CuratorFramework framework) {
    this.curatorFramework = framework;
  }

  public <T> Map<String, T> fetchDataInParallel(Collection<String> paths, Function<byte[], T> transformFunction) throws Exception {
    Map<String, T> dataMap = new ConcurrentHashMap<>();
    CountDownLatch countDownLatch = new CountDownLatch(paths.size());
    Queue<KeeperException> exceptions = new ConcurrentLinkedQueue<>();
    BackgroundCallback callback = new GetDataCallback<>(dataMap, transformFunction, countDownLatch, exceptions);

    for (String path : paths) {
      curatorFramework.getData().inBackground(callback).forPath(path);
    }

    waitAndThrowExceptions(countDownLatch, exceptions);
    return dataMap;
  }

  public Map<String, Collection<String>> fetchChildrenInParallel(Collection<String> paths) throws Exception {
    // Didn't use Guava Multimap because we need thread-safety
    Map<String, Collection<String>> childMap = new ConcurrentHashMap<>();
    CountDownLatch countDownLatch = new CountDownLatch(paths.size());
    Queue<KeeperException> exceptions = new ConcurrentLinkedQueue<>();
    BackgroundCallback callback = new GetChildrenCallback(childMap, countDownLatch, exceptions);

    for (String path : paths) {
      curatorFramework.getChildren().inBackground(callback).forPath(path);
    }

    waitAndThrowExceptions(countDownLatch, exceptions);
    return childMap;
  }

  private void waitAndThrowExceptions(CountDownLatch countDownLatch, Queue<KeeperException> exceptions) throws Exception {
    if (!countDownLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
      throw new TimeoutException("ZkChildrenFetcher timed out waiting for data");
    }

    for (KeeperException exception : exceptions) {
      LOG.error(exception);
    }

    if (!exceptions.isEmpty()) {
      throw exceptions.peek();
    }
  }

  private static class GetDataCallback<T> implements BackgroundCallback {
    private final Map<String, T> dataMap;
    private final Function<byte[], T> transformFunction;
    private final CountDownLatch countDownLatch;
    private final Queue<KeeperException> exceptions;

    private GetDataCallback(Map<String, T> dataMap,
                            Function<byte[], T> transformFunction,
                            CountDownLatch countDownLatch,
                            Queue<KeeperException> exceptions) {
      this.dataMap = dataMap;
      this.transformFunction = transformFunction;
      this.countDownLatch = countDownLatch;
      this.exceptions = exceptions;
    }

    @Override
    public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
      try {
        KeeperException.Code code = KeeperException.Code.get(event.getResultCode());

        switch (code) {
          case OK:
            T data = event.getData() == null ? null : transformFunction.apply(event.getData());
            dataMap.put(ZKPaths.getNodeFromPath(event.getPath()), data);
            break;
          case NONODE:
            // In this case there was a race condition in which the child node was deleted before we asked for data.
            break;
          default:
            exceptions.add(KeeperException.create(code, event.getPath()));
        }
      } finally {
        countDownLatch.countDown();
      }
    }
  }

  private static class GetChildrenCallback implements BackgroundCallback {
    private final Map<String, Collection<String>> childMap;
    private final CountDownLatch countDownLatch;
    private final Queue<KeeperException> exceptions;

    private GetChildrenCallback(Map<String, Collection<String>> childMap,
                                CountDownLatch countDownLatch,
                                Queue<KeeperException> exceptions) {
      this.childMap = childMap;
      this.countDownLatch = countDownLatch;
      this.exceptions = exceptions;
    }

    @Override
    public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
      try {
        KeeperException.Code code = KeeperException.Code.get(event.getResultCode());

        switch (code) {
          case OK:
            childMap.put(ZKPaths.getNodeFromPath(event.getPath()), new HashSet<>(event.getChildren()));
            break;
          case NONODE:
            // In this case there was a race condition in which the child node was deleted before we asked for data.
            break;
          default:
            exceptions.add(KeeperException.create(code, event.getPath()));
        }
      } finally {
        countDownLatch.countDown();
      }
    }
  }
}
