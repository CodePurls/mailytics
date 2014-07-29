package com.codepurls.mailytics.api.v1.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.codepurls.mailytics.service.search.AnalyticsService;

@Produces(MediaType.APPLICATION_JSON)
public class AnalyticsAPI extends APIBase{

  @Context
  private AnalyticsService searchService;

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

  @GET
  @Path("trend")
  public Response getTrend() {
    return Response.ok().build();
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
