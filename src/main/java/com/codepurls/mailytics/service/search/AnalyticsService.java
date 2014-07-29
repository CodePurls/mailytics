package com.codepurls.mailytics.service.search;

import com.codepurls.mailytics.service.security.UserService;

public class AnalyticsService {

  private final SearchService searchService;
  private final UserService   userService;

  public AnalyticsService(SearchService searchService, UserService userService) {
    this.searchService = searchService;
    this.userService = userService;
  }

}
