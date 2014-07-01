package com.codepurls.mailytics.api.v1.resources;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codepurls.mailytics.service.security.UserService;

@Produces(MediaType.APPLICATION_JSON)
public class SecurityAPI {

  private final UserService userService;

  public SecurityAPI(UserService userService) {
    this.userService = userService;
  }
  
  public UserService getUserService() {
    return userService;
  }

}
