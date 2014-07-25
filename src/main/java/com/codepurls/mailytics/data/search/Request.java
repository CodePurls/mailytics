package com.codepurls.mailytics.data.search;

import java.util.Collections;
import java.util.List;

import org.apache.lucene.search.SortField.Type;

import com.codepurls.mailytics.service.index.MailIndexer.MailSchema;

public class Request {
  public enum SortType {
    DATE(MailSchema.date, Type.LONG), FROM(MailSchema.from), TO(MailSchema.to), SUBJECT(MailSchema.subject);

    private final Type   type;
    private final String sortField;

    private SortType(MailSchema sortField, Type type) {
      this.sortField = sortField.name();
      this.type = type;
    }

    private SortType(MailSchema sortField) {
      this(sortField, Type.STRING);
    }

    public Type getValueType() {
      return type;
    }

    public String getSortField() {
      return sortField;
    }
  }

  public enum SortDirecton {
    ASC, DESC
  }

  public String        query;
  public SortType      sort       = SortType.DATE;
  public SortDirecton  sortDir    = SortDirecton.ASC;
  public int           pageSize   = 10;
  public int           pageNum    = 1;
  public long          startTime  = -1;
  public long          endTime    = -1;
  public List<Integer> mailboxIds = Collections.emptyList();
  public List<String>  similarFields;

}
