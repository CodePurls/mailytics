package com.codepurls.mailytics.api.v1.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.security.UserService;
import com.google.common.base.Optional;

public class MailyticsAuthenticator implements Authenticator<String, User> {

  private UserService userService;

  public MailyticsAuthenticator(UserService userService) {
    this.userService = userService;
  }

  @Override
  public Optional<User> authenticate(String credentials) throws AuthenticationException {
    return userService.findUser(credentials);
  }

}
