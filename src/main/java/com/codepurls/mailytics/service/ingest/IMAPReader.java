package com.codepurls.mailytics.service.ingest;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.data.imap.IMAPFolder;
import com.codepurls.mailytics.data.imap.IMAPMail;

public class IMAPReader extends JavaMailReaderBase<IMAPFolder> {
  public static final Logger LOG = LoggerFactory.getLogger("IMAPReader");
  private Mailbox            mailbox;

  public IMAPReader(Mailbox mailbox) {
    this.mailbox = mailbox;
  }

  @Override
  protected Properties getProperties() {
    Properties props = new Properties();
    props.setProperty("mail.host", mailbox.incomingServer.host);
    props.setProperty("mail.port", mailbox.incomingServer.port);
    props.setProperty("mail.transport.protocol", mailbox.incomingServer.protocol);
    return props;
  }

  @Override
  protected Session createSession() {
    String userName = mailbox.username;
    String password = mailbox.password;
    Session session = Session.getInstance(getProperties(), new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(userName, password);
      }
    });
    return session;
  }

  @Override
  protected List<Folder> getDefaultFolders(Store store) throws MessagingException {
    /* Others GMail folders :
     * [Gmail]/All Mail   This folder contains all of your Gmail messages.
     * [Gmail]/Drafts     Your drafts.
     * [Gmail]/Sent Mail  Messages you sent to other people.
     * [Gmail]/Spam       Messages marked as spam.
     * [Gmail]/Starred    Starred messages.
     * [Gmail]/Trash      Messages deleted from Gmail.
     */
    return Arrays.asList(store.getFolder("Inbox"));
  }
  
  @Override
  protected Store getStore(String uri, Session session) throws NoSuchProviderException {
    return session.getStore(mailbox.incomingServer.protocol);
  }

  @Override
  protected Mail newMail(IMAPFolder mboxFolder, Message message) {
    return new IMAPMail(mboxFolder, message);
  }

  @Override
  protected IMAPFolder newFolder(Folder folder, IMAPFolder parent) {
    return new IMAPFolder(folder, parent);
  }

}
