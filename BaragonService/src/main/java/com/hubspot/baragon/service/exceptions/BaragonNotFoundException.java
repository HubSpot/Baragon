package com.hubspot.baragon.service.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class BaragonNotFoundException extends WebApplicationException {

  /**
   * Create a HTTP 404 (Not Found) exception.
   */
  public BaragonNotFoundException() {
    super(Response.Status.NOT_FOUND);
  }

  /**
   * Create a HTTP 404 (Not Found) exception.
   * @param message the String that is the entity of the 404 response.
   */
  public BaragonNotFoundException(String message) {
    super(Response.status(Response.Status.NOT_FOUND).
      entity(message).type("text/plain").build());
  }
}
