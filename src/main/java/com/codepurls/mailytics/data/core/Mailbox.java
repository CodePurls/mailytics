package com.codepurls.mailytics.data.core;

import org.hibernate.validator.constraints.NotEmpty;

public class Mailbox {
  public enum Type {
    PST, MBOX//, OWA, IMAP, POP
  }

  public static class Server {
    public String  name, description;
    public Integer port;
  }

  @NotEmpty(message = ",required,")
  public Type   type;
  @NotEmpty(message = ",required,")
  public String mailLocation;

  public String fullName, description, email, username, password;
  public Server incomingServer, outgoingServer;
}
