package com.codepurls.mailytics.service.search;

import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.map.hash.TLongIntHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import com.codepurls.mailytics.data.search.Request;
import com.codepurls.mailytics.data.search.Request.Resolution;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.index.MailIndexer.MailSchema;
import com.codepurls.mailytics.service.security.UserService;

public class AnalyticsService {

  private final SearchService searchService;
  private final UserService   userService;

  public AnalyticsService(SearchService searchService, UserService userService) {
    this.searchService = searchService;
    this.userService = userService;
  }

  public Map<Long, Integer> getTrend(User user, Request req) throws ParseException, IOException {
    user = userService.validate(user);
    IndexReader reader = searchService.getReader(user, req.mailboxIds);
    Resolution res = req.resolution;
    Query query = searchService.getQuery(req);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopDocs docs = searcher.search(query, req.pageSize);
    TLongIntHashMap counts = new TLongIntHashMap();
    for (ScoreDoc sd : docs.scoreDocs) {
      long longValue = res.translate(searcher.doc(sd.doc).getField(MailSchema.date.name()).numericValue().longValue());
      counts.adjustOrPutValue(longValue, 1, 1);
    }
    Map<Long, Integer> countsByRes = new TreeMap<>();
    TLongIntIterator iterator = counts.iterator();
    while(iterator.hasNext()) {
      iterator.advance();
      long key = iterator.key();
      int value = iterator.value();
      countsByRes.put(key, value);
    }
    return countsByRes;
  }

}
