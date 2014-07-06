package com.codepurls.mailytics.data.search;

import java.util.Collections;
import java.util.List;

public class Request {
  public String        query;
  public int           pageSize   = 10;
  public int           pageNum    = 1;
  public long          startTime  = -1;
  public long          endTime    = -1;
  public List<Integer> mailboxIds = Collections.emptyList();

}
