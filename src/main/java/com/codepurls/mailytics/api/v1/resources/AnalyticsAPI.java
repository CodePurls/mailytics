package com.codepurls.mailytics.api.v1.resources;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.lucene.queryparser.classic.ParseException;

import com.codepurls.mailytics.data.search.Request;
import com.codepurls.mailytics.service.search.AnalyticsService;

import edu.emory.mathcs.backport.java.util.Collections;

@Produces(MediaType.APPLICATION_JSON)
public class AnalyticsAPI extends APIBase{

  @Context
  private AnalyticsService analyticsService;

  @GET
  @Path("entities")
  public Response getEntities() {
    return Response.ok().build();
  }

  @GET
  @Path("keywords")
  public Response getKeywords() {
    return Response.ok().build();
  }

  @SuppressWarnings("unchecked")
  @GET
  @Path("trend")
  public Response getTrend() throws ParseException, IOException {
    Request request = createRequest(Collections.emptyList());
    Map<Long, Integer> trend = analyticsService.getTrend(user, request);
    Map<String, Integer> response = new LinkedHashMap<>();
    for (Entry<Long, Integer> e : trend.entrySet()) {
      if(e.getValue() == 0)
        continue;
      response.put(request.resolution.format(e.getKey()), e.getValue());
    }
// TODO: fix problem with following
//    Map<String, Integer> response = trend.entrySet().stream()
//    .filter(e-> e.getValue() > 0)
//    .limit(request.pageSize)
//    .collect(Collectors.toMap(k -> request.resolution.format(k.getKey()), v -> v.getValue()));
    return Response.ok(response).build();
  }

  @GET
  @Path("summerize")
  public Response getSummary() {
    return Response.ok().build();
  }

  @GET
  @Path("activity")
  public Response getActivity() {
    return Response.ok().build();
  }

  @GET
  @Path("network")
  public Response getNetwork() {
    return Response.ok().build();
  }

  @GET
  @Path("segments/geo")
  public Response getGeoSegments() {
    return Response.ok().build();
  }

  @GET
  @Path("segments/gender")
  public Response getGenderSegments() {
    return Response.ok().build();
  }

  @GET
  @Path("segments/ua")
  public Response getUserAgentSegments() {
    return Response.ok().build();
  }

}
