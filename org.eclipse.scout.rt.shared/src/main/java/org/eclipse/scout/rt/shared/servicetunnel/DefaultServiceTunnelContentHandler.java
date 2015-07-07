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
package org.eclipse.scout.rt.shared.servicetunnel;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.eclipse.scout.commons.Base64Utility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.serialization.IObjectSerializer;
import org.eclipse.scout.commons.serialization.SerializationUtility;
import org.eclipse.scout.rt.platform.config.CONFIG;
import org.eclipse.scout.rt.shared.SharedConfigProperties.CompressServiceTunnelRequestProperty;

/**
 * Creates SOAP envelopes for {@link IServiceTunnelRequest} and {@link IServiceTunnelResponse} objects.<br>
 * This fast hi-speed encoder/decoder ignores xml structure and reads content of first &lt;data&gt; tag directly.
 * <p>
 * Example request:
 *
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <soapenv:Envelope soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
 *   <soapenv:Body>
 *     <request version="3.0.0" format="de_CH" language="de_CH" service="org.eclipse.scout.rt.shared.services.common.ping.IPingService" operation="ping"/>
 *     <data>...</data>
 *     <info ts="20080715114301917" origin="192.168.1.105">For maximum performance, data is reduced, compressed and base64 encoded.</info>
 *   </soapenv:Body>
 * </soapenv:Envelope>
 * }
 * </pre>
 * <p>
 * Example response (success):
 *
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <soapenv:Envelope soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
 *   <soapenv:Body>
 *     <response status="OK" type="String"/>
 *     <data>...</data>
 *     <info ts="20080715114301917" origin="192.168.3.2">For maximum performance, data is reduced, compressed and base64 encoded.</info>
 *   </soapenv:Body>
 * </soapenv:Envelope>
 * }
 * </pre>
 *
 * Example response (error):
 *
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <soapenv:Envelope soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
 *   <soapenv:Body>
 *     <response status="ERROR">
 *       <exception type="SecurityException">Access denied</exception>
 *     </response>
 *     <data>...</data>
 *     <info ts="20080715114301917" origin="192.168.3.2">For maximum performance, data is reduced, compressed and base64 encoded.</info>
 *   </soapenv:Body>
 * </soapenv:Envelope>
 * }
 * </pre>
 *
 * In order to enable/disable content compression, use the system property or config.properties property:
 * <code>org.eclipse.scout.serviceTunnel.compress=true</code>
 * <p>
 * If the client side sets this property to true, all client data is sent to the server with compressed data (false
 * accordingly).<br>
 * If the client side does not set this property at all, data is sent to server in the same mode as it is received from
 * the server.<br>
 * Same is valid for the server side.
 * <p>
 * The default is true.
 * <p>
 * To enable debug output only for this class, use logger specific parameters.
 */
public class DefaultServiceTunnelContentHandler implements IServiceTunnelContentHandler {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(DefaultServiceTunnelContentHandler.class);
  private static final Pattern BEGIN_DATA_TAG = Pattern.compile("[<]([a-zA-Z0-9]+:)?data\\s*>");
  private static final Pattern END_DATA_TAG = Pattern.compile("[<][/]([a-zA-Z0-9]+:)?data\\s*>");
  private static final Pattern COMPRESSED_ATTRIBUTE = Pattern.compile("compressed\\s*=\\s*\"(true|false)\"");
  private static final Boolean COMPRESS = CONFIG.getPropertyValue(CompressServiceTunnelRequestProperty.class);

  private String m_originAddress;
  private Boolean m_sendCompressed;
  private Boolean m_receivedCompressed;
  private IObjectSerializer m_objectSerializer;

  public DefaultServiceTunnelContentHandler() {
  }

  @Override
  public void initialize() {
    try {
      m_originAddress = InetAddress.getLocalHost().getHostAddress();
    }
    catch (Throwable t) {
      // nop
    }
    m_sendCompressed = COMPRESS;
    m_objectSerializer = createObjectSerializer();
  }

  /**
   * @return Creates an {@link IObjectSerializer} instance used for serializing and deserializing data.
   * @since 3.8.2
   */
  protected IObjectSerializer createObjectSerializer() {
    return SerializationUtility.createObjectSerializer(new ServiceTunnelObjectReplacer());
  }

  @Override
  public void writeRequest(OutputStream stream, ServiceTunnelRequest msg) throws Exception {
    boolean debugEnabled = LOG.isDebugEnabled();
    if (debugEnabled) {
      stream = new DebugOutputStream(stream);
    }
    try (OutputStreamWriter out = new OutputStreamWriter(stream, "UTF-8")) {

      // build soap message without sax (hi-speed)
      boolean compressed = isUseCompression();
      out.write("<soapenv:Envelope soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsse=\"http://schemas.xmlsoap.org/ws/2002/04/secext\">\n");
      out.write("<soapenv:Body>\n");
      out.write("  <request compressed=\"");
      out.write(Boolean.toString(compressed));
      out.write("\" locale=\"");
      out.write(msg.getLocale().toString());
      out.write("\" service=\"");
      out.write(msg.getServiceInterfaceClassName());
      out.write("\" operation=\"");
      out.write(msg.getOperation());
      out.write("\"/>\n");
      out.write("  <data>");

      long y = System.nanoTime();
      setData(out, msg, compressed);
      y = System.nanoTime() - y;
      if (debugEnabled) {
        LOG.debug("message encoding took " + y + " nanoseconds");
      }

      out.write("</data>\n");
      out.write("  <info");
      out.write(" origin=\"" + m_originAddress + "\"");
      out.write("/>\n");
      out.write("</soapenv:Body>");
      out.write("</soapenv:Envelope>");
    }
    finally {
      if (debugEnabled) {
        String sentData = ((DebugOutputStream) stream).getContent("UTF-8");
        int lastWrittenCharacter = ((DebugOutputStream) stream).getLastWrittenCharacter();
        Throwable lastThrownException = ((DebugOutputStream) stream).getLastThrownException();
        LOG.debug("lastWrittenCharacter=" + lastWrittenCharacter + ",lastThrownException=" + lastThrownException + ", sentData: " + sentData);
      }
    }
  }

  @Override
  public void writeResponse(OutputStream stream, IServiceTunnelResponse msg) throws Exception {
    boolean debugEnabled = LOG.isDebugEnabled();
    if (debugEnabled) {
      stream = new DebugOutputStream(stream);
    }
    try (OutputStreamWriter out = new OutputStreamWriter(stream, "UTF-8")) {

      // build soap message without sax (hi-speed)
      boolean compressed = isUseCompression();
      out.write("<soapenv:Envelope soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n");
      out.write("<soapenv:Body>\n");
      if (msg.getException() == null) {
        out.write("  <response status=\"OK\"");
        Object x = msg.getData();
        if (x != null) {
          out.write(" type=\"" + x.getClass().getSimpleName() + "\"");
        }
        else {
          out.write(" type=\"\"");
        }
        out.write(" compressed=\"" + compressed + "\"");
        out.write("/>\n");
      }
      else {
        out.write("  <response status=\"ERROR\"");
        out.write(" compressed=\"" + compressed + "\"");
        out.write(">\n");
        out.write("    <exception type=\"" + msg.getException().getClass().getSimpleName() + "\">");
        out.write(msg.getException().getMessage());
        out.write("</exception>\n");
        out.write("  </response>\n");
      }
      out.write("  <data>");

      long y = System.nanoTime();
      setData(out, msg, compressed);
      y = System.nanoTime() - y;
      if (debugEnabled) {
        LOG.debug("message encoding took " + y + " nanoseconds");
      }

      out.write("</data>\n");
      out.write("  <info");
      out.write(" origin=\"" + m_originAddress + "\"");
      out.write("/>\n");
      out.write("</soapenv:Body>");
      out.write("</soapenv:Envelope>");
    }
    finally {
      if (debugEnabled) {
        String sentData = ((DebugOutputStream) stream).getContent("UTF-8");
        int lastWrittenCharacter = ((DebugOutputStream) stream).getLastWrittenCharacter();
        Throwable lastThrownException = ((DebugOutputStream) stream).getLastThrownException();
        LOG.debug("lastWrittenCharacter=" + lastWrittenCharacter + ",lastThrownException=" + lastThrownException + ", sentData: " + sentData);
      }
    }
  }

  protected void setData(Writer writer, Object msg, boolean compressed) throws IOException {
    Deflater deflater = null;
    try {
      // build serialized data
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      OutputStream out = bos;
      if (compressed) {
        deflater = new Deflater(Deflater.BEST_SPEED);
        out = new DeflaterOutputStream(bos, deflater);
      }
      m_objectSerializer.serialize(out, msg);
      String base64Data = StringUtility.wrapText(Base64Utility.encode(bos.toByteArray()), 10000);
      writer.write(base64Data);
    }
    finally {
      if (deflater != null) {
        try {
          deflater.end();
        }
        catch (Throwable fatal) {
        }
      }
    }
  }

  @Override
  public ServiceTunnelRequest readRequest(InputStream in) throws Exception {
    return (ServiceTunnelRequest) read(in);
  }

  @Override
  public IServiceTunnelResponse readResponse(InputStream in) throws Exception {
    return (IServiceTunnelResponse) read(in);
  }

  protected Object/* msg */read(InputStream in) throws Exception {
    if (LOG.isDebugEnabled()) {
      in = new DebugInputStream(in);
    }
    String dataPart = null;
    boolean compressed = true;
    try {
      Reader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
      StringBuilder buf = new StringBuilder();
      int ch;
      while ((ch = r.read()) >= 0) {
        buf.append((char) ch);
      }
      String xml = buf.toString();
      buf.setLength(0);
      //get the 'compressed' attribute
      Matcher mc = COMPRESSED_ATTRIBUTE.matcher(xml);
      if (mc.find()) {
        compressed = "true".equals(mc.group(1));
        m_receivedCompressed = compressed;
      }
      // simply get the content of <ns:data>{?}</ns:data> or <data>{?}</data>
      Matcher m1 = BEGIN_DATA_TAG.matcher(xml);
      Matcher m2 = END_DATA_TAG.matcher(xml);
      int lastMatchingIndex = 0;
      if (m1.find() && m2.find(m1.start())) {
        do { // we want to be sure that we match the last </data> tag of the
          // message
          lastMatchingIndex = m2.start();
        }
        while (m2.find());
        dataPart = xml.substring(m1.end(), lastMatchingIndex);
      }
      else {
        throw new IOException("missing a data tag");
      }
    }
    finally {
      if (LOG.isDebugEnabled()) {
        String receivedData = ((DebugInputStream) in).getContent("UTF-8");
        int lastReadCharacter = ((DebugInputStream) in).getLastReadCharacter();
        Throwable lastThrownException = ((DebugInputStream) in).getLastThrownException();
        LOG.debug("lastReadCharacter=" + lastReadCharacter + ",lastThrownException=" + lastThrownException + ", receivedData:\n" + receivedData);
      }
    }
    long y = System.nanoTime();
    Object res = getData(dataPart, compressed);
    y = System.nanoTime() - y;
    if (LOG.isDebugEnabled()) {
      LOG.debug("message decoding took " + y + " nanoseconds");
    }
    return res;
  }

  protected Object getData(String dataPart, boolean compressed) throws IOException, ClassNotFoundException {
    Inflater inflater = null;
    try {
      String base64Data = dataPart.replaceAll("[\\n\\r]", "");
      // decode serial data
      InputStream in = new ByteArrayInputStream(Base64Utility.decode(base64Data));
      if (compressed) {
        inflater = new Inflater();
        in = new InflaterInputStream(in, inflater);
      }
      return m_objectSerializer.deserialize(in, null);
    }
    finally {
      if (inflater != null) {
        try {
          inflater.end();
        }
        catch (Throwable fatal) {
        }
      }
    }
  }

  protected boolean isUseCompression() {
    if (m_sendCompressed != null) {
      return m_sendCompressed;
    }
    if (m_receivedCompressed != null) {
      return m_receivedCompressed;
    }
    return true;
  }
}
