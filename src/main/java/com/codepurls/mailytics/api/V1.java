package com.codepurls.mailytics.api;

import javax.ws.rs.Path;

import com.codepurls.mailytics.api.v1.AnalyticsAPI;
import com.codepurls.mailytics.api.v1.SearchAPI;
import com.codepurls.mailytics.api.v1.SecurityAPI;

@Path("1")
public class V1 {

  @Path("secure")
  public SecurityAPI getSecurityAPI() {
    return new SecurityAPI();
  }

  @Path("analytics")
  public AnalyticsAPI getAnalyticsAPI() {
    return new AnalyticsAPI();
  }

  @Path("search")
  public SearchAPI getSearchAPI() {
    return new SearchAPI();
  }
}
