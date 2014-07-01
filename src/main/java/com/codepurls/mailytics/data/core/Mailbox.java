package com.codepurls.mailytics.data.core;

import org.hibernate.validator.constraints.NotEmpty;

import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.ingest.MBoxReader;
import com.codepurls.mailytics.service.ingest.MailReader.MailVisitor;
import com.codepurls.mailytics.service.ingest.MailReaderContext;
import com.codepurls.mailytics.service.ingest.PSTReader;

public class Mailbox {
  public enum Type {
    PST, MBOX// , OWA, IMAP, POP
  }

  public static class Server {
    public String  name, description;
    public Integer port;
  }

  @NotEmpty(message = ",required,")
  public Type   type;
  @NotEmpty(message = ",required,")
  public String mailLocation;
  
  public User   user;
  public String fullName, description, email, username, password;
  public Server incomingServer, outgoingServer;

  public void visit(MailReaderContext context, MailVisitor visitor) {
    if (type == Type.PST) {
      new PSTReader().visit(context, mailLocation, visitor);
    } else if (type == Type.MBOX) {
      new MBoxReader().visit(context, mailLocation, visitor);
    }
  }
}
