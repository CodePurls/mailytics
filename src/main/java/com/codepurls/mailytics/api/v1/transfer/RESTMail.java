package com.codepurls.mailytics.api.v1.transfer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RESTMail {
  public String              messageId;
  public Date                date;
  public String              subject, from, cc, to, body;
  public Map<String, String> headers = new HashMap<>();
  public String              folder;
  public int                 attachmentCount;
}
