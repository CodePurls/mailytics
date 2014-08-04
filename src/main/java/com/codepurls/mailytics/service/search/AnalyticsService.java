package com.codepurls.mailytics.service.search;

import static java.lang.String.format;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Scanner;

import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.facet.range.LongRangeFacetCounts;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import com.codepurls.mailytics.data.search.Keywords;
import com.codepurls.mailytics.data.search.Request;
import com.codepurls.mailytics.data.search.Request.Resolution;
import com.codepurls.mailytics.data.search.WordAndCount;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.index.MailIndexer.MailSchema;
import com.codepurls.mailytics.service.security.UserService;
import com.codepurls.mailytics.utils.StringUtils;

public class AnalyticsService {

  private final SearchService searchService;
  private final UserService   userService;

  public AnalyticsService(SearchService searchService, UserService userService) {
    this.searchService = searchService;
    this.userService = userService;
  }

  /**
   * TODO: Facets are based on DocValue fields and not on separate taxonomy index, this will be slower but will take
   * much less memory. Provide option to support hierarchical facets support and add taxonomy index based faceting.
   * 
   * @param user
   * @param req
   * @return
   * @throws ParseException
   * @throws IOException
   */
  public Map<Long, Integer> getTrend(User user, Request req) throws ParseException, IOException {
    user = userService.validate(user);
    Facets facets = runFacetedSearch(req, user);
    FacetResult result = facets.getTopChildren(10, req.trendField.name());
    Map<Long, Integer> countsByRes = new LinkedHashMap<>();
    for (LabelAndValue labelAndValue : result.labelValues) {
      countsByRes.put(Long.valueOf(labelAndValue.label), labelAndValue.value.intValue());
    }
    return countsByRes;
  }

  public Map<String, WordAndCount[]> getHistogram(User user, Request request) throws ParseException, IOException {
    Map<String, TObjectIntHashMap<String>> results = new HashMap<>();
    IndexSearcher searcher = getSearcher(request, user);
    TopDocs topDocs = searcher.search(searchService.getQuery(request), 10000);
    for (ScoreDoc sd : topDocs.scoreDocs) {
      Document doc = searcher.doc(sd.doc);
      for (MailSchema r : request.histogramFields) {
        String value = doc.get(r.name());
        TObjectIntHashMap<String> counts = results.get(r.name());
        if (counts == null) {
          counts = new TObjectIntHashMap<>();
          results.put(r.name(), counts);
        }
        counts.adjustOrPutValue(value, 1, 1);
      }
    }
    Map<String, PriorityQueue<WordAndCount>> pqMap = new HashMap<>();
    for (Entry<String, TObjectIntHashMap<String>> e : results.entrySet()) {
      PriorityQueue<WordAndCount> pq = createPriorityQueue();
      pqMap.put(e.getKey(), pq);
      e.getValue().forEachEntry((a, b) -> pq.add(new WordAndCount(a, b)));
    }
    Map<String, WordAndCount[]> wcMap = new HashMap<>();
    for (Entry<String, PriorityQueue<WordAndCount>> e : pqMap.entrySet()) {
      WordAndCount[] wc = new WordAndCount[request.pageSize];
      for (int i = 0; i < request.pageSize; i++) {
        wc[i] = e.getValue().poll();
      }
      wcMap.put(e.getKey(), wc);
    }
    return wcMap;
  }

  public Keywords findKeywords(User user, Request request) throws ParseException, IOException {
    Path dumpFile = dumpSearchResults(user, request);
    Scanner scanner = new Scanner(dumpFile);
    TObjectIntHashMap<String> words = new TObjectIntHashMap<>();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      for (String word : StringUtils.tokenize(line, /* Remove stop words */true)) {
        if (request.query.contains(word)) continue;
        Integer cnt = words.get(word);
        if (cnt == null) {
          cnt = 1;
        } else {
          cnt++;
        }
        words.put(word, cnt);
      }
    }
    PriorityQueue<WordAndCount> pq = createPriorityQueue();
    words.forEachEntry((a, b) -> pq.add(new WordAndCount(a, b)));
    scanner.close();
    Keywords kw = new Keywords();
    WordAndCount[] wc = new WordAndCount[request.pageSize];
    for (int i = 0; i < wc.length; i++)
      wc[i] = pq.poll();
    kw.keywords = wc;
    return kw;
  }

  private PriorityQueue<WordAndCount> createPriorityQueue() {
    PriorityQueue<WordAndCount> pq = new PriorityQueue<>(new Comparator<WordAndCount>() {
      public int compare(WordAndCount o1, WordAndCount o2) {
        return Integer.compare(o2.count, o1.count);
      }
    });
    return pq;
  }

  private Path dumpSearchResults(User user, Request request) throws ParseException, IOException, FileNotFoundException {
    user = userService.validate(user);
    Query query = searchService.getQuery(request);
    IndexSearcher searcher = getSearcher(request, user);
    Filter f = NumericRangeFilter.newLongRange(MailSchema.date.name(), request.startTime, request.endTime, true, true);
    TopDocs docs = searcher.search(query, f, 10000);
    Path tempFile = Files.createTempFile(format("kw-%s-%s", user.id, request.keywordField), ".mailytics.temp");
    PrintWriter writer = new PrintWriter(tempFile.toFile());
    TIntHashSet dupSet = new TIntHashSet();
    for (ScoreDoc sd : docs.scoreDocs) {
      Document doc = searcher.doc(sd.doc);
      String string = doc.get(request.keywordField.name());
      int hash = string.hashCode();
      if (!dupSet.contains(hash)) {
        writer.println(string);
        dupSet.add(hash);
      }
    }
    dupSet.clear();
    writer.close();
    return tempFile;
  }

  private Facets runFacetedSearch(Request req, User user) throws IOException, ParseException {
    FacetsCollector fc = new FacetsCollector();
    Query query = searchService.getQuery(req);
    FacetsCollector.search(getSearcher(req, user), query, req.pageSize, fc);
    // TODO: support faceting for sortedsetdocvalue based fields (e.g. from)
    Facets facets = new LongRangeFacetCounts(req.trendField.name(), fc, buildRanges(req));
    return facets;
  }

  private IndexSearcher getSearcher(Request req, User user) {
    return new IndexSearcher(getReader(req, user));
  }

  private IndexReader getReader(Request req, User user) {
    IndexReader reader = searchService.getReader(user, req.mailboxIds);
    return reader;
  }

  private LongRange[] buildRanges(Request req) {
    List<LongRange> rangeList = new ArrayList<>();
    Resolution res = req.resolution;
    rangeList.add(new LongRange(Long.toString(req.startTime), 0, true, req.startTime, true));
    for (long l = req.startTime; l < req.endTime; l += res.toMillis()) {
      rangeList.add(new LongRange(Long.toString(l), l, true, l + res.toMillis(), true));
    }
    rangeList.add(new LongRange(Long.toString(req.endTime), req.endTime, true, Long.MAX_VALUE, true));
    LongRange[] ranges = rangeList.toArray(new LongRange[rangeList.size()]);
    return ranges;
  }

}
