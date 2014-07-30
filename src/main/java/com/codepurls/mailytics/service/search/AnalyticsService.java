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
    FacetsCollector fc = new FacetsCollector();
    List<LongRange> rangeList = new ArrayList<>();
    rangeList.add(new LongRange(format("< %s-%s", res.name(), new Date(req.startTime)), 0, true, req.startTime, true));
    for (long l = req.startTime; l < req.endTime; l+= res.toMillis()) {
      rangeList.add(new LongRange(format("%s-%s", res.name(), new Date(l)), l, true, l + res.toMillis(), true));
    }
    rangeList.add(new LongRange(format("> %s-%s", res.name(), new Date(req.endTime)), req.endTime, true, Long.MAX_VALUE, true));

    Query query = searchService.getQuery(req);
    IndexSearcher searcher = new IndexSearcher(reader);
    FacetsCollector.search(searcher, query, req.pageSize, fc);

    LongRange[] ranges = rangeList.toArray(new LongRange[rangeList.size()]);
    Facets facets = new LongRangeFacetCounts(MailSchema.date.name(), fc, ranges);

    FacetResult result = facets.getTopChildren(10, MailSchema.date.name());
    Map<Long, Integer> countsByRes = new TreeMap<>();
    for (LabelAndValue labelAndValue : result.labelValues) {
      System.out.println(labelAndValue.label + " => " + labelAndValue.value);
    }
    return countsByRes;
  }

}
