package com.codepurls.mailytics.service.index;

import static java.lang.String.format;
import io.dropwizard.lifecycle.Managed;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoSuchDirectoryException;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.config.Config.IndexConfig;
import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.core.MailFolder;
import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.data.core.Mailbox.MailboxStatus;
import com.codepurls.mailytics.service.ingest.MailReader.MailVisitor;
import com.codepurls.mailytics.service.ingest.MailReaderContext;
import com.codepurls.mailytics.service.security.UserService;
import com.codepurls.mailytics.utils.NamedThreadFactory;
import com.codepurls.mailytics.utils.Tuple;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;

public class IndexingService implements Managed {
  private static final Logger                                LOG         = LoggerFactory.getLogger("IndexingService");
  private final IndexConfig                                  index;
  private final UserService                                  userService;
  private final ExecutorService                              indexerPool;
  private final BlockingQueue<Tuple<Mailbox, Mail>>          mailQueue;
  private final BlockingQueue<Mailbox>                       mboxQueue;
  private final AtomicBoolean                                keepRunning = new AtomicBoolean(true);
  private final ConcurrentHashMap<Mailbox, IndexWriter>      userIndices;
  private final LoadingCache<Mailbox, Optional<IndexReader>> indexReaders;
  private final Version                                      version     = Version.LUCENE_4_9;
  private final Analyzer                                     analyzer;
  private final Thread                                       mailboxVisitor;

  public class IndexWorker implements Callable<AtomicLong> {
    private final AtomicLong counter = new AtomicLong();

    public AtomicLong call() throws Exception {
      try {
        loop();
        LOG.info("IndexWorker stopped, prepared {} docs", counter.get());
      } catch (Exception e) {
        LOG.error("Error indexing mails, ignoring", e);
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
          Mail mail = tuple.getValue();
          if (mail == null) {
            finalizeIndex(mb);
          } else {
            IndexWriter writer = getWriterFor(mb);
            writer.addDocument(MailIndexer.prepareDocument(mb, mail));
          }
        } catch (InterruptedException e) {
          LOG.warn("Interrupted while polling queue, will break", e);
          break;
        }
      }
    }

    private void finalizeIndex(Mailbox mb) {
      LOG.info("Received end of mailbox, will mark as indexed.");
      IndexWriter writer = userIndices.remove(mb);
      if (writer == null) {
        LOG.warn("No index writer found for mb: '{}'", mb.name);
        return;
      }
      try {
        LOG.info("Commiting index for mailbox '{}'", mb.name);
        writer.commit();
        LOG.info("Closing index for mailbox '{}'", mb.name);
        writer.close(true);
        LOG.info("Mailbox '{}' indexed", mb.name);
        mb.closeReader();
        userService.updateMailboxStatus(mb, MailboxStatus.INDEXED);
      } catch (IOException e) {
        LOG.error("Error commiting index for mailbox {}", mb.name, e);
      }
    }
  }

  public class MailboxVisitor implements Runnable {
    public void run() {
      LOG.info("Starting MailboxVisitor");
      while (keepRunning.get()) {
        try {
          doVisit();
        } catch (InterruptedException e) {
          LOG.error("Interrupted wailing for mailboxes will stop", e);
          break;
        } catch (Exception e) {
          LOG.error("Error during mbox retrieval loop, will ignore", e);
        } catch (Throwable e) {
          LOG.error("Error indexing mails", e);
        }

      }
      LOG.info("Stopping MailboxVisitor");
    }

    private void doVisit() throws InterruptedException {
      Mailbox mb = mboxQueue.take();
      String oldName = Thread.currentThread().getName();
      String newName = format("%s-mb-%s", oldName, mb.name);
      Thread.currentThread().setName(newName);
      LOG.info("Will index new mailbox: {}", mb.name);
      AtomicInteger mails = new AtomicInteger();
      AtomicInteger folders = new AtomicInteger();
      userService.updateMailboxStatus(mb, MailboxStatus.INDEXING);
      mb.visit(new MailReaderContext(), new MailVisitor() {
        public void onNewMail(Mail mail) {
          mails.incrementAndGet();
          try {
            mailQueue.put(Tuple.of(mb, mail));
          } catch (InterruptedException e) {
            Thread.interrupted();
            throw new RuntimeException(e);
          }
        }

        public void onNewFolder(MailFolder folder) {
          LOG.info("Visiting '{}/{}'", mb.name, folder.getName());
          folders.incrementAndGet();
        }

        public void onError(Throwable t, MailFolder folder, Mail mail) {
          LOG.error("Error reading mails, mailbox: {}, folder: {}, mail: {}", mb.name, folder.getName(), mail, t);
        }
      });
      LOG.info("Done visiting mailbox '{}', visited {} folders and {} mails", mb.name, folders.get(), mails.get());
      mailQueue.put(Tuple.of(mb, null));
      Thread.currentThread().setName(oldName);
    }
  }

  public IndexingService(IndexConfig index, UserService userService) {
    this.index = index;
    this.userService = userService;
    this.indexerPool = Executors.newFixedThreadPool(index.indexerThreads, new NamedThreadFactory("indexer"));
    this.mailQueue = new ArrayBlockingQueue<>(index.indexQueueSize);
    this.mboxQueue = new ArrayBlockingQueue<>(1);
    this.mailboxVisitor = new Thread(new MailboxVisitor(), "mb-visitor");
    this.userIndices = new ConcurrentHashMap<>();
    this.indexReaders = CacheBuilder.newBuilder().expireAfterAccess(60, TimeUnit.SECONDS)
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .softValues()
        .removalListener((r) -> LOG.info("Unloading index reader for mailbox: {}", ((Mailbox)r.getKey()).name))
        .build(new CacheLoader<Mailbox, Optional<IndexReader>>() {
          public Optional<IndexReader> load(Mailbox mb) throws Exception {
            LOG.info("Loading index reader for mailbox: {}", mb.name);
            return loadIndexReader(mb);
          }
        });
    this.analyzer = new StandardAnalyzer(version);
  }

  protected Optional<IndexReader> loadIndexReader(Mailbox mb) {
    try {
      return Optional.of(DirectoryReader.open(getIndexDir(mb)));
    } catch (NoSuchDirectoryException e) {
      LOG.warn("No index found for mail box [id:{}, name:{}], error: {}", mb.id, mb.name, e.getMessage());
      return Optional.empty();
    } catch (Exception e) {
      LOG.error("Error retrieving dir for mailbox : {}", mb.name, e);
      return Optional.empty();
    }
  }

  public IndexWriterConfig getWriterConfig() {
    return new IndexWriterConfig(version, analyzer);
  }

  public Directory getIndexDir(Mailbox mb) throws IOException {
    return getIndexDir(index, mb);
  }

  public static Directory getIndexDir(IndexConfig config, Mailbox mb) throws IOException {
    return FSDirectory.open(getIndexDirectory(config, mb));
  }

  public static File getIndexDirectory(IndexConfig config, Mailbox mb) {
    String name = mb.name.toLowerCase();
    name = name.replaceAll("\\W+", "_");
    return new File(config.location, mb.user.username.toLowerCase() + File.separatorChar + name);
  }

  protected IndexWriter getWriterFor(Mailbox mb) throws IOException {
    return userIndices.computeIfAbsent(mb, (mbox) -> {
      try {
        return new IndexWriter(getIndexDir(this.index, mbox), getWriterConfig());
      } catch (Exception e) {
        LOG.error("Error opening index writer for mailbox: {}", mbox.name, e);
        return null;
      }
    });
  }

  public void start() throws Exception {
    validateIndexDir(index);
    for (int i = 0; i < index.indexerThreads; i++) {
      indexerPool.submit(new IndexWorker());
    }
    mailboxVisitor.start();
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
    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
    this.mailboxVisitor.interrupt();
    this.mailQueue.offer(null, 1, TimeUnit.SECONDS);
    this.indexerPool.shutdownNow();
    this.userIndices.forEach((k, w) -> {
      try {
        LOG.info("Closing index writer for mailbox {}", k.name);
        w.close(true);
      } catch (Exception e) {
        LOG.error("Error commiting index of {}", k.username, e);
      }
    });
    this.userIndices.clear();
  }

  public void index(Mailbox mb) {
    LOG.info("Scheduling indexing of mailbox {}-{}", mb.id, mb.name);
    try {
      mboxQueue.put(mb);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while queuing mailbox", e);
    }
  }

  public void reindex(Mailbox mb) throws IOException {
    LOG.info("Re-indexing mailbox {}-{}", mb.id, mb.name);
    File[] files = getIndexDirectory(index, mb).listFiles();
    if (files != null) {
      LOG.info("Will delete existing index with {} files", files.length);
      for (File f : files) {
        Files.delete(f.toPath());
      }
    }
    index(mb);
  }

  public IndexConfig getIndex() {
    return index;
  }

  public UserService getUserService() {
    return userService;
  }

  public Version getVersion() {
    return version;
  }

  public Analyzer getAnalyzer() {
    return analyzer;
  }

  public Optional<IndexReader> getOrOpenReader(Mailbox mb) {
    try {
      return this.indexReaders.get(mb);
    } catch (ExecutionException e) {
      LOG.error("Error retrieving index reader for mailbox {}", mb.name, e);
      return Optional.empty();
    }
  }

}
