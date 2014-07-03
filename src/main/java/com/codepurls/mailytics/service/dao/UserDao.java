package com.codepurls.mailytics.service.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.data.core.Mailbox.Status;
import com.codepurls.mailytics.data.core.Mailbox.Type;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.dao.UserDao.MailboxMapper;
import com.codepurls.mailytics.service.dao.UserDao.UserMapper;

@RegisterMapper({UserMapper.class, MailboxMapper.class})
public interface UserDao {
  public static class UserMapper implements ResultSetMapper<User> {
    public User map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      User user = new User();
      user.id = r.getInt("id");
      user.email = r.getString("email");
      user.username = r.getString("user_name");
      user.firstName = r.getString("first_name");
      user.lastName = r.getString("last_name");
      user.phash = r.getString("phash");
      return user;
    }
  }

  public static class MailboxMapper implements ResultSetMapper<Mailbox> {
    public Mailbox map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      Mailbox mb = new Mailbox();
      mb.id = r.getInt("id");
      mb.name = r.getString("name");
      mb.type = Type.valueOf(r.getString("type"));
      mb.mailLocation = r.getString("location");
      mb.size = r.getLong("size");
      mb.totalFolders = r.getInt("num_folders");
      mb.totalMails = r.getInt("num_messages");
      mb.lastFolderRead = r.getInt("last_folder_read");
      mb.lastMessageRead = r.getInt("last_message_read");
      mb.status = Status.valueOf(r.getString("status"));
      return mb;
    }
  }

  @SqlUpdate("insert into something (id, name) values (:id, :name)")
  void insert(@Bind("id") int id, @Bind("name") String name);

  @SqlUpdate("INSERT INTO mailbox (name, type, location, userid) VALUES (:mb.name, :mb.type, :mb.mailLocation, :mb.userId) ")
  @GetGeneratedKeys
  int createMailbox(@BindBean("mb") Mailbox mb);

  @SqlQuery("SELECT * FROM user WHERE id = :id AND deleted = false")
  User findById(@Bind("id") int uid);

  @SqlQuery("SELECT * FROM mailbox WHERE id = :id")
  Mailbox findMailboxById(@Bind("id") int id);

  @SqlQuery("SELECT * FROM mailbox WHERE userId = :userId")
  Collection<Mailbox> getMailboxes(@Bind("userId") int userId);
}
