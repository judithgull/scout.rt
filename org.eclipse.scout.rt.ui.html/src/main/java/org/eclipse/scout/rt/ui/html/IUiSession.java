/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.ui.html;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.scout.commons.resource.BinaryResource;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.platform.Bean;
import org.eclipse.scout.rt.ui.html.json.IJsonAdapter;
import org.eclipse.scout.rt.ui.html.json.JsonClientSession;
import org.eclipse.scout.rt.ui.html.json.JsonMessageRequestInterceptor;
import org.eclipse.scout.rt.ui.html.json.JsonRequest;
import org.eclipse.scout.rt.ui.html.json.JsonResponse;
import org.eclipse.scout.rt.ui.html.json.JsonStartupRequest;
import org.json.JSONObject;

@Bean
public interface IUiSession {

  /**
   * Prefix for name of HTTP session attribute that is used to store the associated {@link IUiSession}s.
   * <p>
   * The full attribute name is: <b><code>{@link #HTTP_SESSION_ATTRIBUTE_PREFIX} + uiSessionId</code></b>
   */
  String HTTP_SESSION_ATTRIBUTE_PREFIX = "scout.htmlui.uisession."/*+JsonRequest.PROP_UI_SESSION_ID*/;

  void init(HttpServletRequest request, JsonStartupRequest jsonStartupRequest);

  /**
   * Returns a reentrant lock that can be used to synchronize on the {@link IUiSession}.
   */
  ReentrantLock uiSessionLock();

  /**
   * All requests except the polling requests are calling this method from the {@link JsonMessageRequestInterceptor}
   * <p>
   * Note that {@link HttpSession#getLastAccessedTime()} is also updated on polling requests
   */
  void touch();

  /**
   * @return the last access time in millis since 01.01.1970 of a request, except polling requests
   *         <p>
   *         see {@link #touch()}
   */
  long getLastAccessedTime();

  void dispose();

  void logout();

  /**
   * @return the current UI response that is collecting changes for the next
   *         {@link #processJsonRequest(HttpServletRequest, JsonRequest)} cycle
   */
  JsonResponse currentJsonResponse();

  HttpServletRequest currentHttpRequest();

  HttpSession currentHttpSession();

  /**
   * @return a JSON object to send back to the client or <code>null</code> if an empty response shall be sent.
   */
  JSONObject processJsonRequest(HttpServletRequest httpReq, JsonRequest jsonReq);

  /**
   * @param httpRequest
   *          the HTTP request
   * @param targetAdapterId
   *          the ID of the adapter that receives the uploaded files
   * @param uploadResources
   *          list of uploaded files
   * @param uploadProperties
   *          a map of all other submitted string properties (usually not needed)
   * @return a JSON object to send back to the client or <code>null</code> if an empty response shall be sent.
   */
  JSONObject processFileUpload(HttpServletRequest httpReq, String targetAdapterId, List<BinaryResource> uploadResources, Map<String, String> uploadProperties);

  String getUiSessionId();

  String getClientSessionId();

  IClientSession getClientSession();

  JsonClientSession<? extends IClientSession> getJsonClientSession();

  IJsonAdapter<?> getRootJsonAdapter();

  /**
   * Returns an existing IJsonAdapter instance for the given adapter ID.
   */
  IJsonAdapter<?> getJsonAdapter(String id);

  /**
   * Returns an existing IJsonAdapter instance for the given model object.
   */
  <M, A extends IJsonAdapter<? super M>> A getJsonAdapter(M model, IJsonAdapter<?> parent);

  <M, A extends IJsonAdapter<? super M>> A getJsonAdapter(M model, IJsonAdapter<?> parent, boolean checkRoot);

  List<IJsonAdapter<?>> getJsonChildAdapters(IJsonAdapter<?> parent);

  /**
   * Creates a new initialized IJsonAdapter instance for the given model or returns an existing instance.
   * As a side-effect a newly created adapter is added to the current JSON response.
   */
  <M, A extends IJsonAdapter<? super M>> A getOrCreateJsonAdapter(M model, IJsonAdapter<?> parent);

  <M, A extends IJsonAdapter<? super M>> A createJsonAdapter(M model, IJsonAdapter<?> parent);

  String createUniqueIdFor(IJsonAdapter<?> adapter);

  void registerJsonAdapter(IJsonAdapter<?> adapter);

  void unregisterJsonAdapter(String id);

  boolean isInspectorHint();

  /**
   * Blocks the current thread/request until a model job started by a background job has terminated.
   */
  void waitForBackgroundJobs(int pollWaitSeconds);

  /**
   * Sends a "localeChanged" event to the UI. All locale-relevant data (number formats, texts map etc.) is sent along.
   */
  void sendLocaleChangedEvent(Locale locale);
}
