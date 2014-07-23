package com.codepurls.mailytics.service.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.api.v1.transfer.Page;
import com.codepurls.mailytics.api.v1.transfer.RESTMail;
import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.data.search.Request;
import com.codepurls.mailytics.data.search.Request.SortDirecton;
import com.codepurls.mailytics.data.search.Request.SortType;
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

  public Page<RESTMail> search(User user, Request req) {
    if (req.mailboxIds == null || req.mailboxIds.isEmpty()) {
      req.mailboxIds = userService.getMailboxes(user).stream().map((m) -> m.id).collect(Collectors.toList());
    }
    QueryParser qp = newQueryParser();
    try {
      Query q = StringUtils.isBlank(req.query) ? new MatchAllDocsQuery() : qp.parse(req.query);
      IndexReader reader = getReader(user, req.mailboxIds);
      IndexSearcher searcher = new IndexSearcher(reader);
      SortType srt = req.sort;
      boolean reverse = req.sortDir == SortDirecton.DESC;
      TopDocs topDocs = searcher.search(q, req.pageSize, new Sort(new SortField(srt.getSortField(), srt.getValueType(), reverse)));
      int totalHits = topDocs.totalHits;
      if (totalHits > 0) {
        List<RESTMail> results = Arrays.stream(topDocs.scoreDocs).map((sd) -> {
          try {
            return MailIndexer.prepareTransferObject(searcher.doc(sd.doc));
          } catch (IOException e) {
            LOG.error("Error retrieving mail", e);
            return null;
          }
        }).filter((x) -> x != null).collect(Collectors.toList());
        return Page.of(results, totalHits, req.pageNum);
      }
    } catch (ParseException e) {
      LOG.warn("Error parsing query {}", req.query, e);
    } catch (IOException e) {
      LOG.error("Error searching mailboxes with query {} ", req.query, e);
    }
    return Page.empty();
  }

  private IndexReader getReader(User user, List<Integer> mbIds) {
    Collection<Mailbox> mbs = userService.getMailboxes(user, mbIds);
    List<IndexReader> list = mbs.stream()
        .map((mb) -> indexingService.getOrOpenReader(mb))
        .filter((m) -> m.isPresent())
        .map((m) -> m.get())
        .collect(Collectors.toList());
    return new MultiReader(list.toArray(new IndexReader[list.size()]), false);
  }

  private QueryParser newQueryParser() {
    return new QueryParser(indexingService.getVersion(), MailSchema.contents.name(), indexingService.getAnalyzer());
  }

}
