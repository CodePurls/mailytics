package com.codepurls.mailytics.service.ingest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.core.MailFolder;
import com.codepurls.mailytics.service.ingest.MailReader.MailVisitor;

public abstract class BaseReaderTest {

  protected AtomicInteger msgCount;
  protected AtomicInteger folderCount;

  @Test
  public void testReader() throws Exception {
    msgCount = new AtomicInteger();
    folderCount = new AtomicInteger();
    String resource = Thread.currentThread().getContextClassLoader().getResource(getReaderResource()).getFile();
    MailReader reader = getMailReader();
    MailVisitor visitor = getVisitor();
    reader.visit(null, resource, new MailVisitor() {
      public void onNewMail(Mail mail) {
        assertNotNull(mail.getFolder());
        visitor.onNewMail(mail);
        msgCount.incrementAndGet();
      }

      public void onNewFolder(MailFolder folder) {
        if (folder.getParent() != null) {
          assertTrue(folder.getName() != null && folder.getName().length() > 0);
        }
        visitor.onNewFolder(folder);
        folderCount.incrementAndGet();
      }

      public void onError(Throwable t, MailFolder folder, Mail mail) {
        visitor.onError(t, folder, mail);
        t.printStackTrace();
        fail("Unexpected error: " + t.getMessage());
      }
    });
    assertEquals(expectedMails(), msgCount.get());
    assertEquals(expectedFolders(), folderCount.get());

  }

  protected abstract MailReader getMailReader();

  protected abstract String getReaderResource();

  protected abstract MailVisitor getVisitor();

  protected abstract int expectedFolders();

  protected abstract int expectedMails();
}
