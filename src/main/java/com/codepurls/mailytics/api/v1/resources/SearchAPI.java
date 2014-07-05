package com.codepurls.mailytics.api.v1.resources;

import io.dropwizard.auth.Auth;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.search.SearchService;

@Produces(MediaType.APPLICATION_JSON)
public class SearchAPI {
  private static final String PARAM_QUERY        = "q";
  private static final String PARAM_PAGE         = "page";
  private static final String PARAM_SIZE         = "size";
  private static final String PARAM_DEFAULT_PAGE = "1";
  private static final String PARAM_DEFAULT_SIZE = "10";
  private final SearchService searchService;

  public SearchAPI(SearchService searchService) {
    this.searchService = searchService;
  }

  @GET
  public Response searchAll(@Auth User user, @QueryParam(PARAM_QUERY) String query,
      @DefaultValue(PARAM_DEFAULT_SIZE) @QueryParam(PARAM_SIZE) int size, @DefaultValue(PARAM_DEFAULT_PAGE) @QueryParam(PARAM_PAGE) int page) {
    return Response.ok(searchService.search(user, query, page, size)).build();
  }

  @GET
  public Response searchMBox(@Auth User user, @QueryParam(PARAM_QUERY) String query, @QueryParam("mboxes") List<Integer> mbIds,
      @DefaultValue(PARAM_DEFAULT_SIZE) @QueryParam(PARAM_SIZE) int size, @DefaultValue(PARAM_DEFAULT_PAGE) @QueryParam(PARAM_PAGE) int page) {
    return Response.ok(searchService.search(user, mbIds, query, page, size)).build();
  }
}
