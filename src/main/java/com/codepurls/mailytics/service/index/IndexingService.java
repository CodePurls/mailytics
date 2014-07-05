package com.codepurls.mailytics.service.index;

import static com.codepurls.mailytics.utils.StringUtils.orEmpty;
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
import java.util.concurrent.locks.LockSupport;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.config.Config.IndexConfig;
import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.core.MailFolder;
import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.service.ingest.MailReader.MailVisitor;
import com.codepurls.mailytics.service.ingest.MailReaderContext;
import com.codepurls.mailytics.service.security.UserService;
import com.codepurls.mailytics.utils.Tuple;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;

public class IndexingService implements Managed {
  private static final Logger                       LOG         = LoggerFactory.getLogger("IndexingService");
  private final IndexConfig                         index;
  private final UserService                         userService;
  private final ExecutorService                     indexerPool;
  private final BlockingQueue<Tuple<Mailbox, Mail>> mailQueue;
  private final BlockingQueue<Mailbox>              mboxQueue;
  private final AtomicBoolean                       keepRunning = new AtomicBoolean(true);
  private final Map<Mailbox, IndexWriter>           userIndices;
  private final Version                             version     = Version.LUCENE_4_9;
  private final Analyzer                            analyzer;
  private final Thread                              mailboxVisitor;
  private final Object                              LOCK        = new Object();
  private static final ThreadLocal<Document>        TL_DOC      = ThreadLocal.withInitial(() -> createDocument());

  public enum MailSchema {
    id {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getMessageId()));
        }
      }
    },
    folder {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getFolder().getName()));
        }
      }
    },
    date {
      public Field[] getFields() {
        return new Field[] { new LongField(name(), 0, Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((LongField) f).setLongValue(mail.getDate().getTime());
        }
      }
    },
    from {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getFrom()));
        }
      }
    },
    to {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getTo()));
        }
      }
    },
    subject {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getSubject()));
        }
      }
    },
    contents {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getBody()));
        }
      }
    },

    attachment_count {
      public Field[] getFields() {
        return new Field[] { new IntField(name(), 0, Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setIntValue(mail.getAttachments().size());
        }
      }
    };
    public abstract Field[] getFields();

    public abstract void setFieldValues(Document doc, Mail mail);
  }

  public static Document createDocument() {
    Document doc = new Document();
    for (MailSchema sf : MailSchema.values()) {
      for (IndexableField indexableField : sf.getFields()) {
        doc.add(indexableField);
      }
    }
    return doc;
  }

  public static Document prepareDocument(Mailbox mb, Mail value) {
    Document document = TL_DOC.get();
    for (MailSchema mf : MailSchema.values()) {
      mf.setFieldValues(document, value);
    }
    return document;
  }

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
          IndexWriter writer = getWriterFor(mb);
          writer.addDocument(prepareDocument(mb, tuple.getValue()));
        } catch (InterruptedException e) {
          LOG.warn("Interrupted while polling queue, will break", e);
          break;
        }
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
        }
      }
      LOG.info("Stopping MailboxVisitor");
    }

    private void doVisit() throws InterruptedException {
      Mailbox mb = mboxQueue.take();
      LOG.info("Will index new mailbox: {}", mb.name);
      AtomicInteger mails = new AtomicInteger();
      AtomicInteger folders = new AtomicInteger();
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
          LOG.info("Visiting folder {}", folder.getName());
          folders.incrementAndGet();
        }

        public void onError(Throwable t, MailFolder folder, Mail mail) {
          LOG.error("Error reading mails, mailbox: {}, folder: {}, mail: {}", mb.name, folder.getName(), mail.getHeaders());
        }
      });
      LOG.info("Done visiting mailbox '{}', visited {} folders and {} mails", mb.name, folders.get(), mails.get());
      try {
        IndexWriter writer = getWriterFor(mb);
        LOG.info("Commiting index for mailbox '{}'", mb.name);
        writer.commit();
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
        LOG.info("Closing index for mailbox '{}'", mb.name);
        writer.close(true);
        userIndices.remove(writer);
        LOG.info("Mailbox '{}' indexed", mb.name);
      } catch (IOException e) {
        LOG.error("Error commiting index for mailbox {}", mb.name, e);
      }
    }
  }

  public IndexingService(IndexConfig index, UserService userService) {
    this.index = index;
    this.userService = userService;
    this.indexerPool = Executors.newFixedThreadPool(index.indexerThreads);
    this.mailQueue = new ArrayBlockingQueue<>(index.indexQueueSize);
    this.mboxQueue = new ArrayBlockingQueue<>(32);
    this.mailboxVisitor = new Thread(new MailboxVisitor(), "mb-visitor");
    this.userIndices = new HashMap<>();
    this.analyzer = new StandardAnalyzer(version);
  }

  public IndexWriterConfig getWriterConfig() {
    IndexWriterConfig cfg = new IndexWriterConfig(version, analyzer);
    return cfg;
  }

  public Directory getWriterDir(Mailbox mb) throws IOException {
    String name = mb.name.toLowerCase();
    name = name.replaceAll("\\W+", "_");
    return FSDirectory.open(new File(index.location, mb.user.username.toLowerCase() + File.separatorChar + name));
  }

  protected IndexWriter getWriterFor(Mailbox mb) throws IOException {
    IndexWriter writer = userIndices.get(mb);
    if (writer == null) {
      synchronized (LOCK) {
        writer = userIndices.get(mb);
        if (writer == null) {
          writer = new IndexWriter(getWriterDir(mb), getWriterConfig());
          userIndices.put(mb, writer);
        }
      }
    }
    return writer;
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
    mboxQueue.add(mb);
  }

  public IndexConfig getIndex() {
    return index;
  }

  public UserService getUserService() {
    return userService;
  }
}
