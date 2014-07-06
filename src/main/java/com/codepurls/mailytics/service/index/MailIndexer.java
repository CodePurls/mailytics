package com.codepurls.mailytics.service.index;

import static com.codepurls.mailytics.utils.StringUtils.orEmpty;

import java.util.Date;
import java.util.HashSet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.api.v1.transfer.RESTMail;
import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.core.Mailbox;

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
          ((Field) f).setLongValue(mail.getDate().getTime());
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.date = new Date(doc.getField(name()).numericValue().longValue());
      }
    },

    from {
      public Field[] getFields() {
        return new Field[] { new TextField(name(), "", Store.YES), new SortedDocValuesField(name(), new BytesRef("")) };
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
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getBody()));
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.body = doc.get(name());
      }
    },

    attachment_count {
      public Field[] getFields() {
        return new Field[] { new IntField(name(), 0, Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setIntValue(mail.getAttachments().size());
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.attachmentCount = doc.getField(name()).numericValue().intValue();
      }
    };
    public final static Set<String>  STATIC_FIELD_NAMES;
    public final static MailSchema[] STATIC_FIELDS;
    static {
      STATIC_FIELDS = MailSchema.values();
      STATIC_FIELD_NAMES = new HashSet<>();
      for (MailSchema mf : STATIC_FIELDS) {
        STATIC_FIELD_NAMES.add(mf.name());
      }
    }

    public abstract Field[] getFields();

    public abstract void setFieldValues(Document doc, Mail mail);

    public abstract void retrieveValue(RESTMail mail, Document doc);
  }

  private static final ThreadLocal<Document> TL_DOC = ThreadLocal.withInitial(() -> createDocument());
  private static final Logger                LOG    = LoggerFactory.getLogger("mail-indexer");

  public static void setValue(Field f, String value) {
    if (f instanceof SortedDocValuesField) f.setBytesValue(new BytesRef(value));
    else f.setStringValue(value);
  }

  public static Document createDocument() {
    Document doc = new Document();
    for (MailSchema sf : MailSchema.STATIC_FIELDS) {
      for (IndexableField indexableField : sf.getFields()) {
        doc.add(indexableField);
      }
    }
    return doc;
  }

  public static Document prepareDocument(Mailbox mb, Mail value) {
    Document document = TL_DOC.get();
    for (MailSchema mf : MailSchema.STATIC_FIELDS) {
      try {
        mf.setFieldValues(document, value);
      } catch (Exception e) {
        LOG.error("Error setting field value {}, will ignore", mf, e);
      }
    }
    for (Entry<String, String> h : value.getHeaders().entrySet()) {
      String name = h.getKey().toLowerCase();
      String val = h.getValue();
      if (name.isEmpty()) continue;
      if (MailSchema.STATIC_FIELD_NAMES.contains(name)) continue;
      if(name.startsWith(" ")) {
        LOG.warn("Unknown header: {} -> {}", name, val);
        continue;
      }
      
      boolean found = false;
      for (IndexableField f : document.getFields(name)) {
        setValue((Field) f, val);
        found = true;
      }
      if (!found) {
        document.add(new TextField(name, val, Store.YES));
        document.add(new SortedDocValuesField(name, new BytesRef(val)));
      }
    }
    return document;
  }

  public static RESTMail prepareTransferObject(Document doc) {
    RESTMail mail = new RESTMail();
    for (MailSchema mf : MailSchema.STATIC_FIELDS) {
      mf.retrieveValue(mail, doc);
    }
    return mail;
  }

}