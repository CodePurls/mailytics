package com.codepurls.mailytics.service.security;

import io.dropwizard.lifecycle.Managed;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.EventLogService;
import com.codepurls.mailytics.service.EventLogService.EventType;
import com.codepurls.mailytics.service.EventLogService.ObjectType;
import com.codepurls.mailytics.service.dao.UserDao;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.hash.Hashing;

public class UserService implements Managed {
  private static final Logger   LOG = LoggerFactory.getLogger(UserService.class);
  private final UserDao         userDao;
  private final EventLogService eventLogService;

  public UserService(EventLogService eventLogService, UserDao userDao) {
    this.eventLogService = eventLogService;
    this.userDao = userDao;
  }

  public void start() throws Exception {
  }

  public void stop() throws Exception {
  }

  public UserDao getUserDao() {
    return userDao;
  }

  public Mailbox createMailbox(User user, Mailbox mailbox) {
    int id = userDao.createMailbox(mailbox);
    eventLogService.log(EventType.created, ObjectType.mailbox, user, null, mailbox.name);
    return userDao.findMailboxById(id);
  }

  public Optional<User> findUser(String credentials) {
    Optional<User> none = Optional.absent();
    int boundary = credentials.indexOf('.');
    if (boundary == -1) {
      LOG.error("Invalid bearer token: {}", credentials);
      return none;
    }
    try {
      String uid = credentials.substring(0, boundary);
      String hash = credentials.substring(Math.min(credentials.length(), boundary + 1));
      User user =  userDao.findById(Integer.parseInt(uid));
      if (user != null) {
        String h = user.phash;
        String sha1 = Hashing.sha256().hashString(h, Charsets.UTF_8).toString();
        if (sha1.equals(hash)) return Optional.of(user);
      }
    } catch (Exception e) {
      LOG.error("Invalid user id '{}' was supplied.", credentials, e);
      return none;
    }
    return none;
  }

  public Collection<Mailbox> getMailboxes(User user) {
    return userDao.getMailboxes(user.id);
  }

}
