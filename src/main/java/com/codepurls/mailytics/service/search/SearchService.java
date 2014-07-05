package com.codepurls.mailytics.service.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.api.v1.transfer.Page;
import com.codepurls.mailytics.api.v1.transfer.RESTMail;
import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.index.IndexingService;
import com.codepurls.mailytics.service.index.MailIndexer;
import com.codepurls.mailytics.service.index.MailIndexer.MailSchema;
import com.codepurls.mailytics.service.security.UserService;
import com.codepurls.mailytics.utils.StringUtils;

public class SearchService {
  private static final Logger   LOG = LoggerFactory.getLogger("SearchService");
  private final UserService     userService;
  private final IndexingService indexingService;

  public SearchService(IndexingService indexingService, UserService userService) {
    this.indexingService = indexingService;
    this.userService = userService;
  }

  public Page<RESTMail> search(User user, String query, int page, int size) {
    List<Integer> mbIds = userService.getMailboxes(user).stream().map((m) -> m.id).collect(Collectors.toList());
    return search(user, mbIds, query, page, size);
  }

  public Page<RESTMail> search(User user, List<Integer> mbIds, String query, int page, int size) {
    QueryParser qp = newQueryParser();
    try {
      Query q = StringUtils.isBlank(query) ? new MatchAllDocsQuery() : qp.parse(query);
      IndexReader reader = getReader(user, mbIds);
      IndexSearcher searcher = new IndexSearcher(reader);
      TopDocs topDocs = searcher.search(q, size);
      int totalHits = topDocs.totalHits;
      List<RESTMail> results = new ArrayList<>(Math.min(totalHits, size));
      if (totalHits > 0) {
        for (ScoreDoc sd : topDocs.scoreDocs) {
          Document match = searcher.doc(sd.doc);
          results.add(MailIndexer.prepareTransferObject(match));
        }
      }
      return Page.of(results, totalHits, page);
    } catch (ParseException e) {
      LOG.warn("Error parsing query {}", query, e);
    } catch (IOException e) {
      LOG.error("Error searching mailboxes with query {} ", query, e);
    }
    return Page.empty();
  }

  private IndexReader getReader(User user, List<Integer> mbIds) {
    Collection<Mailbox> mbs = userService.getMailboxes(user, mbIds);
    List<DirectoryReader> list = mbs.stream().map((mb) -> {
      try {
        return DirectoryReader.open(indexingService.getIndexDir(mb));
      } catch (Exception e) {
        LOG.error("Error retrieving dir for mailbox : {}", mb.name, e);
        return null;
      }
    }).filter((r) -> r != null).collect(Collectors.toList());
    return new MultiReader(list.toArray(new IndexReader[list.size()]), false);
  }

  private QueryParser newQueryParser() {
    return new QueryParser(indexingService.getVersion(), MailSchema.contents.name(), indexingService.getAnalyzer());
  }

}
