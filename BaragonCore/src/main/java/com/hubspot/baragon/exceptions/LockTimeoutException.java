package com.hubspot.baragon.exceptions;

import java.util.concurrent.locks.ReentrantLock;

public class LockTimeoutException extends Exception {
  private final String lockInfo;

  public LockTimeoutException(String message, ReentrantLock agentLock) {
    super(message);
    this.lockInfo = String.format("LockState: %s, Queue Length: %s, Hold Count: %s", agentLock.toString(), agentLock.getQueueLength(), agentLock.getHoldCount());
  }

  public String getLockInfo() {
    return lockInfo;
  }
}
