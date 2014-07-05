package com.codepurls.mailytics.service.index;

import static com.codepurls.mailytics.utils.StringUtils.orEmpty;

import java.util.Date;

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
    }
    ;
    
    protected void setValue(Field f, String value) {
      if(f instanceof SortedDocValuesField)
        f.setBytesValue(new BytesRef(value));
      else
        f.setStringValue(value);
    }
    public abstract Field[] getFields();

    public abstract void setFieldValues(Document doc, Mail mail);

    public abstract void retrieveValue(RESTMail mail, Document doc);
  }

  private static final ThreadLocal<Document> TL_DOC = ThreadLocal.withInitial(() -> createDocument());
  private static final Logger                LOG    = LoggerFactory.getLogger("mail-indexer");

  public static Document createDocument() {
    Document doc = new Document();
    for (MailSchema sf : MailSchema.values()) {
      for (IndexableField indexableField : sf.getFields()) {
        doc.add(indexableField);
      }
    }
    return doc;
  }

  public static Document prepareDocument(Mailbox mb, Mail value) {
    Document document = TL_DOC.get();
    for (MailSchema mf : MailSchema.values()) {
      try {
        mf.setFieldValues(document, value);
      } catch (Exception e) {
        LOG.error("Error setting field value {}, will ignore", mf, e);
      }
    }
    return document;
  }

  public static RESTMail prepareTransferObject(Document doc) {
    RESTMail mail = new RESTMail();
    for (MailSchema mf : MailSchema.values()) {
      mf.retrieveValue(mail, doc);
    }
    return mail;
  }

}