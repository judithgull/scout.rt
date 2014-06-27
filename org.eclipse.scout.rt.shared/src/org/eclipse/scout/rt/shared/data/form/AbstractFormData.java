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
package org.eclipse.scout.rt.shared.data.form;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.scout.commons.ClassIdentifier;
import org.eclipse.scout.commons.CloneUtility;
import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.annotations.Replace;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.shared.data.form.fields.AbstractFormFieldData;
import org.eclipse.scout.rt.shared.data.form.properties.AbstractPropertyData;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.service.SERVICES;

public abstract class AbstractFormData implements Serializable, Cloneable {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractFormData.class);
  private static final long serialVersionUID = 1L;

  private Map<Class<?>, Class<? extends AbstractFormFieldData>> m_fieldDataReplacements;
  private Map<Class<? extends AbstractPropertyData>, AbstractPropertyData> m_propertyMap;
  private Map<Class<? extends AbstractFormFieldData>, AbstractFormFieldData> m_fieldMap;

  public AbstractFormData() {
    initConfig();
  }

  private List<Class<AbstractPropertyData>> getConfiguredPropertyDatas() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    return ConfigurationUtility.filterClasses(dca, AbstractPropertyData.class);
  }

  private List<Class<? extends AbstractFormFieldData>> getConfiguredFieldDatas() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    List<Class<AbstractFormFieldData>> fca = ConfigurationUtility.filterClasses(dca, AbstractFormFieldData.class);
    return ConfigurationUtility.removeReplacedClasses(fca);
  }

  protected void initConfig() {
    // add properties
    m_propertyMap = new HashMap<Class<? extends AbstractPropertyData>, AbstractPropertyData>();
    for (Class<AbstractPropertyData> propertyDataClazz : getConfiguredPropertyDatas()) {
      AbstractPropertyData p;
      try {
        p = ConfigurationUtility.newInnerInstance(this, propertyDataClazz);
        m_propertyMap.put(p.getClass(), p);
      }// end try
      catch (Exception e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("error creating instance of class '" + propertyDataClazz.getName() + "'.", e));
      }
    }// end for
     // add fields
    m_fieldMap = new HashMap<Class<? extends AbstractFormFieldData>, AbstractFormFieldData>();
    List<Class<? extends AbstractFormFieldData>> formFieldDataClazzes = getConfiguredFieldDatas();
    Map<Class<?>, Class<? extends AbstractFormFieldData>> replacements = ConfigurationUtility.getReplacementMapping(formFieldDataClazzes);
    if (!replacements.isEmpty()) {
      m_fieldDataReplacements = replacements;
    }
    for (Class<? extends AbstractFormFieldData> formFieldDataClazz : formFieldDataClazzes) {
      AbstractFormFieldData f;
      try {
        f = ConfigurationUtility.newInnerInstance(this, formFieldDataClazz);
        m_fieldMap.put(f.getClass(), f);
      }// end try
      catch (Exception e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("error creating instance of class '" + formFieldDataClazz.getName() + "'.", e));
      }
    }// end for
  }

  public AbstractPropertyData getPropertyById(String id) {
    for (AbstractPropertyData p : m_propertyMap.values()) {
      if (p.getPropertyId().equalsIgnoreCase(id)) {
        return p;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T extends AbstractPropertyData> T getPropertyByClass(Class<T> c) {
    return (T) m_propertyMap.get(c);
  }

  public <T extends AbstractPropertyData> void setPropertyByClass(Class<T> c, T v) {
    if (v == null) {
      m_propertyMap.remove(c);
    }
    else {
      m_propertyMap.put(c, v);
    }
  }

  public AbstractPropertyData[] getAllProperties() {
    return m_propertyMap != null ? m_propertyMap.values().toArray(new AbstractPropertyData[m_propertyMap.size()]) : new AbstractPropertyData[0];
  }

  public AbstractFormFieldData getFieldById(String id) {
    String fieldDataId = FormDataUtility.getFieldDataId(id);
    for (AbstractFormFieldData f : m_fieldMap.values()) {
      if (f.getFieldId().equals(fieldDataId)) {
        return f;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T extends AbstractFormFieldData> T getFieldByClass(Class<T> c) {
    Class<? extends T> clazz = getReplacingFieldDataClass(c);
    return (T) m_fieldMap.get(clazz);
  }

  public <T extends AbstractFormFieldData> void setFieldByClass(Class<T> c, T v) {
    Class<? extends T> clazz = getReplacingFieldDataClass(c);
    if (v == null) {
      m_fieldMap.remove(clazz);
    }
    else {
      m_fieldMap.put(clazz, v);
    }
  }

  /**
   * Checks whether the form field data with the given class has been replaced by another field. If so, the replacing
   * form field data's class is returned. Otherwise the given class itself.
   * 
   * @param c
   * @return Returns the possibly available replacing field data class for the given class.
   * @see Replace
   * @since 3.8.2
   */
  private <T extends AbstractFormFieldData> Class<? extends T> getReplacingFieldDataClass(Class<T> c) {
    if (m_fieldDataReplacements != null) {
      @SuppressWarnings("unchecked")
      Class<T> replacingFieldDataClass = (Class<T>) m_fieldDataReplacements.get(c);
      if (replacingFieldDataClass != null) {
        return replacingFieldDataClass;
      }
    }
    return c;
  }

  /**
   * @return all fields of the form data itself, not including fields in
   *         external field templates
   */
  public AbstractFormFieldData[] getFields() {
    return m_fieldMap.values().toArray(new AbstractFormFieldData[m_fieldMap.size()]);
  }

  /**
   * @return all fields of the form data and all its external template field
   *         datas in a map with qualified ids<br>
   *         The array of returned fields is the result of a top-down
   *         breath-first tree traversal
   *         <p>
   *         Example:
   * 
   *         <pre>
   * A
   *   U
   *     E
   *     F
   *   V
   * B
   *   X
   *   Y
   * </pre>
   * 
   *         would be returned as A B U V X Y E F
   */
  public Map<Integer, Map<String/* qualified field id */, AbstractFormFieldData>> getAllFieldsRec() {
    TreeMap<Integer, Map<String, AbstractFormFieldData>> breathFirstMap = new TreeMap<Integer, Map<String, AbstractFormFieldData>>();
    for (AbstractFormFieldData child : getFields()) {
      collectAllFieldsRec(child, breathFirstMap, 0, "");
    }
    return breathFirstMap;
  }

  private void collectAllFieldsRec(AbstractFormFieldData field, Map<Integer/* level */, Map<String/* qualified field id */, AbstractFormFieldData>> breathFirstMap, int level, String prefix) {
    Map<String/* qualified field id */, AbstractFormFieldData> subMap = breathFirstMap.get(level);
    if (subMap == null) {
      subMap = new HashMap<String/* qualified field id */, AbstractFormFieldData>();
      breathFirstMap.put(level, subMap);
    }
    subMap.put(prefix + field.getFieldId(), field);
    for (AbstractFormFieldData child : field.getFields()) {
      collectAllFieldsRec(child, breathFirstMap, level + 1, prefix + field.getFieldId() + "/");
    }
  }

  /**
   * Searches the given form field data in this form data as well as in all externally referenced template
   * field data.
   * 
   * @param breathFirstMap
   *          The breath-first search map as returned by {@link AbstractFormData#getAllFieldsRec()}. If
   *          <code>null</code>, a new map is created.
   * @param valueTypeIdentifier
   *          The class identifier to be searched in the form data.
   * @return Returns the form data's {@link AbstractFormFieldData} of the given valueType or <code>null</code>, if it
   *         does not exist.
   * @throws ProcessingException
   */
  public AbstractFormFieldData findFieldByClass(Map<Integer, Map<String, AbstractFormFieldData>> breathFirstMap, ClassIdentifier valueTypeIdentifier) throws ProcessingException {
    if (breathFirstMap == null) {
      breathFirstMap = getAllFieldsRec();
    }
    AbstractFormFieldData candidate = null;
    for (Map<String, AbstractFormFieldData> subMap : breathFirstMap.values()) {
      for (Entry<String, AbstractFormFieldData> entry : subMap.entrySet()) {
        AbstractFormFieldData fd = entry.getValue();
        String fieldId = entry.getKey();
        if (matchesAllParts(valueTypeIdentifier, fieldId, fd)) {
          if (candidate != null) {
            throw new ProcessingException("Found more than one field for class: [" + fd.getClass() + "]");
          }
          candidate = fd;
        }
      }
    }
    return candidate;
  }

  /**
   * @return all properties of the form data and all its external template field
   *         data in a map with qualified ids<br>
   *         The array of returned fields is the result of a top-down
   *         breath-first tree traversal
   *         <p>
   *         Example:
   * 
   *         <pre>
   * A (p1, p4)
   *   U
   *     E (p3)
   *     F
   *   V
   * B
   *   X (p2)
   *   Y
   * </pre>
   * 
   *         would be returned as p1, p4, p2, p3
   */
  public Map<Integer, Map<String/* qualified property id */, AbstractPropertyData<?>>> getAllPropertiesRec() {
    TreeMap<Integer, Map<String, AbstractPropertyData<?>>> breathFirstMap = new TreeMap<Integer, Map<String, AbstractPropertyData<?>>>();
    HashMap<String, AbstractPropertyData<?>> rootMap = new HashMap<String/* qualified field id */, AbstractPropertyData<?>>();
    breathFirstMap.put(0, rootMap);
    for (AbstractPropertyData<?> prop : getAllProperties()) {
      rootMap.put(prop.getClass().getSimpleName(), prop);
    }
    for (AbstractFormFieldData child : getFields()) {
      collectAllPropertiesRec(child, breathFirstMap, 1, child.getFieldId() + "/");
    }
    return breathFirstMap;
  }

  private void collectAllPropertiesRec(AbstractFormFieldData field, Map<Integer/* level */, Map<String/* qualified field id */, AbstractPropertyData<?>>> breathFirstMap, int level, String prefix) {
    Map<String/* qualified field id */, AbstractPropertyData<?>> subMap = breathFirstMap.get(level);
    if (subMap == null) {
      subMap = new HashMap<String/* qualified field id */, AbstractPropertyData<?>>();
      breathFirstMap.put(level, subMap);
    }
    for (AbstractPropertyData<?> prop : field.getAllProperties()) {
      subMap.put(prefix + prop.getClass().getSimpleName(), prop);
    }
    for (AbstractFormFieldData child : field.getFields()) {
      collectAllPropertiesRec(child, breathFirstMap, level + 1, prefix + child.getFieldId() + "/");
    }
  }

  /**
   * Searches the given property data in this form data as well as in all externally referenced template
   * field data.
   * 
   * @param breathFirstMap
   *          The breath-first search map as returned by {@link AbstractFormData#getAllPropertiesRec()}. If
   *          <code>null</code>, a new map is created.
   * @param valueType
   *          The type to be searched in the form data.
   * @return Returns the form data's {@link AbstractPropertyData} of the given valueType or <code>null</code>, if it
   *         does not exist.
   * @throws ProcessingException
   */
  public AbstractPropertyData<?> findPropertyByClass(Map<Integer, Map<String, AbstractPropertyData<?>>> breathFirstMap, ClassIdentifier valueTypeClassIdentifier) throws ProcessingException {
    if (breathFirstMap == null) {
      breathFirstMap = getAllPropertiesRec();
    }
    AbstractPropertyData<?> candidate = null;
    for (Map<String, AbstractPropertyData<?>> subMap : breathFirstMap.values()) {
      for (Map.Entry<String, AbstractPropertyData<?>> entry : subMap.entrySet()) {
        String propertyId = entry.getKey();
        AbstractPropertyData<?> pd = entry.getValue();
        if (matchesAllParts(valueTypeClassIdentifier, propertyId, pd)) {
          if (candidate != null) {
            throw new ProcessingException("Found more than one property for class: [" + pd.getClass() + "]");
          }
          candidate = pd;
        }
      }
    }
    return candidate;
  }

  @Override
  public Object clone() {
    try {
      return CloneUtility.createDeepCopyBySerializing(this);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Checks whether the given fully qualified fieldId matches all parts of the given class identifier.
   * The last segment is checked against the given objects's type.
   * 
   * @param valueTypeIdentifier
   * @param fullyQualifiedFieldId
   *          The fully qualified fieldId.
   * @param obj
   *          The object representing the last segment.
   * @return Returns <code>true</code> if all segments of the given class identifier are part of the fully qualified
   *         field id. <code>false</code> otherwise.
   */
  private boolean matchesAllParts(ClassIdentifier valueTypeIdentifier, String fullyQualifiedFieldId, Object obj) {
    // check last segment by class
    if (obj == null || obj.getClass() != valueTypeIdentifier.getLastSegment()) {
      return false;
    }
    // check other segments by id
    Class<?>[] classes = valueTypeIdentifier.getClasses();
    String[] fieldIdParts = fullyQualifiedFieldId.split("[/]");
    int i = classes.length - 2;
    int j = fieldIdParts.length - 2;
    while (i >= 0 && j >= 0) {
      String fieldId = classes[i].getName();
      int i1 = Math.max(fieldId.lastIndexOf('$'), fieldId.lastIndexOf('.'));
      fieldId = fieldId.substring(i1 + 1);
      if (fieldIdParts[j].equals(fieldId)) {
        i--;
      }
      j--;
    }
    return i < 0;
  }

  /*
   * In subclasses of this class the configurator will add setters/getters for
   * properties that are declared in the form
   */

  /*
   * In subclasses of this class the configurator will add getters for inner
   * form field data types
   */

}
