/*******************************************************************************
 * Copyright (c) 2011, 2013 BSI Business Systems Integration.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.scout.rt.server.services.common.jdbc.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.server.services.common.jdbc.style.AbstractSqlStyle;

public class PostgreSqlStyle extends AbstractSqlStyle {
  private static final long serialVersionUID = 1L;
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(PostgreSqlStyle.class);

  @Override
  public void testConnection(Connection conn) throws SQLException {
    Statement testStatement = null;
    try {
      testStatement = conn.createStatement();
      testStatement.execute("SELECT 1 + 1");
    }
    finally {
      if (testStatement != null) {
        try {
          testStatement.close();
        }
        catch (Exception e) {
          LOG.error("Failed to close the connection", e);
        }
      }
    }
  }

  @Override
  public Object readBind(final ResultSet rs, final ResultSetMetaData meta, final int type, final int jdbcBindIndex) throws SQLException {
    Object result = null;
    if (Types.BIT == type) {
      result = rs.getObject(jdbcBindIndex);
      if (result instanceof Boolean) {
        return result;
      }
    }
    return super.readBind(rs, meta, type, jdbcBindIndex);
  }

  @Override
  public boolean isBlobEnabled() {
    return false;
  }

  @Override
  public boolean isClobEnabled() {
    return false;
  }

  @Override
  public boolean isLargeString(String s) {
    return (s.length() > MAX_SQL_STRING_LENGTH);
  }

  @Override
  protected int getMaxListSize() {
    return MAX_LIST_SIZE;
  }
}