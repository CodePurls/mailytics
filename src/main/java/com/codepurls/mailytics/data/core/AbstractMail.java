package com.codepurls.mailytics.data.core;

import static com.codepurls.mailytics.utils.StringUtils.orEmpty;

import java.nio.charset.Charset;
import java.util.List;

import com.codepurls.mailytics.utils.RFC822Constants;
import com.codepurls.mailytics.utils.StringUtils;
import com.google.common.base.Charsets;

public abstract class AbstractMail<F extends MailFolder> implements Mail {
  protected final F folder;

  public AbstractMail(F folder) {
    this.folder = folder;
  }

  public F getFolder() {
    return folder;
  }

  public String getFrom() {
    return StringUtils.canonicalize(getHeaders().get(RFC822Constants.FROM)).get(0);
  }

  public List<String> getTo() {
    return StringUtils.canonicalize(getHeaders().get(RFC822Constants.TO));
  }

  public String getMessageId() {
    return getHeaders().get(RFC822Constants.MESSAGE_ID);
  }

  public String getSubject() {
    return getHeaders().get(RFC822Constants.SUBJECT);
  }

  public List<String> getCc() {
    return StringUtils.canonicalize(getHeaders().get(RFC822Constants.CC));
  }

  public List<String> getBcc() {
    return StringUtils.canonicalize(getHeaders().get(RFC822Constants.BCC));
  }

  public boolean isReply() {
    String subject = getSubject().toLowerCase();
    return subject.startsWith("re:");
  }

  public boolean isForwarded() {
    String subject = getSubject().toLowerCase();
    return subject.startsWith("fw:") || subject.startsWith("fwd:");
  }

  public String getActualBody() {
    String body = getBody();
    if (isReply() || isForwarded()) { return StringUtils.toPlainText(body, getContentType()); }
    return body;
  }

  public String getContentType() {
    String contentType = orEmpty(getHeaders().get(RFC822Constants.CONTENT_TYPE));
    return contentType.split(";")[0];
  }

  public String getCharset() {
    String contentType = orEmpty(getHeaders().get(RFC822Constants.CONTENT_TYPE));
    for (String val : contentType.split(";")) {
      val = val.trim().toLowerCase();
      if (val.startsWith("boundary") || val.startsWith("format")) {
        continue;
      } else if (val.startsWith("charset")) {
        String cs = val.split("=")[1];
        if (cs.startsWith("\"")) {
          cs = cs.substring(1);
        }
        if (cs.endsWith("\"")) {
          cs = cs.substring(0, cs.lastIndexOf("\""));
        }
        cs = cs.toLowerCase();
        return Charset.forName(cs).name();
      }
    }
    return Charsets.UTF_8.name();
  }
}
