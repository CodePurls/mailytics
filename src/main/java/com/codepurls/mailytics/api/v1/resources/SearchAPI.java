package com.codepurls.mailytics.api.v1.resources;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.codepurls.mailytics.service.search.SearchService;

@Produces(MediaType.APPLICATION_JSON)
public class SearchAPI extends APIBase {
  @Context
  protected SearchService searchService;

  @GET
  @Path("all")
  public Response searchAll() {
    return Response.ok(searchService.search(user, createRequest(Collections.emptyList()))).build();
  }

  @GET
  @Path("mailbox")
  public Response searchMBox(@QueryParam("id") List<Integer> mbIds) {
    return Response.ok(searchService.search(user, createRequest(mbIds))).build();
  }

}
