package com.codepurls.mailytics.api.v1.resources;

import javax.validation.Valid;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.codepurls.mailytics.data.core.Mailbox;

@Produces(MediaType.APPLICATION_JSON)
public class ManagementAPI {

  @PUT
  public Response addMailbox(@Valid Mailbox mailbox) {
    return Response.ok().build();
  }
}
