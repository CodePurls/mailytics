package com.codepurls.mailytics.service.index;

import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.lucene49.Lucene49Codec;
import org.apache.lucene.codecs.pulsing.Pulsing41PostingsFormat;

import com.codepurls.mailytics.service.index.MailIndexer.MailSchemaField;

public class MailyticsIndexCodec extends Lucene49Codec {
  private Pulsing41PostingsFormat pulsingFormat = new Pulsing41PostingsFormat();
  public PostingsFormat getPostingsFormatForField(String field) {
    if (MailSchemaField.STATIC_FIELD_NAMES.contains(field)) {
      MailSchemaField f = MailSchemaField.valueOf(field);
      if (f == MailSchemaField.date) { return pulsingFormat; }
    }
    return super.getPostingsFormatForField(field);
  }

}
