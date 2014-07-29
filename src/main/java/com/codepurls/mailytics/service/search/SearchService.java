package com.codepurls.mailytics.service.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queries.mlt.MoreLikeThisQuery;
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
import com.codepurls.mailytics.data.search.StoredQuery;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.dao.QueryLogDao;
import com.codepurls.mailytics.service.index.IndexingService;
import com.codepurls.mailytics.service.index.MailIndexer;
import com.codepurls.mailytics.service.index.MailIndexer.MailSchema;
import com.codepurls.mailytics.service.security.UserService;
import com.codepurls.mailytics.utils.StringUtils;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

public class SearchService {
  private static final Logger   LOG = LoggerFactory.getLogger("SearchService");
  private final UserService     userService;
  private final IndexingService indexingService;
  private final QueryLogDao     queryLog;

  public SearchService(IndexingService indexingService, UserService userService, QueryLogDao queryLog) {
    this.indexingService = indexingService;
    this.userService = userService;
    this.queryLog = queryLog;
  }

  public Page<RESTMail> search(User user, Request req) {
    try {
      Query q;
      if(!StringUtils.isBlank(req.query)) {
        long queryHash = Hashing.sha512().hashString(req.query, Charsets.UTF_8).asLong();
        StoredQuery query = queryLog.findByHash(queryHash);
        if(query == null) {
          queryLog.log(queryHash, req.query);
        }
      }
      q = getQuery(req);
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

  public Query getQuery(Request req) throws ParseException {
    Query q;
    boolean blankQuery = StringUtils.isBlank(req.query);
    if (blankQuery) {
      q = new MatchAllDocsQuery();
    } else if (req.similarFields != null) {
      MoreLikeThisQuery mlt = new MoreLikeThisQuery(req.query, req.similarFields.toArray(new String[0]), indexingService.getAnalyzer(),
          MailSchema.subject.name());
      mlt.setMinDocFreq(0);
      mlt.setMinTermFrequency(0);
      q = mlt;
    } else {
      q = newQueryParser().parse(req.query);
    }
    return q;
  }

  public IndexReader getReader(User user, List<Integer> mbIds) {
    Collection<Mailbox> mbs = userService.getMailboxes(user, mbIds);
    List<IndexReader> list = mbs.stream().map((mb) -> indexingService.getOrOpenReader(mb)).filter((m) -> m.isPresent()).map((m) -> m.get())
        .collect(Collectors.toList());
    return new MultiReader(list.toArray(new IndexReader[list.size()]), false);
  }

  private QueryParser newQueryParser() {
    return new QueryParser(indexingService.getVersion(), MailSchema.contents.name(), indexingService.getAnalyzer());
  }
}
