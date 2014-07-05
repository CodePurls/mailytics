package com.codepurls.mailytics.api.v1.resources;

import static java.lang.String.format;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.hibernate.validator.constraints.NotEmpty;

import com.codepurls.mailytics.api.v1.transfer.Errors;
import com.codepurls.mailytics.service.security.UserService;

@Produces(MediaType.APPLICATION_JSON)
public class SecurityAPI {

  private final UserService userService;

  public SecurityAPI(UserService userService) {
    this.userService = userService;
  }

  public static class Visitor {
    @NotEmpty(message = " is required")
    public String user, password;
  }

  @POST
  @Path("login")
  public Response login(@Valid Visitor visitor) {
    String token = userService.authenticate(visitor.user, visitor.password);
    if (token == null) {
      return Response.status(Status.UNAUTHORIZED).entity(Errors.addTopLevelError("Invalid username or password")).build();
    } else {
      return Response.ok(format("{\"token\" : \"%s\"}", token)).build();
    }
  }

  public UserService getUserService() {
    return userService;
  }

}
