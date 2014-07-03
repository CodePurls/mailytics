package com.codepurls.mailytics.data.core;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.ingest.MBoxReader;
import com.codepurls.mailytics.service.ingest.MailReader.MailVisitor;
import com.codepurls.mailytics.service.ingest.MailReaderContext;
import com.codepurls.mailytics.service.ingest.PSTReader;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Mailbox {
  public enum Type {
    PST, MBOX// , OWA, IMAP, POP
  }

  public static class Server {
    public String  name, description;
    public Integer port;
  }

  public enum Status {
    NEW, INDEXING, INDEXED, ERROR
  }

  public int    id;
  public long   size;
  public int    totalFolders, totalMails;
  public int    lastFolderRead, lastMessageRead;
  public Status status;

  @NotEmpty(message = "is required")
  public String name;
  @NotNull(message = "is required")
  public Type   type;
  @NotEmpty(message = "is required")
  public String mailLocation;

  public User   user;
  public String fullName, description, email, username, password;
  public Server incomingServer, outgoingServer;

  public void visit(MailReaderContext context, MailVisitor visitor) {
    if (type == Type.PST) {
      new PSTReader().visit(context, getMailLocation(), visitor);
    } else if (type == Type.MBOX) {
      new MBoxReader().visit(context, getMailLocation(), visitor);
    }
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type.name();
  }

  public String getMailLocation() {
    return mailLocation;
  }

  @JsonIgnore
  public int getUserId() {
    return user.id;
  }
}
