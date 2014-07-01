package com.codepurls.mailytics.data.mbox;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.BodyPart;
import javax.mail.MessagingException;

import com.codepurls.mailytics.data.core.Attachment;
import com.codepurls.mailytics.service.ingest.MailReaderException;

public class MBoxAttachment implements Attachment {

  private final BodyPart part;

  public MBoxAttachment(BodyPart part) {
    this.part = part;
  }

  public String getName() {
    try {
      return part.getFileName();
    } catch (MessagingException e) {
      throw new MailReaderException("Error reading file name", e);
    }
  }

  public String getMediaType() {
    try {
      String contentType = part.getContentType();
      int limit = contentType.indexOf(';');
      return contentType.substring(0, Math.min(limit, contentType.length()));
    } catch (MessagingException e) {
      throw new MailReaderException("Error reading content-type header", e);
    }
  }

  public int getSize() throws MailReaderException {
    try {
      return part.getSize();
    } catch (MessagingException e) {
      throw new MailReaderException("Error determining file size", e);
    }
  }

  public InputStream getStream() throws MailReaderException {
    try {
      return part.getInputStream();
    } catch (IOException | MessagingException e) {
      throw new MailReaderException("Error streaming attachment body", e);
    }
  }

}