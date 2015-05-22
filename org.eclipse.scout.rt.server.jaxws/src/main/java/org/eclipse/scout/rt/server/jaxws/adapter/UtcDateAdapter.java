/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.jaxws.adapter;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.rt.platform.exception.PlatformException;

/**
 * <p>
 * The {@link String} provided must correspond to the <code>xsd:dateTime</code> format defined on <a
 * href="http://www.w3.org/TR/xmlschema-2/#dateTime">http://www.w3.org/TR/xmlschema-2/#dateTime</a>. The format was
 * inspired by [ISO 8601] but with timezone information included, because in [ISO 8601], a time is only represented as
 * local time or in relation to UTC (Zulu time).
 * </p>
 * <p>
 * <b> Please note, that if there is a timezone present, this adapter transforms the date into the UTC time. To get the
 * local time in that timezone, the timezone shift has to be added to the date. </b>
 * </p>
 * <p>
 * In the following, the rules of conversion to an UTC time are explained:
 * <ul>
 * <li>If there is a timezone included, the resulting {@link Date} would represent the UTC time, not the local time. To
 * get the local time in that timezone, the timezone shift has to be added to the UTC date. If you are expecting dates
 * from within different timezones, this adapter might not be suitable because the timeshift portion is lost which makes
 * it impossible to derive the local time.</li>
 * <li>If an UTC time is provided (dennoted by a 'Z', +00:00 or -00:00 as its timezone), the date is interpreted as UTC
 * time</li>
 * <li>If there no time zone provided at all, the date provided is interpreted to be a local date in respect to the
 * default timezone of this JVM. The resulting date would also be an UTC date.</li>
 * </ul>
 * </p>
 * <p>
 * In the following, please find some examples of the conversion from <code>xsd:date</code> to the UTC {@link Date}.
 * <table border="1">
 * <tr>
 * <th><code>xsd:date</code></th>
 * <th>UTC date</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>2011-11-03T18:04:00-05:00</td>
 * <td>2011-11-03 23:04:00</td>
 * <td>The date provided represents a local time in the given timezone whereas the resulting date is an UTC date.</td>
 * </tr>
 * <tr>
 * <td>2011-11-03T18:04:00Z</td>
 * <td>2011-11-03 18:04:00</td>
 * <td>The date provided is already an UTC date and therefore is the same as the resulting UTC {@link Date}</td>
 * </tr>
 * <tr>
 * <td>2011-11-03T18:04:00</td>
 * <td>e.g. 2011-11-03 17:04:00, if timezone is <code>+01:00</code></td>
 * <td>The date provided has no timezone information included and therefore is interpreted to be a local date in respect
 * to the default timezone of this JVM. Depending on the default timezone, the resulting UTC date differs.</td>
 * </tr>
 * </table>
 * </p>
 * <p>
 * <h2>Definition of xsd:dateTime format</h2> <b> Format:
 * <code>'-'? yyyy '-' mm '-' dd 'T' hh ':' mm ':' ss ('.' s+)? (zzzzzz)?</code></b>
 * <ul>
 * <li>'-'? <em>yyyy</em> is a four-or-more digit optionally negative-signed numeral that represents the year; if more
 * than four digits, leading zeros are prohibited, and '0000' is prohibited; also note that a plus sign is <b>not</b>
 * permitted);</li>
 * <li>the remaining '-'s are separators between parts of the date portion;</li>
 * <li>the first <em>mm</em> is a two-digit numeral that represents the month;</li>
 * <li><em>dd</em> is a two-digit numeral that represents the day;</li>
 * <li>'T' is a separator indicating that time-of-day follows;</li>
 * <li><em>hh</em> is a two-digit numeral that represents the hour; '24' is permitted if the minutes and seconds
 * represented are zero, and the <code>dateTime</code> value so represented is the first instant of the following day
 * (the hour property of a <code>dateTime</code> object cannot have a value greater than 23);</li>
 * <li>':' is a separator between parts of the time-of-day portion;</li>
 * <li>the second <em>mm</em> is a two-digit numeral that represents the minute;</li>
 * <li><em>ss</em> is a two-integer-digit numeral that represents the whole seconds;</li>
 * <li>'.' <em>s+</em> (if present) represents the fractional seconds;</li>
 * <li><em>zzzzzz</em> (if present) represents the timezone.</li>
 * </ul>
 * </p>
 */
public class UtcDateAdapter extends XmlAdapter<String, Date> {

  protected static final DatatypeFactory FACTORY;
  static {
    try {
      FACTORY = DatatypeFactory.newInstance();
    }
    catch (final DatatypeConfigurationException e) {
      throw new PlatformException("Failed to create 'DatatypeFactory' instance", e);
    }
  }

  @Override
  public String marshal(final Date date) throws Exception {
    if (date == null) {
      return null;
    }

    final long utcMillis = date.getTime();
    final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    calendar.setTimeInMillis(utcMillis);

    return FACTORY.newXMLGregorianCalendar(calendar).toXMLFormat();
  }

  @Override
  public Date unmarshal(final String rawValue) throws Exception {
    if (!StringUtility.hasText(rawValue)) {
      return null;
    }

    // local time of given timezone (or default timezone if not applicable)
    final XMLGregorianCalendar xmlCalendar = FACTORY.newXMLGregorianCalendar(rawValue);
    final GregorianCalendar calendar = xmlCalendar.toGregorianCalendar();
    final long utcMillis = calendar.getTimeInMillis();

    // UTC time
    final Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    utcCalendar.setTimeInMillis(utcMillis);
    return utcCalendar.getTime();
  }
}