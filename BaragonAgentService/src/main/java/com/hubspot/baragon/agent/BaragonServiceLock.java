package com.hubspot.baragon.agent;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BaragonServiceLock {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonServiceLock.class);

  private final ConcurrentHashMap<String, ReentrantLock> serviceLocks;

  @Inject
  public BaragonServiceLock() {
    this.serviceLocks = new ConcurrentHashMap<>();
  }

  public void runWithServiceLock(Runnable function, String serviceId, String name) {
    long startWaiting = System.currentTimeMillis();
    ReentrantLock lock = serviceLocks.computeIfAbsent(serviceId, (r) -> new ReentrantLock());
    lock.lock();
    long start = System.currentTimeMillis();
    LOG.debug("({}) Acquired lock on {} after {}ms", name, serviceId, start - startWaiting);
    try {
      function.run();
    } finally {
      lock.unlock();
      LOG.debug("({}) Unlocked {} after {}ms", name, serviceId, System.currentTimeMillis() - start);
    }
  }

  public <T> T runWithServiceLockAndReturn(Callable<T> function, String serviceId, String name) {
    long startWaiting = System.currentTimeMillis();
    ReentrantLock lock = serviceLocks.computeIfAbsent(serviceId, (r) -> new ReentrantLock());
    lock.lock();
    long start = System.currentTimeMillis();
    LOG.debug("({}) Acquired lock on {} after {}ms", name, serviceId, start - startWaiting);
    try {
      return function.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      lock.unlock();
      LOG.debug("({}) Unlocked {} after {}ms", name, serviceId, System.currentTimeMillis() - start);
    }
  }

  public <T> T runWithServiceLocksAndReturn(Callable<T> function, Set<String> serviceIds, String name) {
    long startWaiting = System.currentTimeMillis();
    ReentrantLock[] held = new ReentrantLock[serviceIds.size()];
    int i = 0;
    for (String serviceId : serviceIds) {
      held[i] = serviceLocks.computeIfAbsent(serviceId, (r) -> new ReentrantLock());
      i++;
    }
    long start = System.currentTimeMillis();
    LOG.debug("({}) Acquired lock on {} after {}ms", name, serviceIds, start - startWaiting);
    try {
      return function.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      for (ReentrantLock lock : held) {
        try {
          lock.unlock();
        } catch (Throwable t) {
          LOG.error("({}) Could not unlock", name, t);
        }
      }
      LOG.debug("({}) Unlocked {} after {}ms", name, serviceIds, System.currentTimeMillis() - start);
    }
  }
}
