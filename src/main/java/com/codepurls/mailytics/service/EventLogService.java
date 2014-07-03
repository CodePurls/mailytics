package com.codepurls.mailytics.service;

import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.dao.EventLogDao;

public class EventLogService {
  public enum EventType{
    created, deleted, updated, login_success, login_failure, logout, password_changed
  }
  public enum ObjectType{
    user, mailbox
  }
  private EventLogDao eventLogDao;

  public EventLogService(EventLogDao eventLogDao) {
    this.eventLogDao = eventLogDao;
  }

  public void log(EventType event, ObjectType object, User user, String oldValue, String newValue) {
    eventLogDao.log(event.name(), object.name(), user.id, oldValue, newValue);
  }
}
