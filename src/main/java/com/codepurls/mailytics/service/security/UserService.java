package com.codepurls.mailytics.service.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.data.core.Mailbox.MailboxStatus;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.EventLogService;
import com.codepurls.mailytics.service.EventLogService.EventType;
import com.codepurls.mailytics.service.EventLogService.ObjectType;
import com.codepurls.mailytics.service.dao.UserDao;
import com.codepurls.mailytics.utils.crypto.PBEHash;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;

public class UserService {
  private static final Logger   LOG = LoggerFactory.getLogger(UserService.class);
  private final UserDao         userDao;
  private final EventLogService eventLogService;

  public UserService(EventLogService eventLogService, UserDao userDao) {
    this.eventLogService = eventLogService;
    this.userDao = userDao;
  }

  public UserDao getUserDao() {
    return userDao;
  }

  public Mailbox createMailbox(User user, Mailbox mailbox) {
    user = validate(user);
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
      User user = userDao.findById(Integer.parseInt(uid));
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

  public String authenticate(String username, String password) {
    User user = userDao.findByUsername(username);
    if (PBEHash.validatePassword(password, user.phash)) return user.id + '.' + password;
    return null;
  }

  public Collection<Mailbox> getMailboxes(User user) {
    User usr = validate(user);
    Collection<Mailbox> mailboxes = userDao.getMailboxes(usr.id);
    mailboxes.forEach((mb) -> mb.user = usr);
    return mailboxes;
  }

  public Mailbox getMailbox(User user, int mailboxId) {
    user = validate(user);
    Collection<Mailbox> mailboxes = getMailboxes(user);
    for (Mailbox mailbox : mailboxes) {
      if (mailbox.id == mailboxId) return mailbox;
    }
    return null;
  }

  public Collection<Mailbox> getMailboxes(User user, List<Integer> mbIds) {
    user = validate(user);
    Collection<Mailbox> mailboxes = getMailboxes(user);
    List<Mailbox> toReturn = new ArrayList<Mailbox>(mailboxes.size() - mbIds.size());
    for (Mailbox mailbox : mailboxes) {
      if (mbIds.contains(mailbox.id)) toReturn.add(mailbox);
    }
    return toReturn;
  }

  public void updateMailboxStatus(Mailbox mb, MailboxStatus status) {
    userDao.updateMailboxStatus(mb.id, status.name());
  }

  public int getUserCount() {
    return userDao.getUserCount();
  }

  public Optional<User> findFirstUser() {
    return Optional.fromNullable(userDao.findById(1));
  }

  private User validate(User user) {
    if(getUserCount() > 1) {
      Preconditions.checkArgument(user != null);
    }
    return findFirstUser().get();
  }
}
