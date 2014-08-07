package com.codepurls.mailytics.service.ingest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import com.codepurls.mailytics.data.core.Attachment;
import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.core.MailFolder;
import com.codepurls.mailytics.service.ingest.MailReader.MailVisitor;

public class MBoxReaderTest extends BaseReaderTest {

  protected MailReader getMailReader() {
    return new MBoxReader();
  }

  protected String getReaderResource() {
    return "test-mbox-folder";
  }

  protected MailVisitor getVisitor() {
    return new MailVisitor() {
      public void onNewMail(Mail mail) {
        assertFalse(mail.getHeaders().isEmpty());
        assertFalse(mail.getBody() == null);
        List<Attachment> list = mail.getAttachments();
        if (msgCount.get() == 3) {
          assertEquals(1, list.size());
          Attachment att = list.get(0);
          assertEquals("application/ics", att.getMediaType());
          assertEquals("invite.ics", att.getName());
          assertEquals(1467, att.getSize());
          assertNotNull(att.getStream());
        } else assertEquals(0, list.size());
      }

      public void onNewFolder(MailFolder folder) {
        if (folderCount.get() == 0) {
          assertTrue(folder.getParent() == null);
        }
//        TODO: fix this test
//        else {
//          assertTrue(folder.getParent() != null);
//        }
      }

      public void onError(Throwable t, MailFolder folder, Mail mail) {
        t.printStackTrace();
        fail(t.getMessage());
      }
    };
  }

  protected int expectedFolders() {
    return 2;
  }

  protected int expectedMails() {
    return 5;
  }
}
