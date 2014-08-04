package com.codepurls.mailytics.service.index;

import static com.codepurls.mailytics.utils.StringUtils.orEmpty;
import static com.codepurls.mailytics.utils.StringUtils.toPlainText;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.api.v1.transfer.RESTAttachment;
import com.codepurls.mailytics.api.v1.transfer.RESTMail;
import com.codepurls.mailytics.data.core.Attachment;
import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.service.ingest.MailReaderException;
import com.codepurls.mailytics.utils.RFC822Constants;

public class MailIndexer {
  public enum MailSchema {
    id {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getMessageId()));
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.messageId = doc.get(name());
      }
    },
    organization {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getHeaders().get(RFC822Constants.ORGANIZATION)));
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
      }
    },
    content_type {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getHeaders().get(RFC822Constants.CONTENT_TYPE)));
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
      }
    },

    folder {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getFolder().getName()));
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.folder = doc.get(name());
      }
    },

    date {
      public Field[] getFields() {
        return new Field[] { new LongField(name(), 0, Store.YES), new NumericDocValuesField(name(), 0) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          Date date = mail.getDate();
          if (date == null) {
            LOG.warn("Null date for mail: {}, folder: {} ", mail.getSubject(), mail.getFolder().getName());
            date = new Date(0);
          }
          ((Field) f).setLongValue(date.getTime());
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.date = new Date(doc.getField(name()).numericValue().longValue());
      }
    },

    from {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.NO), new TextField(name(), "", Store.YES),
            new SortedDocValuesField(name(), new BytesRef("")) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          setValue((Field) f, orEmpty(mail.getFrom()));
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.from = doc.get(name());
      }
    },

    to {
      public Field[] getFields() {
        return new Field[] { new TextField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getTo()));
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.to = doc.get(name());
      }
    },

    subject {
      public Field[] getFields() {
        return new Field[] { new TextField(name(), "", Store.YES), new SortedDocValuesField(name(), new BytesRef("")) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          setValue((Field) f, orEmpty(mail.getSubject()));
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.subject = doc.get(name());
      }
    },

    contents {
      public Field[] getFields() {
        return new Field[] { new TextField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        String text = toPlainText(orEmpty(mail.getBody()));
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(text);
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.body = doc.get(name());
      }
    },
    attachment {
      private static final String ATTACHMENT_COUNT = "attachment_count";

      public Field[] getFields() {
        return new Field[0];
      }

      public void setFieldValues(Document doc, Mail mail) {
        List<Attachment> attachments = mail.getAttachments();
        doc.add(new IntField(ATTACHMENT_COUNT, attachments.size(), Store.YES));
        for (Attachment attachment : attachments) {
          try {
            doc.add(new TextField(name(), new Tika().parseToString(attachment.getStream()), Store.NO));
            doc.add(new StringField(name() + "_content_type", attachment.getMediaType(), Store.YES));
            doc.add(new StringField(name() + "_name", attachment.getName(), Store.YES));
            doc.add(new LongField(name() + "_size", attachment.getSize(), Store.YES));
          } catch (MailReaderException | IOException e) {
            LOG.warn("Error parsing attachment {}", attachment.getName(), e);
          } catch (TikaException e) {
            LOG.warn("Error parsing attachment {} for mail {}", attachment.getName(), mail.getSubject(), e);
          }
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.attachmentCount = doc.getField(ATTACHMENT_COUNT).numericValue().intValue();
        RESTAttachment[] attArr = new RESTAttachment[mail.attachmentCount];
        for (int i = 0; i < mail.attachmentCount; i++) {
          RESTAttachment att = new RESTAttachment();
          att.name = doc.get(name() + "_name");
          att.type = doc.get(name() + "_content_type");
          att.size = doc.getField(name() + "_size").numericValue().longValue();
          attArr[i] = att;
        }
        for (RESTAttachment ra : attArr) {
          mail.attachments.put(ra.name, ra);
        }
      }

    };
    public final static Set<String>  STATIC_FIELD_NAMES;
    public final static MailSchema[] STATIC_FIELDS;
    static {
      STATIC_FIELDS = MailSchema.values();
      Arrays.sort(STATIC_FIELDS);
      STATIC_FIELD_NAMES = new HashSet<>();
      for (MailSchema mf : STATIC_FIELDS) {
        STATIC_FIELD_NAMES.add(mf.name());
      }
    }

    public abstract Field[] getFields();

    public abstract void setFieldValues(Document doc, Mail mail);

    public abstract void retrieveValue(RESTMail mail, Document doc);
  }

  private static final Logger LOG = LoggerFactory.getLogger("mail-indexer");

  public static void setValue(Field f, String value) {
    if (f instanceof SortedDocValuesField) {
      f.setBytesValue(new BytesRef(value.substring(0, Math.min(32766, value.length()))));
    } else {
      f.setStringValue(value);
    }
  }

  public static Document createDocument() {
    Document doc = new Document();
    Arrays.stream(MailSchema.STATIC_FIELDS).forEach(sf -> Arrays.stream(sf.getFields()).forEach(f -> doc.add(f)));
    return doc;
  }

  public static Document prepareDocument(Mailbox mb, Mail mail) {
    Document document = createDocument();
    Arrays.stream(MailSchema.STATIC_FIELDS).forEach(mf -> {
      try {
        mf.setFieldValues(document, mail);
      } catch (Exception e) {
        LOG.error("Error setting field value {}, will ignore", mf, e);
      } catch (Throwable e) {
        LOG.error("Error indexing mails", e);
      }

    });
    for (Entry<String, String> h : mail.getHeaders().entrySet()) {
      String name = h.getKey().toLowerCase();
      String value = h.getValue();
      if (name.isEmpty()) continue;
      if (MailSchema.STATIC_FIELD_NAMES.contains(name)) continue;
      if (name.startsWith(" ")) {
        LOG.warn("Unknown header: {} -> {}", name, value);
        continue;
      }
      try {
        boolean found = false;
        for (IndexableField f : document.getFields(name)) {
          setValue((Field) f, value);
          found = true;
        }
        if (!found) {
          document.add(new TextField(name, value, Store.YES));
          document.add(new SortedDocValuesField(name, new BytesRef(value)));
        }
      } catch (Exception e) {
        LOG.error("Error indexing header: {} -> {}", name, value, e);
      }
    }
    return document;
  }

  public static RESTMail prepareTransferObject(Document doc) {
    RESTMail mail = new RESTMail();
    for (MailSchema mf : MailSchema.STATIC_FIELDS) {
      mf.retrieveValue(mail, doc);
    }
    Map<String, String> fieldMap = new HashMap<>();
    for (IndexableField f : doc.getFields()) {
      if (f.fieldType().stored() || MailSchema.STATIC_FIELD_NAMES.contains(f.name())) continue;
      fieldMap.put(f.name(), f.stringValue());
    }
    mail.headers = fieldMap;
    return mail;
  }

}