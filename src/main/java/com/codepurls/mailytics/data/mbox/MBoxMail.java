package com.codepurls.mailytics.data.mbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
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

import org.apache.commons.lang.StringUtils;

import com.codepurls.mailytics.data.core.AbstractMail;
import com.codepurls.mailytics.data.core.Attachment;
import com.codepurls.mailytics.service.ingest.MailReaderException;

public final class MBoxMail extends AbstractMail<MBoxFolder> {
  private final Message mail;

  public MBoxMail(MBoxFolder folder, Message mail) {
    super(folder);
    this.mail = mail;
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getHeaders() {
    Map<String, String> res = new HashMap<>();
    Enumeration<Header> headers;
    try {
      headers = mail.getAllHeaders();
    } catch (MessagingException e) {
      throw new MailReaderException("Error retrieving headers", e);
    }
    while (headers.hasMoreElements()) {
      Header header = headers.nextElement();
      res.put(header.getName(), header.getValue());
    }
    return res;
  }

  public String getBody() {
    try {
      Object content = mail.getContent();
      if (content instanceof String) return (String) content;
      return findFirstBodyPart(content);
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

  private List<Attachment> getAttachments(BodyPart part) throws Exception {
    List<Attachment> result = new ArrayList<>();
    Object content = part.getContent();
    if (content instanceof InputStream || content instanceof String) {
      if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || StringUtils.isNotBlank(part.getFileName())) {
        result.add(new MBoxAttachment(part));
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
}
