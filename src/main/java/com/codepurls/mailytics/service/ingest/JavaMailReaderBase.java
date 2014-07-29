package com.codepurls.mailytics.service.ingest;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.core.MailFolder;

public abstract class JavaMailReaderBase<F extends MailFolder> implements MailReader {
  public static final Logger LOG = LoggerFactory.getLogger("JavaMailReader");
  private Store              store;

  public void close() throws IOException {
    try {
      store.close();
    } catch (MessagingException e) {
      throw new IOException(e);
    }
  }

  public void visit(MailReaderContext context, File file, MailVisitor visitor) {
    visit(context, file.getAbsolutePath(), visitor);
  }

  @Override
  public void visit(MailReaderContext context, String uri, MailVisitor visitor) throws MailReaderException {
    Session session = createSession();
    try {
      store = getStore(uri, session);
      store.connect();
      List<Folder> folders = getDefaultFolders(store);
      for (Folder folder : folders) {
        walk(null, folder, visitor);
      }
    } catch (Exception e) {
      throw new MailReaderException(format("Error reading file: %s", uri), e);
    }
  }

  protected List<Folder> getDefaultFolders(Store store) throws MessagingException {
    return Arrays.asList(store.getDefaultFolder());
  }

  protected Session createSession() {
    return Session.getDefaultInstance(getProperties());
  }

  private void walk(F parent, Folder folder, MailVisitor visitor) {
    try {
      folder.open(Folder.READ_ONLY);
      F mboxFolder = newFolder(folder, parent);
      visitor.onNewFolder(mboxFolder);
      if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
          try {
            Message message = folder.getMessage(i);
            visitor.onNewMail(newMail(mboxFolder, message));
          } catch (OutOfMemoryError oom) {
            LOG.error("OutOfMemory parsing message #{} in folder {}, will skip.", i, mboxFolder.getName());
            continue;
          } catch (IndexOutOfBoundsException e) {
            break;
          } catch (MessagingException me) {
            LOG.error("Messaging exception while reading folder {}", mboxFolder.getName(), me);
            break;
          }
        }
      }
      if ((folder.getType() & Folder.HOLDS_FOLDERS) != 0) {
        Arrays.stream(folder.list()).filter(f -> !f.getName().startsWith(".")).forEach((f) -> walk(newFolder(f, parent), f, visitor));
      }
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }


  protected abstract Store getStore(String uri, Session session) throws NoSuchProviderException;

  protected abstract Mail newMail(F mboxFolder, Message message);

  protected abstract F newFolder(Folder folder, F parent);

  protected abstract Properties getProperties();

}
