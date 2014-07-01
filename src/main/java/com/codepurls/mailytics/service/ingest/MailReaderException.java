package com.codepurls.mailytics.service.ingest;

public class  MailReaderException extends RuntimeException{
  private static final long serialVersionUID = -642946980377451183L;
  public MailReaderException(String msg, Exception e) {
    super(msg, e);
  }
}