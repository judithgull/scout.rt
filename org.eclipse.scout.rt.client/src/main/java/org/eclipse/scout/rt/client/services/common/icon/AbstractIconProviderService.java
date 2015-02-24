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
package org.eclipse.scout.rt.client.services.common.icon;

import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.scout.commons.Assertions;
import org.eclipse.scout.commons.IOUtility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.service.AbstractService;

/**
 *
 */
public abstract class AbstractIconProviderService extends AbstractService implements IIconProviderService {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractIconProviderService.class);

  private String m_folderName = "resources/icons";
  private String m_iconExtensions = "png,gif,jpg";
  private String[] m_iconExtensionsArray;

  protected synchronized String[] getIconExtensionsArray() {
    if (m_iconExtensionsArray == null) {
      ArrayList<String> fileExtensions = new ArrayList<String>();
      if (getIconExtensions() != null) {
        StringTokenizer tokenizer = new StringTokenizer(getIconExtensions(), ",;");
        while (tokenizer.hasMoreTokens()) {
          String t = tokenizer.nextToken().trim();
          if (t.length() > 0) {
            fileExtensions.add(t);
          }
        }
      }
      m_iconExtensionsArray = fileExtensions.toArray(new String[fileExtensions.size()]);
    }
    return m_iconExtensionsArray;
  }

  @Override
  public IconSpec getIconSpec(String iconName) {
    String name = iconName;
    if (StringUtility.isNullOrEmpty(name)) {
      return null;
    }
    name = name.replaceAll("\\A[\\/\\\\]*", "");
    if (!name.startsWith(getFolderName())) {
      name = getFolderName() + "/" + iconName;
    }
    String[] fqns = new String[getIconExtensionsArray().length + 1];
    String[] iconNames = new String[getIconExtensionsArray().length + 1];
    fqns[0] = name;
    iconNames[0] = iconName;
    for (int i = 1; i < fqns.length; i++) {
      fqns[i] = name + "." + getIconExtensionsArray()[i - 1];
      iconNames[i] = iconName + "." + getIconExtensionsArray()[i - 1];
    }

    IconSpec spec = null;
    spec = findIconSpec(fqns, iconNames);
    return spec;

  }

  protected IconSpec findIconSpec(String[] fqns, String[] iconNames) {
    if (fqns != null && fqns.length > 0) {
      for (int i = 0; i < fqns.length; i++) {
        String fqn = fqns[i];
        String iconName = "";
        if (iconNames != null && iconNames.length > i) {
          iconName = iconNames[i];
        }
        URL url = findResource(fqn);
        if (url != null) {
          try {
            IconSpec iconSpec = new IconSpec();
            byte[] content = IOUtility.getContent(url.openStream(), true);
            if (content != null) {
              iconSpec.setContent(content);
            }
            iconSpec.setName(iconName);
            return iconSpec;
          }
          catch (Exception e) {
            LOG.error("could not read input stream from url '" + url + "'.", e);
          }
        }
      }
    }
    return null;
  }

  protected URL findResource(String fullPath) {
    return getClass().getClassLoader().getResource(fullPath);
  }

  public void setFolderName(String folderName) {
    Assertions.assertNotNullOrEmpty(folderName);
    m_folderName = folderName.replace('.', '/');
  }

  public String getFolderName() {
    return m_folderName;
  }

  public synchronized void setIconExtensions(String iconExtensions) {
    m_iconExtensions = iconExtensions;
    m_iconExtensionsArray = null;
  }

  /**
   * @return a comma separated list of all extensions e.g. 'gif,png,jpg'
   */
  public String getIconExtensions() {
    return m_iconExtensions;
  }
}