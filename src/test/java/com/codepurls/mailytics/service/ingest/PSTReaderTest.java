package com.codepurls.mailytics.service.ingest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import com.codepurls.mailytics.data.core.Attachment;
import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.core.MailFolder;
import com.codepurls.mailytics.service.ingest.MailReader.MailVisitor;

public class PSTReaderTest extends BaseReaderTest {

  protected MailReader getMailReader() {
    return new PSTReader();
  }

  protected String getReaderResource() {
    return "test-pst/outlook.pst";
  }

  protected MailVisitor getVisitor() {
    return new MailVisitor() {
      public void onNewFolder(MailFolder folder) {
        String parentName = folder.getParent() != null ? folder.getParent().getName() : "none";
        System.out.println("Folder:=> " + folder.getName() + ", Parent:=> " + parentName);
      }

      public void onNewMail(Mail mail) {
        Map<String, String> headers = mail.getHeaders();
        headers.entrySet().forEach((e) -> {
          assertNotNull(e.getKey());
          assertNotNull(e.getValue());
        });
        assertNotNull(mail.getBody());
        List<Attachment> attachments = mail.getAttachments();
        for (Attachment at : attachments) {
          String mediaType = at.getMediaType();
          String name = at.getName();
          int size = at.getSize();
          assertNotNull(mediaType);
          assertNotNull(name);
          assertTrue(size > 0);
          System.out.printf("\t%s\t%s\t%d%n", mediaType, name, size);
        }
      }

      public void onError(Throwable t, MailFolder folder, Mail mail) {
        fail(t.getMessage());
      }
    };
  }

  protected int expectedFolders() {
    return 24;
  }

  protected int expectedMails() {
    return 3;
  }
}
