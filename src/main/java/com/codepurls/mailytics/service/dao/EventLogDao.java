package com.codepurls.mailytics.service.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface EventLogDao {

  @SqlUpdate("INSERT INTO event_log (action, object, userid, old_value, new_value) VALUES (:action, :object, :userId, :oldValue, :newValue)")
  public void log(@Bind("action") String action, @Bind("object") String object, 
      @Bind("userId") int userId, @Bind("oldValue") String oldValue, @Bind("newValue") String newValue);

}
