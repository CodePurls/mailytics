package com.codepurls.mailytics.service.index;

import io.dropwizard.lifecycle.Managed;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.config.Config.IndexConfig;
import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.service.security.UserService;
import com.codepurls.mailytics.utils.Tuple;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;

public class IndexingService implements Managed {
  private static final Logger                       LOG         = LoggerFactory.getLogger("IndexingService");
  private final IndexConfig                         index;
  private final UserService                         userService;
  private final ExecutorService                     indexerPool;
  private final BlockingQueue<Tuple<Mailbox, Mail>> mailQueue;
  private final AtomicBoolean                       keepRunning = new AtomicBoolean();
  private final Map<Mailbox, IndexWriter>           userIndices;
  private final Version                             version     = Version.LUCENE_4_9;
  private final Analyzer                            analyzer;

  public class IndexWorker implements Callable<AtomicLong> {
    private final AtomicLong counter = new AtomicLong();

    public AtomicLong call() throws Exception {
      try {
        loop();
        LOG.info("IndexWorker stopped, prepared {} docs", counter.get());
      } catch (Exception e) {
        LOG.error("Error indexing mails, quitting", e);
      }
      return counter;
    }

    private void loop() throws IOException {
      while (keepRunning.get()) {
        try {
          Tuple<Mailbox, Mail> tuple = mailQueue.take();
          if (tuple == null) {
            LOG.info("Received term signal, will quit.");
            break;
          }
          Mailbox mb = tuple.getKey();
          IndexWriter writer = userIndices.get(mb);
          if (writer == null) {
            writer = new IndexWriter(getWriterDir(mb), getWriterConfig());
            userIndices.put(mb, writer);
          }
        } catch (InterruptedException e) {
          LOG.warn("Interrupted while polling queue, will break", e);
          break;
        }
      }
    }
  }

  public IndexingService(IndexConfig index, UserService userService) {
    this.index = index;
    this.userService = userService;
    this.indexerPool = Executors.newFixedThreadPool(index.indexerThreads);
    this.mailQueue = new ArrayBlockingQueue<>(index.indexQueueSize);
    this.userIndices = new HashMap<>();
    this.analyzer = new StandardAnalyzer(version);
  }

  public IndexWriterConfig getWriterConfig() {
    IndexWriterConfig cfg = new IndexWriterConfig(version, analyzer);
    return cfg;
  }

  public Directory getWriterDir(Mailbox mb) throws IOException {
    return FSDirectory.open(new File(index.location, mb.user.username + File.separatorChar + mb.email));
  }

  public void start() throws Exception {
    validateIndexDir(index);
    for (int i = 0; i < index.indexerThreads; i++) {
      indexerPool.submit(new IndexWorker());
    }
    indexerPool.shutdown();
  }

  private void validateIndexDir(IndexConfig index) {
    File loc = new File(index.location);
    if (!loc.exists()) {
      LOG.info("Creating non-existitent index directory {}", loc);
      if (!loc.mkdirs()) { throw new RuntimeException("Unable to create index directory:" + loc); }
    }
    if (!loc.canWrite()) { throw new RuntimeException("Index directory " + loc + " is not writable."); }
  }

  public void stop() throws Exception {
    this.keepRunning.set(false);
    this.mailQueue.offer(null, 1, TimeUnit.SECONDS);
    this.indexerPool.shutdownNow();
    this.userIndices.forEach((k, v) -> {
      try {
        v.commit();
        v.close();
      } catch (Exception e) {
        LOG.error("Error commiting index of {}", k.username, e);
      }
    });
    this.userIndices.clear();

  }

  public IndexConfig getIndex() {
    return index;
  }

  public UserService getUserService() {
    return userService;
  }
}
