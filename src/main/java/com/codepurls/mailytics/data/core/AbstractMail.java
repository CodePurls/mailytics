package com.codepurls.mailytics.data.core;

public abstract class AbstractMail<F extends MailFolder> implements Mail {
  protected final F folder;

  public AbstractMail(F folder) {
    this.folder = folder;
  }

  public F getFolder() {
    return folder;
  }
}
