package com.codepurls.mailytics.service.ingest;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import net.fortuna.mstor.data.MboxFile;
import net.fortuna.mstor.data.MboxFile.BufferStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.data.mbox.MBoxFolder;
import com.codepurls.mailytics.data.mbox.MBoxMail;

public class MBoxReader implements MailReader {
  public static final Logger LOG = LoggerFactory.getLogger(MBoxReader.class);
  private Store              store;

  public void visit(MailReaderContext context, String uri, MailVisitor visitor) throws MailReaderException {
    Properties props = new Properties();
    System.setProperty(MboxFile.KEY_BUFFER_STRATEGY, BufferStrategy.MAPPED.name());
    System.setProperty("mstor.cache.disabled", "true");
    System.setProperty("mail.imap.partialfetch", "false");
    System.setProperty("mstor.mbox.metadataStrategy", "none");
    System.setProperty("mstor.mbox.cacheBuffers", "false");
    System.setProperty("mstor.metadata", "disabled");
    System.setProperty("mstor.mbox.parsing.relaxed", "true");
//    System.setProperty("mstor.mbox.encoding", "US-ASCII");
    System.setProperty("mstor.mbox.mozillaCompatibility", "true");
    Session session = Session.getDefaultInstance(props);
    try {
      store = session.getStore(new URLName(format("mstor:%s", uri)));
      store.connect();
      Folder folder = store.getDefaultFolder();
      walk(null, folder, visitor);
    } catch (Exception e) {
      throw new MailReaderException(format("Error reading file: %s", uri), e);
    }
  }

  public void visit(MailReaderContext context, File file, MailVisitor visitor) {
    visit(context, file.getAbsolutePath(), visitor);
  }

  private void walk(MBoxFolder parent, Folder folder, MailVisitor visitor) {
    try {
      folder.open(Folder.READ_ONLY);
      MBoxFolder mboxFolder = new MBoxFolder(folder, parent);
      visitor.onNewFolder(mboxFolder);
      if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
          try {
            Message message = folder.getMessage(i);
            visitor.onNewMail(new MBoxMail(mboxFolder, message));
          } catch (OutOfMemoryError oom) {
            LOG.error("OutOfMemory parsing message #{} in folder {}, will skip.", i, mboxFolder.getName());
            continue;
          } catch (IndexOutOfBoundsException e) {
            break;
          } catch(MessagingException me) {
            LOG.error("Messaging exception while reading folder {}", mboxFolder.getName(), me);
            break;
          }
        }
      }
      if ((folder.getType() & Folder.HOLDS_FOLDERS) != 0) {
        Arrays.stream(folder.list()).filter(f-> !f.getName().startsWith(".")).forEach((f) -> walk(new MBoxFolder(f, parent), f, visitor));
      }
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  public void close() throws IOException {
    try {
      store.close();
    } catch (MessagingException e) {
      throw new IOException(e);
    }
  }
}
