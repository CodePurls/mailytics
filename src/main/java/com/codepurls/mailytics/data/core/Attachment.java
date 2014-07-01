package com.codepurls.mailytics.data.core;

import java.io.InputStream;

import com.codepurls.mailytics.service.ingest.MailReaderException;

public interface Attachment {
  public String getName();

  public String getMediaType();

  public int getSize() throws MailReaderException;

  public InputStream getStream() throws MailReaderException;
}
