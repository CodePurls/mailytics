package com.codepurls.mailytics.data.pst;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;

import com.codepurls.mailytics.data.core.Attachment;
import com.codepurls.mailytics.service.ingest.MailReaderException;
import com.pff.PSTAttachment;
import com.pff.PSTException;

public class MSPSTAttachment implements Attachment {
  private final PSTAttachment att;

  public MSPSTAttachment(PSTAttachment att) {
    this.att = att;
  }

  public String getName() {
    String name = att.getDisplayName();
    if (StringUtils.isBlank(name)) name = att.getFilename();
    return name;
  }

  public String getMediaType() {
    return att.getMimeTag();
  }

  public int getSize() {
    try {
      return att.getFilesize();
    } catch (PSTException | IOException e) {
      throw new MailReaderException("Error determining size of attachment", e);
    }
  }

  public InputStream getStream() {
    try {
      return att.getFileInputStream();
    } catch (IOException | PSTException e) {
      throw new MailReaderException("Error streaming attachment contents", e);
    }
  }

}