package com.codepurls.mailytics.api.v1.transfer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RESTMail {
  public String              messageId;
  public Date                date;
  public String              subject, from, cc, to, body;
  @JsonIgnore
  public Map<String, String> headers = new HashMap<>();
  public String              folder;
  public int                 attachmentCount;
}
