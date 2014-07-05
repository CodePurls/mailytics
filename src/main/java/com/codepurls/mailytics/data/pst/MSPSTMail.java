package com.codepurls.mailytics.data.pst;

import java.io.IOException;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.data.core.AbstractMail;
import com.codepurls.mailytics.data.core.Attachment;
import com.pff.PSTException;
import com.pff.PSTMessage;

public final class MSPSTMail extends AbstractMail<MSPSTFolder> {
  private static final Logger LOG = LoggerFactory.getLogger("MSPSTMail");
  private final PSTMessage    msg;
  private Map<String, String> parsedHeaders;

  public MSPSTMail(MSPSTFolder folder, PSTMessage msg) {
    super(folder);
    this.msg = msg;
  }

  public Map<String, String> getHeaders() {
    for (int i = 0; i < 3; i++)
      try {
        return lazyParse(msg.getTransportMessageHeaders());
      } catch (Exception e) {
        LOG.warn("Error parsing headers for message, attempt {}", i, e);
      }
    return Collections.emptyMap();
  }

  public String getBody() {
    for (int i = 0; i < 3; i++)
      try {
        return msg.getBody();
      } catch (Exception e) {
        LOG.warn("Error parsing message, attempt {}", i, e);
      }
    return "";
  }

  public String getMessageId() {
    return msg.getInternetMessageId();
  }

  public Date getDate() {
    return msg.getMessageDeliveryTime();
  }

  private Map<String, String> lazyParse(String transportMessageHeaders) {
    if (this.parsedHeaders != null) return parsedHeaders;
    Scanner scanner = new Scanner(transportMessageHeaders);
    Map<String, String> headers = new LinkedHashMap<>();
    String prev = "";
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if (line.isEmpty()) continue;
      int colon = line.indexOf(':');
      boolean filler = line.startsWith(" ");
      String key, value;
      if (colon == -1 || filler) {
        key = prev;
        value = headers.get(key) + " " + line.trim();
      } else {
        key = line.substring(0, colon);
        prev = key;
        value = line.substring(colon + 1).trim();
      }
      headers.put(key, value);
    }
    this.parsedHeaders = headers;
    scanner.close();
    return headers;
  }

  public static class AttachmentList extends AbstractList<Attachment> {
    private final PSTMessage msg;

    public AttachmentList(PSTMessage msg) {
      this.msg = msg;
    }

    public Attachment get(int index) {
      try {
        return new MSPSTAttachment(msg.getAttachment(index));
      } catch (IOException | PSTException e) {
        throw new RuntimeException(e);
      }
    }

    public int size() {
      return msg.getNumberOfAttachments();
    }

  }

  public List<Attachment> getAttachments() {
    return new AttachmentList(msg);
  }
}