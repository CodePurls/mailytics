package com.codepurls.mailytics.service.index;

import static com.codepurls.mailytics.utils.StringUtils.orEmpty;
import static com.codepurls.mailytics.utils.StringUtils.toPlainText;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.api.v1.transfer.RESTAttachment;
import com.codepurls.mailytics.api.v1.transfer.RESTMail;
import com.codepurls.mailytics.data.core.Attachment;
import com.codepurls.mailytics.data.core.Mail;
import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.utils.RFC822Constants;
import com.codepurls.mailytics.utils.StringUtils;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

public class MailIndexer {
  private static final int MAX_DOC_VALUE_SIZE = 32766;

  public interface SchemaField{
    public String name();
    static SchemaField resolve(String name) {
      if(MailSchemaField.STATIC_FIELD_NAMES.contains(name))
        return MailSchemaField.valueOf(name);
      return () -> name;
    }
  }
  public enum MailSchemaField implements SchemaField{
    id {
      public Field[] getFields() {
        return new Field[] { new LongField(name(), 0L, Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        long id = Hashing.murmur3_128().hashString((mail.getDate().toString() +  mail.getFrom() + mail.getSubject()), Charsets.UTF_8).asLong();
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setLongValue(id);
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.id = doc.get(name());
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
        return new Field[] { new StringField(name(), "", Store.YES), new StringField("charset", "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        String contentType = orEmpty(mail.getHeaders().get(RFC822Constants.CONTENT_TYPE));
        for (String val : contentType.split(";")) {
          val = val.trim().toLowerCase();
          if(val.startsWith("boundary") || val.startsWith("format")) {
            continue;
          }else if(val.startsWith("charset")) {
            String cs = val.split("=")[1];
            if(cs.startsWith("\"")) {
              cs = cs.substring(1);
            }
            if(cs.endsWith("\"")) {
              cs = cs.substring(0, cs.lastIndexOf("\""));
            }
            cs=cs.toLowerCase();
            ((Field) doc.getField("charset")).setStringValue(cs);
          }else {
            ((Field) doc.getField(name())).setStringValue(val);
          }
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
          ((Field) f).setLongValue(mail.getDate().getTime());
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.date = new Date(doc.getField(name()).numericValue().longValue());
        mail.dateString = mail.date.toInstant().toString();
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
          for (String to : mail.getTo()) {
            ((Field) f).setStringValue(orEmpty(to));
          }
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.to = doc.get(name());
      }
    },
    
    cc {
      public Field[] getFields() {
        return new Field[] { new TextField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          for (String cc : mail.getCc()) {
            ((Field) f).setStringValue(orEmpty(cc));
          }
        }
      }
      public void retrieveValue(RESTMail mail, Document doc) {
        mail.cc = doc.get(name());
      }
    },
    
    bcc {
      public Field[] getFields() {
        return new Field[] { new TextField(name(), "", Store.YES) };
      }
      public void setFieldValues(Document doc, Mail mail) {
        for (IndexableField f : doc.getFields(name())) {
          for (String bcc : mail.getBcc()) {
            ((Field) f).setStringValue(orEmpty(bcc));
          }
        }
      }
      public void retrieveValue(RESTMail mail, Document doc) {
        mail.bcc = doc.get(name());
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
    
    thread_topic{
      public Field[] getFields() {
        return new Field[] { new TextField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        String topic = orEmpty(mail.getHeaders().get("thread-topic"));
        for (IndexableField f : doc.getFields(name())) {
          setValue((Field) f, topic);
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.thread_topic = doc.get(name());
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
    
    language{
      public Field[] getFields() {
        return new Field[] { new StringField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        String lang = orEmpty(mail.getHeaders().get("content-language"));
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(lang);
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.language = doc.get(name());
      }
    },
    user_agent{
      public Field[] getFields() {
        return new Field[] { new TextField(name(), "", Store.YES) };
      }

      public void setFieldValues(Document doc, Mail mail) {
        String ua = mail.getHeaders().get("user-agent");
        if(StringUtils.isBlank(ua)) {
          ua = mail.getHeaders().get("x-mailer");
        }
        for (IndexableField f : doc.getFields(name())) {
          ((Field) f).setStringValue(orEmpty(ua));
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.userAgent = doc.get(name());
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
        int i = 0;
        for (Attachment attachment : attachments) {
          try {
            doc.add(new TextField(i + "_" + name(), new Tika().parse(attachment.getStream())));
            doc.add(new StringField(i + "_" + name() + "_content_type", attachment.getMediaType(), Store.YES));
            doc.add(new StringField(i + "_" + name() + "_name", attachment.getName(), Store.YES));
            doc.add(new LongField(i + "_" + name() + "_size", attachment.getSize(), Store.YES));
          } catch (Exception e) {
            LOG.warn("Error parsing attachment {}", attachment.getName(), e);
          }
          i++;
        }
      }

      public void retrieveValue(RESTMail mail, Document doc) {
        mail.attachmentCount = doc.getField(ATTACHMENT_COUNT).numericValue().intValue();
        RESTAttachment[] attArr = new RESTAttachment[mail.attachmentCount];
        for (int i = 0; i < mail.attachmentCount; i++) {
          RESTAttachment att = new RESTAttachment();
          att.name = doc.get(i + "_" + name() + "_name");
          att.type = doc.get(i + "_" + name() + "_content_type");
          att.size = doc.getField(i + "_" + name() + "_size").numericValue().longValue();
          attArr[i] = att;
        }
        mail.attachments = Arrays.asList(attArr);
      }

    };
    public final static Set<String>  STATIC_FIELD_NAMES;
    public final static MailSchemaField[] STATIC_FIELDS;
    static {
      STATIC_FIELDS = MailSchemaField.values();
      Arrays.sort(STATIC_FIELDS);
      STATIC_FIELD_NAMES = new HashSet<>();
      for (MailSchemaField mf : STATIC_FIELDS) {
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
      f.setBytesValue(new BytesRef(value.substring(0, Math.min(MAX_DOC_VALUE_SIZE, value.length()))));
    } else {
      f.setStringValue(value);
    }
  }

  public static Document createDocument() {
    Document doc = new Document();
    Arrays.stream(MailSchemaField.STATIC_FIELDS).forEach(sf -> Arrays.stream(sf.getFields()).forEach(f -> doc.add(f)));
    return doc;
  }

  public static Document prepareDocument(Mailbox mb, Mail mail) {
    Date date = mail.getDate();
    if (date == null) {
      LOG.warn("Null date for mail: {}, folder: {}, this email will not be indexed.", mail.getSubject(), mail.getFolder().getName());
      return null;
    }
    Document document = createDocument();
    Arrays.stream(MailSchemaField.STATIC_FIELDS).forEach(mf -> {
      try {
        mf.setFieldValues(document, mail);
      } catch (Exception e) {
        LOG.error("Error setting field value {}, will ignore", mf, e);
      }
    });
    
    mail.getHeaders().entrySet().stream()
    .filter(e-> !e.getKey().isEmpty() && !e.getKey().startsWith(" ") && !MailSchemaField.STATIC_FIELD_NAMES.contains(e.getKey()))
    .forEach(e->{
      String name = e.getKey().toLowerCase();
      String value = e.getValue().substring(0, Math.min(e.getValue().length(), MAX_DOC_VALUE_SIZE));
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
      } catch (Exception ex) {
        LOG.error("Error indexing header: {} -> {}", name, value, ex);
      }

    });
    return document;
  }

  public static RESTMail prepareTransferObject(Document doc) {
    RESTMail mail = new RESTMail();
    for (MailSchemaField mf : MailSchemaField.STATIC_FIELDS) {
      mf.retrieveValue(mail, doc);
    }
    Map<String, String> fieldMap = new HashMap<>();
    for (IndexableField f : doc.getFields()) {
      if (MailSchemaField.STATIC_FIELD_NAMES.contains(f.name())) continue;
      fieldMap.put(f.name(), f.stringValue());
    }
    mail.headers = fieldMap;
    return mail;
  }

}