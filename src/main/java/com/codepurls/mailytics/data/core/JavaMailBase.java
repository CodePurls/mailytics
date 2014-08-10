package com.codepurls.mailytics.data.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.service.ingest.MailReaderException;
import com.codepurls.mailytics.utils.StringUtils;

public abstract class JavaMailBase<F extends MailFolder> extends AbstractMail<F> {
  private static final Logger LOG = LoggerFactory.getLogger("JavaMailBase");
  private final Message       mail;
  private Map<String, String> headers;

  public JavaMailBase(F folder, Message mail) {
    super(folder);
    this.mail = mail;
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getHeaders() {
    if (headers != null) return headers;
    Map<String, String> res = new HashMap<>();
    Enumeration<Header> mailHeaders;
    try {
      mailHeaders = mail.getAllHeaders();
    } catch (MessagingException e) {
      throw new MailReaderException("Error retrieving headers", e);
    } catch (OutOfMemoryError e) {
      LOG.error("", e);
      return res;
    }
    while (mailHeaders.hasMoreElements()) {
      Header header = mailHeaders.nextElement();
      res.put(header.getName().toLowerCase(), header.getValue());
    }
    this.headers = res;
    return this.headers;
  }

  public String getBody() {
    try {
      Object content = mail.getContent();
      if (content instanceof String) return StringUtils.toSimpleText((String) content);
      String bodyPart = findFirstBodyPart(content);
      return StringUtils.toSimpleText(bodyPart);
    } catch (IOException | MessagingException e) {
      throw new MailReaderException("Error retrieving email body", e);
    }
  }

  private String findFirstBodyPart(Object content) throws MessagingException, IOException {
    if (content instanceof String) return (String) content;
    if (content instanceof BodyPart) { return findFirstBodyPart(((BodyPart) content).getContent()); }
    if (content instanceof Multipart) {
      Multipart multipart = (Multipart) content;
      for (int i = 0; i < multipart.getCount(); i++) {
        String str = findFirstBodyPart(multipart.getBodyPart(i));
        if (str != null) return str;
      }
    }
    return null;
  }

  public List<Attachment> getAttachments() {
    try {
      Object content = mail.getContent();
      if (content instanceof String) return Collections.emptyList();

      if (content instanceof Multipart) {
        Multipart multipart = (Multipart) content;
        List<Attachment> result = new ArrayList<>();

        for (int i = 0; i < multipart.getCount(); i++) {
          result.addAll(getAttachments(multipart.getBodyPart(i)));
        }
        return result;

      }
    } catch (Exception e) {
      throw new MailReaderException("Error reading attachments", e);
    }
    return Collections.emptyList();
  }

  public Date getDate() {
    try {
      Date receivedDate = mail.getReceivedDate();
      Date sentDate = mail.getSentDate();
      return sentDate == null ? receivedDate : sentDate;
    } catch (MessagingException e) {
      return new Date();
    }
  }

  private List<Attachment> getAttachments(BodyPart part) throws Exception {
    List<Attachment> result = new ArrayList<>();
    Object content = part.getContent();
    if (content instanceof InputStream || content instanceof String) {
      if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || !StringUtils.isBlank(part.getFileName())) {
        result.add(newAttachment(part));
        return result;
      } else {
        return Collections.emptyList();
      }
    }

    if (content instanceof Multipart) {
      Multipart multipart = (Multipart) content;
      for (int i = 0; i < multipart.getCount(); i++) {
        BodyPart bodyPart = multipart.getBodyPart(i);
        result.addAll(getAttachments(bodyPart));
      }
    }
    return result;
  }

  protected abstract Attachment newAttachment(BodyPart part);

}
