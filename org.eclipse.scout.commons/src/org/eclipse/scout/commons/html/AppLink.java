package org.eclipse.scout.commons.html;

import java.io.Serializable;

/**
 * Bean representing an app link.
 * 
 * @since 5.1 (backported)
 */
public class AppLink implements Serializable {

  private static final long serialVersionUID = 1L;

  private String m_ref;
  private String m_name;

  public AppLink() {
  }

  /**
   * Creates an app link bean
   * 
   * @param ref
   *          Reference
   * @param name
   *          Name of the app link
   */
  public AppLink(String ref, String name) {
    m_ref = ref;
    m_name = name;
  }

  public String getRef() {
    return m_ref;
  }

  public void setRef(String ref) {
    m_ref = ref;
  }

  public String getName() {
    return m_name;
  }

  public void setName(String name) {
    m_name = name;
  }
}
