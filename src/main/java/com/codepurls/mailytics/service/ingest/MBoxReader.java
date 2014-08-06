package com.codepurls.mailytics.service.ingest;

import static java.lang.String.format;

import java.io.File;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.mbox.MBoxFolder;
import com.codepurls.mailytics.data.mbox.MBoxMail;

public class MBoxReader extends JavaMailReaderBase<MBoxFolder> {

  @Override
  protected Mail newMail(MBoxFolder mboxFolder, Message message) {
    return new MBoxMail(mboxFolder, message);
  }

  @Override
  protected MBoxFolder newFolder(Folder folder, MBoxFolder parent) {
    return new MBoxFolder(folder, parent);
  }

  @Override
  protected Properties getProperties() {
    return new Properties();
  }

  @Override
  protected Store getStore(String uri, Session session) throws NoSuchProviderException {
    return session.getStore(new URLName(format("mstor:%s", uri)));
  }
  
  @Override
  public void visit(MailReaderContext context, String uri, MailVisitor visitor) throws MailReaderException {
    File f = new File(uri);
    if(f.isDirectory()) {
      for (File file : f.listFiles()) {
        if(file.getName().startsWith(".") || file.getName().endsWith(".msf") || file.getName().endsWith(".html") || file.getName().endsWith(".dat"))
          continue;
        if(file.isDirectory())
          visit(context,file.getAbsolutePath(), visitor);
        super.visit(context, file.getAbsolutePath(), visitor);
      }
    }else {
      super.visit(context, uri, visitor);
    }
  }
}
