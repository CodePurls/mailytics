package com.codepurls.mailytics.api;

import javax.ws.rs.Path;

import com.codepurls.mailytics.api.v1.resources.AnalyticsAPI;
import com.codepurls.mailytics.api.v1.resources.ManagementAPI;
import com.codepurls.mailytics.api.v1.resources.SearchAPI;
import com.codepurls.mailytics.api.v1.resources.SecurityAPI;

@Path("1")
public class V1 {

  @Path("secure")
  public SecurityAPI getSecurityAPI() {
    return new SecurityAPI();
  }
  
  @Path("manage")
  public ManagementAPI getManagementAPI() {
    return new ManagementAPI();
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
