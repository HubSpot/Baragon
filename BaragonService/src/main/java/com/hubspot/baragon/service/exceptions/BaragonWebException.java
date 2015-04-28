package com.hubspot.baragon.service.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class BaragonWebException extends WebApplicationException {

  /**
   * Create a HTTP 500 (Not Found) exception.
   */
  public BaragonWebException() {
    super(Response.Status.INTERNAL_SERVER_ERROR);
  }

  /**
   * Create a HTTP 500 (Not Found) exception.
   * @param message the String that is the entity of the 404 response.
   */
  public BaragonWebException(String message) {
    super(Response.status(Response.Status.INTERNAL_SERVER_ERROR).
      entity(message).type("text/plain").build());
  }
}
