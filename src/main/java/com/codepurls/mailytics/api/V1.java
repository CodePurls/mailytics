package com.codepurls.mailytics.api;

import javax.ws.rs.Path;

import com.codepurls.mailytics.api.v1.resources.AnalyticsAPI;
import com.codepurls.mailytics.api.v1.resources.ManagementAPI;
import com.codepurls.mailytics.api.v1.resources.SearchAPI;
import com.codepurls.mailytics.api.v1.resources.SecurityAPI;
import com.codepurls.mailytics.service.security.UserService;

@Path("api/1")
public class V1 {
  private final UserService userService;

  public V1(UserService userService) {
    this.userService = userService;
  }

  @Path("secure")
  public SecurityAPI getSecurityAPI() {
    return new SecurityAPI(userService);
  }

  @Path("manage")
  public ManagementAPI getManagementAPI() {
    return new ManagementAPI(userService);
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
