package com.codepurls.mailytics.data.pst;

import java.io.IOException;
import java.util.AbstractList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.codepurls.mailytics.data.core.AbstractMail;
import com.codepurls.mailytics.data.core.Attachment;
import com.pff.PSTException;
import com.pff.PSTMessage;

public final class MSPSTMail extends AbstractMail<MSPSTFolder> {
  private final PSTMessage    msg;
  private Map<String, String> parsedHeaders;

  public MSPSTMail(MSPSTFolder folder, PSTMessage msg) {
    super(folder);
    this.msg = msg;
  }

  public Map<String, String> getHeaders() {
    return lazyParse(msg.getTransportMessageHeaders());
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

  public String getBody() {
    return msg.getBody();
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