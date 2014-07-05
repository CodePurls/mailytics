package com.codepurls.mailytics.service.ingest;

import java.io.Closeable;
import java.io.File;

import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.core.MailFolder;


public interface MailReader extends Closeable{
  public interface MailVisitor{
    void onNewFolder(MailFolder folder);
    void onNewMail(Mail mail);
    void onError(Throwable t, MailFolder folder, Mail mail);
  }

  public abstract void visit(MailReaderContext context, File file, MailVisitor visitor);

  public abstract void visit(MailReaderContext context, String uri, MailVisitor visitor) throws MailReaderException;

}
