package com.codepurls.mailytics.data.core;

public abstract class AbstractFolder<F, P extends MailFolder> implements MailFolder {
  protected final F folder;
  protected final P parent;
  
  protected AbstractFolder(F f, P p) {
    this.folder = f;
    this.parent = p;
  }
  
  @Override
  public MailFolder getParent() {
    return parent;
  }

}
