package com.codepurls.mailytics.utils;

import static java.lang.String.format;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
  private final String        prefix;
  private final AtomicInteger cnt = new AtomicInteger(1);

  public NamedThreadFactory(String prefix) {
    this.prefix = prefix;
  }

  public Thread newThread(Runnable r) {
    return new Thread(r, format("%s-%d", prefix, cnt.get()));
  }

}
