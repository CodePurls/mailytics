package com.codepurls.mailytics.api;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import com.codepurls.mailytics.api.v1.resources.AnalyticsAPI;
import com.codepurls.mailytics.api.v1.resources.ManagementAPI;
import com.codepurls.mailytics.api.v1.resources.SearchAPI;
import com.codepurls.mailytics.api.v1.resources.SecurityAPI;
import com.sun.jersey.api.core.ResourceContext;

@Path("api/1")
public class V1 {
  @Context
  private ResourceContext resourceContext;

  @Path("secure")
  public SecurityAPI getSecurityAPI() {
    return resourceContext.getResource(SecurityAPI.class);
  }

  @Path("manage")
  public ManagementAPI getManagementAPI() {
    return resourceContext.getResource(ManagementAPI.class);
  }

  @Path("analytics")
  public AnalyticsAPI getAnalyticsAPI() {
    return resourceContext.getResource(AnalyticsAPI.class);
  }

  @Path("search")
  public SearchAPI getSearchAPI() {
    return resourceContext.getResource(SearchAPI.class);
  }
}
