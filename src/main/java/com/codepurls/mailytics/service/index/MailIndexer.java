package com.codepurls.mailytics.service.index;

import static com.codepurls.mailytics.utils.StringUtils.orEmpty;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    },
    date {
      public Field[] getFields() {
        return new Field[] { new LongField(name(), 0, Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((LongField) f).setLongValue(mail.getDate().getTime());
        }
      }
    },
    from {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getFrom()));
        }
      }
    },
    to {
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getTo()));
        }
      }
    },
    subject {
      public Field[] getFields() {
        return new Field[] { new TextField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(mail.getSubject()));
        }
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
    };
    public abstract Field[] getFields();

    public abstract void setFieldValues(Document doc, Mail mail);

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

}