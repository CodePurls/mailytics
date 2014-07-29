package com.codepurls.mailytics.data.imap;

import javax.mail.BodyPart;
import javax.mail.Message;

import com.codepurls.mailytics.data.core.Attachment;
import com.codepurls.mailytics.data.core.JavaMailBase;

public class IMAPMail extends JavaMailBase<IMAPFolder> {

  public IMAPMail(IMAPFolder folder, Message mail) {
    super(folder, mail);
  }

  @Override
  protected Attachment newAttachment(BodyPart part) {
    return new IMAPAttachment(part);
  }
}
