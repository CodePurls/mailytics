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

  public String getSubject();

  public List<String> getTo();

  public List<String> getCc();

  public List<String> getBcc();

  public String getActualBody();

  public String getContentType();

  public String getCharset();
}
