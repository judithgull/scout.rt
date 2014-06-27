/*******************************************************************************
 * Copyright (c) 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.admin.diagnostic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.LocaleThreadLocal;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.rt.shared.OfficialVersion;
import org.osgi.framework.Version;

public class DiagnosticSession {

  public void serviceRequest(HttpServletRequest req, HttpServletResponse res) throws IOException {
    if (!req.getParameterMap().isEmpty()) {
      for (Iterator paramIt = req.getParameterMap().entrySet().iterator(); paramIt.hasNext();) {
        Entry next = (Entry) paramIt.next();
        String action = (String) next.getKey();
        Object value = next.getValue();
        IDiagnostic diagnosticProvider = DiagnosticFactory.getDiagnosticProvider(action);
        if (diagnosticProvider != null && value instanceof Object[]) {
          diagnosticProvider.call(action, (Object[]) value);
        }
      }
    }

    String format = req.getParameter("format");
    if (CompareUtility.equals("xml", format)) {
      doXmlResponse(res);
    }
    else {
      doHtmlResponse(req, res);
    }
  }

  private void doXmlResponse(HttpServletResponse resp) throws IOException {
    List<List<String>> result = getDiagnosticItems();
    String diagnosticXML = getDiagnosticItemsXML(result);

    resp.setContentType("text/xml");
    ServletOutputStream out = resp.getOutputStream();
    out.println("<?xml version='1.0' encoding='UTF-8' ?>");
    out.println("<diagnosticsStatus>");
    out.println(diagnosticXML);
    out.println("</diagnosticsStatus>");
  }

  private void doHtmlResponse(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String errorMsg = "";

    /* run garbage collection for better estimation of current memory usage */
    String doGc = req.getParameter("gc");
    if (StringUtility.hasText(doGc)) {
      System.gc();
      errorMsg = "<font color='blue'> System.gc() triggered.</font>";
    }

    List<List<String>> result = getDiagnosticItems();

    IDiagnostic[] diagnosticServices = DiagnosticFactory.getDiagnosticProviders();
    for (IDiagnostic diagnosticService : diagnosticServices) {
      if (CollectionUtility.hasElements(diagnosticService.getPossibleActions())) {
        diagnosticService.addSubmitButtonsHTML(result);
      }
    }
    DiagnosticFactory.addDiagnosticItemToList(result, "System.gc()", "", "<input type='checkbox' name='gc' value='yes'/>");

    String diagnosticHTML = getDiagnosticItemsHTML(result);

    String title = "unknown";
    Version version = Version.emptyVersion;
    IProduct product = Platform.getProduct();
    if (product != null) {
      title = product.getName();
      version = Version.parseVersion("" + product.getDefiningBundle().getHeaders().get("Bundle-Version"));
    }

    resp.setContentType("text/html");
    ServletOutputStream out = resp.getOutputStream();
    out.println("<html>");
    out.println("<head>");
    out.println("<title>" + title + "</title>");
    out.println("<style>");
    out.println("body {font-family: sans-serif; font-size: 12; background-color : #F6F6F6;}");
    out.println("a,a:VISITED {color: #6666ff;text-decoration: none;}");
    out.println("table {font-size: 12; empty-cells: show;}");
    out.println("th {text-align: left;vertical-align: top; padding-left: 2; background-color : #cccccc;}");
    out.println("td {text-align: left;vertical-align: top; padding-left: 2;}");
    out.println("p {margin-top: 4; margin-bottom: 4; padding-top: 4; padding-bottom: 4;}");
    out.println("dt {font-weight: bold;}");
    out.println("dd {margin-left: 20px; margin-bottom: 3px;}");
    out.println(".copyright {font-size: 10;}");
    out.println("</style>");
    out.println("<script type=\"text/javascript\">");
    out.println("function toggle_visibility(id) {");
    out.println("   var el = document.getElementById(id);");
    out.println("   el.style.display = (el.style.display != 'none' ? 'none' : 'block');");
    out.println("}");
    out.println("</script>");
    out.println("</head>");
    out.println("<body>");
    out.println("<h3>" + title + " " + version + "</h3>");
    out.println("<form method='POST' action='" + StringUtility.join("?", req.getRequestURL().toString(), req.getQueryString()) + "'>");
    out.print(diagnosticHTML);
    out.println("<p><input type='submit' value='submit'/></p>");
    out.println("</form>");
    out.print(errorMsg);
    out.println("<p class=\"copyright\">&copy; " + OfficialVersion.COPYRIGHT + "</p>");
    out.println("</body>");
    out.println("</html>");
  }

  private List<List<String>> getDiagnosticItems() {
    List<List<String>> result = new ArrayList<List<String>>();

    /* system information from JVM */
    ArrayList<String> infos = getSystemInformation();
    DiagnosticFactory.addDiagnosticItemToList(result, "Server", "", DiagnosticFactory.STATUS_TITLE);
    DiagnosticFactory.addDiagnosticItemToList(result, "Runtime Environment", infos.get(0), DiagnosticFactory.STATUS_INFO);
    DiagnosticFactory.addDiagnosticItemToList(result, "Application Directory", infos.get(1), DiagnosticFactory.STATUS_INFO);
    DiagnosticFactory.addDiagnosticItemToList(result, "JVM Memory Status", "Max: " + infos.get(2) + ", Reserved: " + infos.get(3) + ", Currently Used: " + infos.get(4), DiagnosticFactory.STATUS_INFO);
    DiagnosticFactory.addDiagnosticItemToList(result, "JVM Locale", infos.get(11), DiagnosticFactory.STATUS_INFO);
    DiagnosticFactory.addDiagnosticItemToList(result, "Operating System", infos.get(6), DiagnosticFactory.STATUS_INFO);
    DiagnosticFactory.addDiagnosticItemToList(result, "Architecture", infos.get(5), DiagnosticFactory.STATUS_INFO);
    DiagnosticFactory.addDiagnosticItemToList(result, "#CPUs available to JVM", infos.get(12), DiagnosticFactory.STATUS_INFO);
    DiagnosticFactory.addDiagnosticItemToList(result, "OS Country / Timezone", infos.get(9) + " / " + infos.get(10), DiagnosticFactory.STATUS_INFO);
    DiagnosticFactory.addDiagnosticItemToList(result, "Host Address / Name", infos.get(13) + " / " + infos.get(14), DiagnosticFactory.STATUS_INFO);

    IDiagnostic[] diagnosticServices = DiagnosticFactory.getDiagnosticProviders();
    for (IDiagnostic diagnosticService : diagnosticServices) {
      diagnosticService.addDiagnosticItemToList(result);
    }

    // system properties
    List<String> properties = new ArrayList<String>();
    for (Object property : System.getProperties().keySet()) {
      properties.add(property + "");
    }
    Collections.sort(properties);
    String sysprops = "";
    sysprops += "<a href=\"#\" onClick=\"javascript:toggle_visibility('sysprops'); return false;\">(show / hide)</a>";
    sysprops += "<div id=\"sysprops\" style=\"width:600px; margin: 0px; padding: 0px; display: none; word-wrap: break-word;\">";
    sysprops += "<dl>";
    for (String property : properties) {
      sysprops += "<dt>" + property + ":</b></dt><dd>" + System.getProperty(property) + "</dd>";
    }
    sysprops += "</dl>";
    sysprops += "</div>";
    DiagnosticFactory.addDiagnosticItemToList(result, "System properties", sysprops, DiagnosticFactory.STATUS_INFO);

    // environment
    List<String> envKeys = new ArrayList<String>();
    for (String envKey : System.getenv().keySet()) {
      envKeys.add(envKey);
    }
    Collections.sort(envKeys);
    String envList = "";
    envList += "<a href=\"#\" onClick=\"javascript:toggle_visibility('env'); return false;\">(show / hide)</a>";
    envList += "<div id=\"env\" style=\"width:600px; margin: 0px; padding: 0px; display: none; word-wrap: break-word;\">";
    envList += "<dl>";
    for (String envKey : envKeys) {
      envList += "<dt>" + envKey + ":</b></dt><dd>" + System.getenv(envKey) + "</dd>";
    }
    envList += "</dl>";
    envList += "</div>";
    DiagnosticFactory.addDiagnosticItemToList(result, "Environment variables", envList, DiagnosticFactory.STATUS_INFO);

    DiagnosticFactory.addDiagnosticItemToList(result, "Version", "", DiagnosticFactory.STATUS_TITLE);
    Version v = Version.emptyVersion;
    IProduct product = Platform.getProduct();
    String productId = "n/a";
    String productName = "n/a";
    String application = "n/a";
    String definingBundle = "n/a";
    if (product != null) {
      productId = product.getId();
      productName = product.getName();
      application = product.getApplication();
      definingBundle = product.getDefiningBundle().getSymbolicName();
      v = Version.parseVersion("" + product.getDefiningBundle().getHeaders().get("Bundle-Version"));
    }
    DiagnosticFactory.addDiagnosticItemToList(result, "Product ID", productId, DiagnosticFactory.STATUS_INFO);
    DiagnosticFactory.addDiagnosticItemToList(result, "Product Name", productName, DiagnosticFactory.STATUS_INFO);
    DiagnosticFactory.addDiagnosticItemToList(result, "Application", application, DiagnosticFactory.STATUS_INFO);
    DiagnosticFactory.addDiagnosticItemToList(result, "Defining Bundle", definingBundle, DiagnosticFactory.STATUS_INFO);
    DiagnosticFactory.addDiagnosticItemToList(result, "Defining Bundle Version", v.toString(), DiagnosticFactory.STATUS_INFO);

    DiagnosticFactory.addDiagnosticItemToList(result, "Change values", "", DiagnosticFactory.STATUS_TITLE);
    return result;
  }

  private String getDiagnosticItemsXML(List<List<String>> result) {
    StringBuffer buf = new StringBuffer();
    for (List<String> status : result) {
      if (!CompareUtility.equals(DiagnosticFactory.STATUS_TITLE, status.get(2))) {
        buf.append("<status name='" + status.get(0) + "' status='" + status.get(2) + "'>");
        buf.append(status.get(1));
        buf.append("</status>");
      }
    }
    return buf.toString();
  }

  private String getDiagnosticItemsHTML(List<List<String>> diagnosticItems) {
    StringBuffer buf = new StringBuffer();
    buf.append("<table>");
    for (int i = 0; i < diagnosticItems.size(); i++) {
      List<String> item = diagnosticItems.get(i);
      String style = "";
      String status = item.get(2);
      if (DiagnosticFactory.STATUS_TITLE.equals(status)) {
        buf.append("<tr><td><b>" + item.get(0) + "&nbsp;&nbsp;</b></td><td></td><td></td>");
      }
      else {
        if (DiagnosticFactory.STATUS_OK.equals(status) || DiagnosticFactory.STATUS_ACTIVE.equals(status)) {
          style = " style =\"color:white;background-color:green\"";
        }
        else if (DiagnosticFactory.STATUS_FAILED.equals(status) || DiagnosticFactory.STATUS_INACTIVE.equals(status)) {
          style = " style =\"color:white;background-color:red\"";
        }
        else if (DiagnosticFactory.STATUS_INFO.equals(status)) {
          style = " style =\"color:white;background-color:blue\"";
        }
        buf.append("<tr><td style=\"background-color:lightgrey\">" + item.get(0) + "&nbsp;&nbsp;</td><td>" + item.get(1) + "&nbsp;&nbsp;</td><td" + style + "><b>" + item.get(2) + "</b></td>");
      }
    }
    buf.append("</table>");
    return buf.toString();
  }

  /**
   * Returns array list of strings with the following system information:
   * - java runtime
   * - application directory
   * - VM memory max
   * - VM memory currently reserved
   * - VM memory currently used
   * - OS name & version
   * - OS Architecture
   * - user name
   * - user home directory
   * - user country (OS)
   * - timezone (OS)
   * - locale
   * - number of available (logical) processors
   * - client host address
   * - client host name
   * 
   * @return ArrayList<String>
   */
  public static ArrayList<String> getSystemInformation() {
    Runtime rt = Runtime.getRuntime();
    ArrayList<String> result = new ArrayList<String>();

    result.add(System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") + ")");
    result.add(System.getProperty("user.dir"));

    DecimalFormat df = new DecimalFormat("0.00");
    result.add(df.format(rt.maxMemory() / 1048576f) + " MB");
    result.add(df.format(rt.totalMemory() / 1048576f) + " MB");
    result.add(df.format((rt.totalMemory() - rt.freeMemory()) / 1048576f) + " MB");
    result.add(System.getProperty("os.arch"));
    result.add(System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")");
    result.add(System.getProperty("user.name"));
    result.add(System.getProperty("user.home"));
    result.add(System.getProperty("user.country"));
    result.add(System.getProperty("user.timezone"));
    result.add(LocaleThreadLocal.get().getDisplayLanguage() + " (L) / " + LocaleThreadLocal.get().getLanguage() + " (F)");

    result.add(String.valueOf(rt.availableProcessors()));

    String hostname, ip = "";
    try {
      InetAddress addr = InetAddress.getLocalHost();
      ip = addr.getHostAddress();
      hostname = addr.getHostName();
    }
    catch (UnknownHostException e) {
      hostname = "Unknown";
      ip = "Unknown";
    }
    result.add(ip);
    result.add(hostname);

    return result;
  }
}
