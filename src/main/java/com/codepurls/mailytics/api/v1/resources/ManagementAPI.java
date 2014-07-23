package com.codepurls.mailytics.api.v1.resources;

import static java.lang.String.format;
import io.dropwizard.auth.Auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import com.codepurls.mailytics.api.v1.transfer.Errors;
import com.codepurls.mailytics.data.core.Mailbox;
import com.codepurls.mailytics.data.core.Mailbox.MailboxStatus;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.index.IndexingService;
import com.codepurls.mailytics.service.security.UserService;

@Produces(MediaType.APPLICATION_JSON)
public class ManagementAPI {

  @Context
  private UserService     userService;
  @Context
  private IndexingService indexingService;

  @Auth(required = false)
  private User            user;

  @Path("mailboxes")
  @GET
  public Response getMailbox() {
    return Response.ok(userService.getMailboxes(user)).build();
  }

  @PUT
  public Response addMailbox(@Valid Mailbox mailbox) {
    mailbox.user = user;
    Mailbox created = userService.createMailbox(user, mailbox);
    created.user = user;
    return Response.created(UriBuilder.fromPath("{id}").build(created.id)).entity(created).build();
  }

  @Path("mailboxes/index")
  @PUT
  public Response indexAll() {
    Collection<Mailbox> mailboxes = userService.getMailboxes(user);
    List<String> messages = new ArrayList<>(mailboxes.size());
    for (Mailbox mailbox : mailboxes) {
      userService.updateMailboxStatus(mailbox, MailboxStatus.NEW);
      Response response = scheduleIndex(mailbox.id);
      if (response.getStatus() != Status.OK.getStatusCode()) {
        messages.add(((Errors) response.getEntity()).errors.get(0).message);
      } else {
        messages.add(format("{ \"status\": \"Mailbox '%s' scheduled for indexing\"}", mailbox.name));
      }
    }
    return Response.ok(messages).build();
  }

  @Path("mailboxes/{id}/index")
  @PUT
  public Response scheduleIndex(@PathParam("id") int mailboxId) {
    user = userService.validate(user);
    Mailbox mb = userService.getMailbox(user, mailboxId);
    if (mb == null) { return mbNotfoundResponse(mailboxId); }
    if (mb.status == MailboxStatus.INDEXED) {
      Errors errors = Errors.addTopLevelError(format("Mailbox with id %d is already indexed", mailboxId));
      return Response.status(Status.NOT_MODIFIED).entity(errors).build();
    }
    mb.user = user;
    indexingService.index(mb);
    return Response.ok(format("{ \"status\": \"Mailbox '%s' scheduled for indexing\"}", mb.name)).build();
  }

  @Path("mailboxes/{id}/reindex")
  @PUT
  public Response scheduleReindex(@PathParam("id") int mailboxId) throws IOException {
    user = userService.validate(user);
    Mailbox mb = userService.getMailbox(user, mailboxId);
    if (mb == null) { return mbNotfoundResponse(mailboxId); }
    if (mb.status == MailboxStatus.INDEXING) {
      Errors errors = Errors.addTopLevelError(format("Mailbox with id %d is being indexed", mailboxId));
      return Response.status(Status.BAD_REQUEST).entity(errors).build();
    }
    mb.user = user;
    indexingService.reindex(mb);
    return Response.ok(format("{ \"status\": \"Mailbox '%s' scheduled for re-indexing\"}", mb.name)).build();
  }

  private Response mbNotfoundResponse(int mailboxId) {
    Errors errors = Errors.addTopLevelError(format("Mailbox with id %d was not found", mailboxId));
    return Response.status(Status.NOT_FOUND).entity(errors).build();
  }
}
