package com.codepurls.mailytics.data.core;

import com.codepurls.mailytics.utils.RFC822Constants;

public abstract class AbstractMail<F extends MailFolder> implements Mail {
  protected final F folder;

  public AbstractMail(F folder) {
    this.folder = folder;
  }

  public F getFolder() {
    return folder;
  }

  public String getFrom() {
    return getHeaders().get(RFC822Constants.FROM);
  }

  public String getTo() {
    return getHeaders().get(RFC822Constants.TO);
  }

  public String getMessageId() {
    return getHeaders().get(RFC822Constants.MESSAGE_ID);
  }

  public String getSubject() {
    return getHeaders().get(RFC822Constants.SUBJECT);
  }

  public String getCc() {
    return getHeaders().get(RFC822Constants.CC);
  }
}
