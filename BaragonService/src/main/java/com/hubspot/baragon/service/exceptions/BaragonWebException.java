package com.hubspot.baragon.service.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.sun.jersey.api.Responses;

public class BaragonWebException extends WebApplicationException {

  /**
   * Create a HTTP 500 (Not Found) exception.
   */
  public BaragonWebException() {
    super(Responses.clientError().build());
  }

  /**
   * Create a HTTP 500 (Not Found) exception.
   * @param message the String that is the entity of the 404 response.
   */
  public BaragonWebException(String message) {
    super(Response.status(Responses.CLIENT_ERROR).
      entity(message).type("text/plain").build());
  }
}