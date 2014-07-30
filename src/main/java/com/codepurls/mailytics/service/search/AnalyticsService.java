package com.codepurls.mailytics.service.search;

import static java.lang.String.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.facet.range.LongRangeFacetCounts;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

import com.codepurls.mailytics.data.search.Request;
import com.codepurls.mailytics.data.search.Request.Resolution;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.security.UserService;

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
  public Map<String, Integer> getTrend(User user, Request req) throws ParseException, IOException {
    user = userService.validate(user);
    Facets facets = runFacetedSearch(req, user);
    FacetResult result = facets.getTopChildren(10, req.trendField.name());
    Map<String, Integer> countsByRes = new TreeMap<>();
    for (LabelAndValue labelAndValue : result.labelValues) {
      countsByRes.put(labelAndValue.label, labelAndValue.value.intValue());
    }
    return countsByRes;
  }

  private Facets runFacetedSearch(Request req, User user) throws IOException, ParseException {
    IndexReader reader = searchService.getReader(user, req.mailboxIds);
    FacetsCollector fc = new FacetsCollector();
    Query query = searchService.getQuery(req);
    IndexSearcher searcher = new IndexSearcher(reader);
    FacetsCollector.search(searcher, query, req.pageSize, fc);
    Facets facets = new LongRangeFacetCounts(req.trendField.name(), fc, buildRanges(req));
    return facets;
  }

  private LongRange[] buildRanges(Request req) {
    List<LongRange> rangeList = new ArrayList<>();
    Resolution res = req.resolution;
    rangeList.add(new LongRange(format("%s or older", new Date(req.startTime)), 0, true, req.startTime, true));
    for (long l = req.startTime; l < req.endTime; l += res.toMillis()) {
      rangeList.add(new LongRange(format("%s", new Date(l)), l, true, l + res.toMillis(), true));
    }
    rangeList.add(new LongRange(format("%s or newer", new Date(req.endTime)), req.endTime, true, Long.MAX_VALUE, true));
    LongRange[] ranges = rangeList.toArray(new LongRange[rangeList.size()]);
    return ranges;
  }

}
