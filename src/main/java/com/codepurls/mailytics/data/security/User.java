package com.codepurls.mailytics.data.security;

import java.util.List;

import com.codepurls.mailytics.data.core.Mailbox;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class User {
  public int           id;
  public String        firstName, lastName, email;
  public List<Mailbox> mailboxes;
  public String        username;
  @JsonIgnore
  public String        phash;
}
