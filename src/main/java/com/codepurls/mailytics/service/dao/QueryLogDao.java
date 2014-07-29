package com.codepurls.mailytics.service.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.codepurls.mailytics.data.search.StoredQuery;
import com.codepurls.mailytics.service.dao.QueryLogDao.QueryLogMapper;

@RegisterMapper({ QueryLogMapper.class })
public interface QueryLogDao {
  public static class QueryLogMapper implements ResultSetMapper<StoredQuery> {
    public StoredQuery map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      StoredQuery q = new StoredQuery();
      q.id = r.getInt("id");
      q.query = r.getString("query");
      q.hash = r.getLong("hash");
      return q;
    }
  }

  @SqlUpdate("INSERT INTO query_log (hash, query) VALUES (:hash, :query)")
  public void log(@Bind("hash") long hash, @Bind("query") String query);

  @SqlQuery("SELECT * FROM query_log WHERE hash = :hash")
  public StoredQuery findByHash(@Bind("hash") long hash);

}
