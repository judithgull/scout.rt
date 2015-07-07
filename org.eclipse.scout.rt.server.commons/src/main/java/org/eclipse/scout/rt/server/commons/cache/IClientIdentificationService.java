package org.eclipse.scout.rt.server.commons.cache;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.scout.rt.platform.service.IService;

/**
 * This service identifies the requesting client
 *
 * @since 4.0.0
 */
// TODO[aho] remove
public interface IClientIdentificationService extends IService {

  /**
   * returns the session id of the HTTP-Request. If no session id is set a new id will be generated and set.
   *
   * @param req
   *          HttpServletRequest
   * @param res
   *          HttpServletResponse
   * @return the session id
   */
  String getClientId(HttpServletRequest req, HttpServletResponse res);
}
