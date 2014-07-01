package com.codepurls.mailytics.service.security;

import io.dropwizard.lifecycle.Managed;

import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.dao.UserDao;

public class UserService implements Managed {

  private final UserDao userDao;

  public UserService(UserDao userDao) {
    this.userDao = userDao;
  }

  public void start() throws Exception {
  }

  public void stop() throws Exception {
  }

  public UserDao getUserDao() {
    return userDao;
  }

  public void createMailbox(User user, Mailbox mailbox) {

  }

}
