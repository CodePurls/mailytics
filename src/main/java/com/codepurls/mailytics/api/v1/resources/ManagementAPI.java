package com.codepurls.mailytics.api.v1.resources;

import io.dropwizard.auth.Auth;

import javax.validation.Valid;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.security.UserService;

@Produces(MediaType.APPLICATION_JSON)
public class ManagementAPI {

  private final UserService userService;

  public ManagementAPI(UserService userService) {
    this.userService = userService;
  }

  @PUT
  public Response addMailbox(@Auth User user, @Valid Mailbox mailbox) {
    mailbox.user = user;
    userService.createMailbox(user, mailbox);
    return Response.created(UriBuilder.fromResource(ManagementAPI.class).build()).build();
  }
}
