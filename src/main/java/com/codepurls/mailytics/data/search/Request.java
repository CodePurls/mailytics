package com.codepurls.mailytics.data.search;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.search.SortField.Type;

import com.codepurls.mailytics.service.index.MailIndexer.MailSchema;

public class Request {
  public enum Resolution {
    //@formatter:off
    MINUTE ("yyyy/MM/dd HH:mm", 60 * 1000),
    HOUR("yyyy/MM/dd HH", 60 * MINUTE.toMillis()),
    DAY("yyyy/MM/dd", 24 * HOUR.toMillis()),
    MONTH("yyyy/MM", 30 * DAY.toMillis()),
    YEAR("yyyy", 365 * MONTH.toMillis())
    //@formatter:on
    ;

    private final long              millis;
    private final DateTimeFormatter formatter;

    private Resolution(String format, long millis) {
      this.millis = millis;
      this.formatter = DateTimeFormatter.ofPattern(format);
    }

    public long toMillis() {
      return this.millis;
    }

    public String format(Long ts) {
      return formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()));
    }
  }

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
  public SortType      sort                      = SortType.DATE;
  public SortDirecton  sortDir                   = SortDirecton.ASC;
  public int           pageSize                  = 10;
  public int           pageNum                   = 1;
  public long          startTime                 = -1;
  public long          endTime                   = -1;
  public List<Integer> mailboxIds                = Collections.emptyList();
  public List<String>  similarFields;
  public Resolution    resolution                = Resolution.DAY;
  public MailSchema    trendField                = MailSchema.date;
  public MailSchema    keywordField              = MailSchema.subject;
  public int           termFrequencyMinThreshold = 10;

}
