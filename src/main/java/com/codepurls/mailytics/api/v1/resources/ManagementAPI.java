package com.codepurls.mailytics.api.v1.resources;

import static java.lang.String.format;
import io.dropwizard.auth.Auth;

import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import com.codepurls.mailytics.api.v1.transfer.Errors;
import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.index.IndexingService;
import com.codepurls.mailytics.service.security.UserService;

@Produces(MediaType.APPLICATION_JSON)
public class ManagementAPI {

  private final UserService userService;
  private final IndexingService indexingService;

  public ManagementAPI(UserService userService, IndexingService indexingService) {
    this.userService = userService;
    this.indexingService = indexingService;
  }

  @Path("mailboxes")
  @GET
  public Response getMailbox(@Auth User user) {
    return Response.ok(userService.getMailboxes(user)).build();
  }

  @PUT
  public Response addMailbox(@Auth User user, @Valid Mailbox mailbox) {
    mailbox.user = user;
    Mailbox created = userService.createMailbox(user, mailbox);
    created.user = user;
    return Response.created(UriBuilder.fromPath("manage/{id}").build(created.id)).entity(created).build();
  }

  @Path("mailboxes/{id}/index")
  @PUT
  public Response scheduleIndex(@Auth User user, @PathParam("id") int mailboxId) {
    Mailbox mb = userService.getMailbox(user, mailboxId);
    if(mb == null) {
      Errors errors = Errors.addTopLevelError(format("Mailbox with id %d was not found", mailboxId));
      return Response.status(Status.NOT_FOUND).entity(errors).build();
    }
    mb.user = user;
    indexingService.index(mb);
    return Response.ok("{ \"status\": \"Scheduled\"}").build();
  }
}
