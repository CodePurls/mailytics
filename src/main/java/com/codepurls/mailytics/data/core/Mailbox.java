package com.codepurls.mailytics.data.core;

public class Mailbox {
  public enum Type {
    PST, MBOX, OWA, IMAP, POP
  }

  public static class Server {
    public String  name, description;
    public Integer port;
  }

  public Type type;
  public String mailLocation, fullName, description, email, username, password;
  public Server incomingServer, outgoingServer;
}
