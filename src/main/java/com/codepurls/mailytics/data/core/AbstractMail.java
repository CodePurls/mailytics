package com.codepurls.mailytics.data.core;

import java.util.List;

import com.codepurls.mailytics.utils.RFC822Constants;
import com.codepurls.mailytics.utils.StringUtils;

public abstract class AbstractMail<F extends MailFolder> implements Mail {
  protected final F folder;

  public AbstractMail(F folder) {
    this.folder = folder;
  }

  public F getFolder() {
    return folder;
  }

  public String getFrom() {
    return StringUtils.canonicalize(getHeaders().get(RFC822Constants.FROM)).get(0);
  }

  public List<String> getTo() {
    return StringUtils.canonicalize(getHeaders().get(RFC822Constants.TO));
  }

  public String getMessageId() {
    return getHeaders().get(RFC822Constants.MESSAGE_ID);
  }

  public String getSubject() {
    return getHeaders().get(RFC822Constants.SUBJECT);
  }
  
  public List<String> getCc() {
    return StringUtils.canonicalize(getHeaders().get(RFC822Constants.CC));
  }

  public List<String> getBcc() {
    return StringUtils.canonicalize(getHeaders().get(RFC822Constants.BCC));
  }
}
