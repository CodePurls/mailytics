package com.codepurls.mailytics.data.security;

import java.util.List;

import com.codepurls.mailytics.data.core.Mailbox;

public class User {
  public Integer       id;
  public String        firstName, lastName, email, organization;
  public List<Mailbox> mailboxes;
  public String username;
}
