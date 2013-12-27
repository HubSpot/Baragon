package com.hubspot.baragon.utils;

import com.google.common.base.Throwables;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class LockUtils {
  private LockUtils() { }

  public static void tryLock(Lock lock, long timeout, TimeUnit timeUnit) {
    try {
      if (!lock.tryLock(timeout, timeUnit)) {
        throw new RuntimeException("Failed to acquire lock");
      }
    } catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }
}
