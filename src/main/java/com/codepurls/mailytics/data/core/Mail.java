package com.codepurls.mailytics.data.core;

import java.util.List;
import java.util.Map;

public interface Mail {
  public MailFolder getFolder();

  public Map<String, String> getHeaders();

  public String getBody();

  public List<Attachment> getAttachments();
}
