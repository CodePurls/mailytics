package com.codepurls.mailytics.data.imap;

import javax.mail.Folder;

import com.codepurls.mailytics.data.core.AbstractFolder;

public class IMAPFolder extends AbstractFolder<Folder, IMAPFolder> {

  public IMAPFolder(Folder f, IMAPFolder p) {
    super(f, p);
  }

  @Override
  public String getName() {
    return folder.getName();
  }

}
