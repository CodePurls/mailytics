package com.codepurls.mailytics.data.imap;

import javax.mail.BodyPart;

import com.codepurls.mailytics.data.mbox.MBoxAttachment;

public class IMAPAttachment extends MBoxAttachment {

  public IMAPAttachment(BodyPart part) {
    super(part);
  }

}
