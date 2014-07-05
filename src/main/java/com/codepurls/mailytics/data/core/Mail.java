package com.codepurls.mailytics.data.core;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface Mail {
  public MailFolder getFolder();

  public Map<String, String> getHeaders();

  public String getBody();

  public List<Attachment> getAttachments();
  
  public String getMessageId();
  public Date getDate();
  public String getFrom();
  public String getTo();
  public String getSubject();
  public String getCc();
  
}
