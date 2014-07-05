package com.codepurls.mailytics.data.core;

import java.io.IOException;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.ingest.MBoxReader;
import com.codepurls.mailytics.service.ingest.MailReader;
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

  public enum MailboxStatus {
    NEW, INDEXING, INDEXED, ERROR
  }

  public int                   id;
  public long                  size;
  public int                   totalFolders, totalMails;
  public int                   lastFolderRead, lastMessageRead;
  public MailboxStatus                status;

  @NotEmpty(message = "is required")
  public String                name;
  @NotNull(message = "is required")
  public Type                  type;
  @NotEmpty(message = "is required")
  public String                mailLocation;

  public User                  user;
  public String                fullName, description, email, username, password;
  public Server                incomingServer, outgoingServer;

  private transient MailReader reader;

  public void visit(MailReaderContext context, MailVisitor visitor) {
    if (type == Type.PST) {
      reader = new PSTReader();
    } else if (type == Type.MBOX) {
      reader = new MBoxReader();
    }
    reader.visit(context, getMailLocation(), visitor);
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((user == null) ? 0 : user.hashCode());
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Mailbox other = (Mailbox) obj;
    if (id != other.id) return false;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    if (user == null) {
      if (other.user != null) return false;
    } else if (!user.equals(other.user)) return false;
    if (username == null) {
      if (other.username != null) return false;
    } else if (!username.equals(other.username)) return false;
    return true;
  }

  public void closeReader() throws IOException {
    if(this.reader != null)
      this.reader.close();
  }
}
