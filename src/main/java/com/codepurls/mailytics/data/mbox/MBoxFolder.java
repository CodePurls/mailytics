package com.codepurls.mailytics.data.mbox;

import javax.mail.Folder;

import com.codepurls.mailytics.data.core.AbstractFolder;

public class MBoxFolder extends AbstractFolder<Folder, MBoxFolder> {
  public MBoxFolder(Folder folder, MBoxFolder parent) {
    super(folder, parent);
  }

  public String getName() {
    return folder.getName();
  }
}