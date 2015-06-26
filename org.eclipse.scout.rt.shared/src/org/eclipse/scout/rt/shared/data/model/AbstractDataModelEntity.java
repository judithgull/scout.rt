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
package org.eclipse.scout.rt.shared.data.model;

import java.io.Serializable;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.IOrdered;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.annotations.OrderedCollection;
import org.eclipse.scout.commons.beans.AbstractPropertyObserver;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.shared.extension.AbstractSerializableExtension;
import org.eclipse.scout.rt.shared.extension.ContributionComposite;
import org.eclipse.scout.rt.shared.extension.ExtensionUtility;
import org.eclipse.scout.rt.shared.extension.IContributionOwner;
import org.eclipse.scout.rt.shared.extension.IExtensibleObject;
import org.eclipse.scout.rt.shared.extension.IExtension;
import org.eclipse.scout.rt.shared.extension.ObjectExtensions;
import org.eclipse.scout.rt.shared.extension.data.model.DataModelEntityChains.DataModelEntityInitEntityChain;
import org.eclipse.scout.rt.shared.extension.data.model.IDataModelEntityExtension;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.services.common.security.IAccessControlService;
import org.eclipse.scout.service.SERVICES;

public abstract class AbstractDataModelEntity extends AbstractPropertyObserver implements IDataModelEntity, Serializable, IContributionOwner, IExtensibleObject {
  private static final long serialVersionUID = 1L;
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractDataModelEntity.class);

  private String m_id;
  private double m_order;
  private Permission m_visiblePermission;
  private boolean m_visibleGranted;
  private boolean m_visibleProperty;
  private boolean m_oneToMany;
  private String m_text;
  private String m_iconId;
  private List<IDataModelAttribute> m_attributes;
  private List<IDataModelEntity> m_entities;
  private IDataModelEntity m_parentEntity;
  private boolean m_initializedChildEntities;
  private boolean m_initialized;
  private IContributionOwner m_contributionHolder;
  private final ObjectExtensions<AbstractDataModelEntity, IDataModelEntityExtension<? extends AbstractDataModelEntity>> m_objectExtensions;

  public AbstractDataModelEntity() {
    this(true);
  }

  /**
   * @param callInitConfig
   *          true if {@link #callInitializer()} should automatically be invoked, false if the subclass invokes
   *          {@link #callInitializer()} itself
   */
  public AbstractDataModelEntity(boolean callInitConfig) {
    m_attributes = new ArrayList<IDataModelAttribute>();
    m_entities = new ArrayList<IDataModelEntity>();
    m_objectExtensions = new ObjectExtensions<AbstractDataModelEntity, IDataModelEntityExtension<? extends AbstractDataModelEntity>>(this);
    if (callInitConfig) {
      callInitializer();
    }
  }

  @Override
  public final List<Object> getAllContributions() {
    return m_contributionHolder.getAllContributions();
  }

  @Override
  public final <T> List<T> getContributionsByClass(Class<T> type) {
    return m_contributionHolder.getContributionsByClass(type);
  }

  @Override
  public final <T> T getContribution(Class<T> contribution) {
    return m_contributionHolder.getContribution(contribution);
  }

  protected final void callInitializer() {
    interceptInitConfig();
  }

  /**
   * Calculates the column's view order, e.g. if the @Order annotation is set to 30.0, the method will
   * return 30.0. If no {@link Order} annotation is set, the method checks its super classes for an @Order annotation.
   *
   * @since 3.10.0-M4
   */
  protected double calculateViewOrder() {
    double viewOrder = getConfiguredViewOrder();
    Class<?> cls = getClass();
    if (viewOrder == IOrdered.DEFAULT_ORDER) {
      while (cls != null && IDataModelEntity.class.isAssignableFrom(cls)) {
        if (cls.isAnnotationPresent(Order.class)) {
          Order order = (Order) cls.getAnnotation(Order.class);
          return order.value();
        }
        cls = cls.getSuperclass();
      }
    }
    return viewOrder;
  }

  /*
   * Configuration
   */

  @ConfigProperty(ConfigProperty.TEXT)
  @Order(20)
  protected String getConfiguredText() {
    return null;
  }

  @ConfigProperty(ConfigProperty.ICON_ID)
  @Order(10)
  protected String getConfiguredIconId() {
    return null;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(50)
  protected boolean getConfiguredVisible() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(50)
  protected boolean getConfiguredOneToMany() {
    return true;
  }

  /**
   * Configures the view order of this data model entity. The view order determines the order in which the entities
   * appear. The order of entities with no view order configured ({@code < 0}) is initialized based on the {@link Order}
   * annotation of the entity class.
   * <p>
   * Subclasses can override this method. The default is {@link IOrdered#DEFAULT_ORDER}.
   *
   * @return View order of this entity.
   */
  @ConfigProperty(ConfigProperty.DOUBLE)
  @Order(60)
  protected double getConfiguredViewOrder() {
    return IOrdered.DEFAULT_ORDER;
  }

  /**
   * Initialize this entity.
   */
  @ConfigOperation
  @Order(10)
  protected void execInitEntity() throws ProcessingException {
  }

  private List<Class<IDataModelAttribute>> getConfiguredAttributes() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    return ConfigurationUtility.filterClasses(dca, IDataModelAttribute.class);
  }

  private List<Class<IDataModelEntity>> getConfiguredEntities() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    return ConfigurationUtility.filterClasses(dca, IDataModelEntity.class);
  }

  protected final void interceptInitConfig() {
    m_objectExtensions.initConfig(createLocalExtension(), new Runnable() {
      @Override
      public void run() {
        initConfig();
      }
    });
  }

  protected void initConfig() {
    m_visibleGranted = true;
    m_contributionHolder = new ContributionComposite(this);
    setText(getConfiguredText());
    setIconId(getConfiguredIconId());
    setVisible(getConfiguredVisible());
    setOneToMany(getConfiguredOneToMany());
    setOrder(calculateViewOrder());

    List<Class<IDataModelAttribute>> configuredAttributes = getConfiguredAttributes();
    List<IDataModelAttribute> contributedAttributes = m_contributionHolder.getContributionsByClass(IDataModelAttribute.class);

    OrderedCollection<IDataModelAttribute> attributes = new OrderedCollection<IDataModelAttribute>();
    for (Class<? extends IDataModelAttribute> c : configuredAttributes) {
      try {
        attributes.addOrdered(ConfigurationUtility.newInnerInstance(this, c));
      }
      catch (Exception e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("error creating instance of class '" + c.getName() + "'.", e));
      }
    }

    attributes.addAllOrdered(contributedAttributes);

    injectAttributesInternal(attributes);
    ExtensionUtility.moveModelObjects(attributes);
    m_attributes = attributes.getOrderedList();

    for (IDataModelAttribute a : m_attributes) {
      if (a instanceof AbstractDataModelAttribute) {
        ((AbstractDataModelAttribute) a).setParentEntity(this);
      }
    }
    //lazy create entities at point when setParentEntity is set, this is necessary to avoid cyclic loops
    m_entities = new ArrayList<IDataModelEntity>();
  }

  @Override
  public final List<? extends IDataModelEntityExtension<? extends AbstractDataModelEntity>> getAllExtensions() {
    return m_objectExtensions.getAllExtensions();
  }

  protected IDataModelEntityExtension<? extends AbstractDataModelEntity> createLocalExtension() {
    return new LocalDataModelEntityExtension<AbstractDataModelEntity>(this);
  }

  @Override
  public <T extends IExtension<?>> T getExtension(Class<T> c) {
    return m_objectExtensions.getExtension(c);
  }

  @Override
  public Map<String, String> getMetaDataOfEntity() {
    return null;
  }

  /*
   * Runtime
   */

  @Override
  public final void initEntity() throws ProcessingException {
    if (m_initialized) {
      return;
    }

    try {
      interceptInitEntity();
    }
    catch (Throwable t) {
      LOG.error("entity " + this, t);
    }
    for (IDataModelAttribute a : getAttributes()) {
      try {
        a.initAttribute();
      }
      catch (Throwable t) {
        LOG.error("attribute " + this + "/" + a, t);
      }
    }
    m_initialized = true;
    for (IDataModelEntity e : getEntities()) {
      try {
        e.initEntity();
      }
      catch (Throwable t) {
        LOG.error("entity " + this + "/" + e, t);
      }
    }
  }

  @Override
  public Permission getVisiblePermission() {
    return m_visiblePermission;
  }

  @Override
  public void setVisiblePermission(Permission p) {
    setVisiblePermissionInternal(p);
    boolean b;
    if (p != null) {
      b = SERVICES.getService(IAccessControlService.class).checkPermission(p);
    }
    else {
      b = true;
    }
    setVisibleGranted(b);
  }

  protected void setVisiblePermissionInternal(Permission p) {
    m_visiblePermission = p;
  }

  @Override
  public boolean isVisibleGranted() {
    return m_visibleGranted;
  }

  @Override
  public void setVisibleGranted(boolean b) {
    m_visibleGranted = b;
    calculateVisible();
  }

  @Override
  public boolean isVisible() {
    return propertySupport.getPropertyBool(PROP_VISIBLE);
  }

  @Override
  public void setVisible(boolean b) {
    setVisibleProperty(b);
    calculateVisible();
  }

  protected void setVisibleProperty(boolean b) {
    m_visibleProperty = b;
  }

  protected boolean isVisibleProperty() {
    return m_visibleProperty;
  }

  @Override
  public boolean isOneToMany() {
    return m_oneToMany;
  }

  @Override
  public void setOneToMany(boolean b) {
    m_oneToMany = b;
  }

  /**
   * no access control for system buttons CANCEL and CLOSE
   */
  private void calculateVisible() {
    // access control
    propertySupport.setPropertyBool(PROP_VISIBLE, m_visibleGranted && m_visibleProperty);
  }

  @Override
  public double getOrder() {
    return m_order;
  }

  @Override
  public void setOrder(double order) {
    m_order = order;
  }

  @Override
  public String getIconId() {
    return m_iconId;
  }

  @Override
  public void setIconId(String s) {
    m_iconId = s;
  }

  @Override
  public String getText() {
    return m_text;
  }

  @Override
  public void setText(String s) {
    m_text = s;
  }

  @Override
  public List<IDataModelAttribute> getAttributes() {
    return CollectionUtility.arrayList(m_attributes);
  }

  @Override
  public List<IDataModelEntity> getEntities() {
    return CollectionUtility.arrayList(m_entities);
  }

  @Override
  public IDataModelAttribute getAttribute(Class<? extends IDataModelAttribute> attributeClazz) {
    for (IDataModelAttribute attribute : m_attributes) {
      if (attribute.getClass() == attributeClazz) {
        return attribute;
      }
    }
    return null;
  }

  @Override
  public IDataModelEntity getEntity(Class<? extends IDataModelEntity> entityClazz) {
    for (IDataModelEntity entity : m_entities) {
      if (entity.getClass() == entityClazz) {
        return entity;
      }
    }
    return null;
  }

  @Override
  public IDataModelEntity getParentEntity() {
    return m_parentEntity;
  }

  public void setParentEntity(IDataModelEntity parent) {
    m_parentEntity = parent;
  }

  @Override
  public void initializeChildEntities(Map<Class<? extends IDataModelEntity>, IDataModelEntity> instanceMap) {
    if (!m_initializedChildEntities) {
      m_initializedChildEntities = true;
      List<Class<IDataModelEntity>> configuredEntities = getConfiguredEntities();
      List<IDataModelEntity> contributedEntities = m_contributionHolder.getContributionsByClass(IDataModelEntity.class);
      int numEntities = configuredEntities.size() + contributedEntities.size();

      Set<IDataModelEntity> newConfiguredInstances = new HashSet<IDataModelEntity>(numEntities);
      OrderedCollection<IDataModelEntity> entities = new OrderedCollection<IDataModelEntity>();
      for (Class<? extends IDataModelEntity> c : configuredEntities) {
        try {
          //check if a parent is of same type, in that case use reference
          IDataModelEntity e = instanceMap.get(c);
          if (e == null) {
            e = ConfigurationUtility.newInnerInstance(this, c);
            newConfiguredInstances.add(e);
            instanceMap.put(c, e);
          }
          entities.addOrdered(e);
        }
        catch (Exception ex) {
          SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("error creating instance of class '" + c.getName() + "'.", ex));
        }
      }
      newConfiguredInstances.addAll(contributedEntities);
      entities.addAllOrdered(contributedEntities);
      injectEntitiesInternal(entities);
      ExtensionUtility.moveModelObjects(entities);

      m_entities.clear();
      m_entities.addAll(entities.getOrderedList());

      for (IDataModelEntity e : m_entities) {
        if (e instanceof AbstractDataModelEntity) {
          AbstractDataModelEntity adme = (AbstractDataModelEntity) e;
          if (adme.getParentEntity() != this) {
            adme.setParentEntity(this);
          }
        }
      }
      for (IDataModelEntity e : m_entities) {
        if (newConfiguredInstances.contains(e) || !instanceMap.containsKey(e.getClass())) {
          e.initializeChildEntities(instanceMap);
        }
      }
    }
  }

  /**
   * Override this internal method only in order to make use of dynamic attributes<br>
   * Used to add and/or remove attributes<br>
   * To change the order or specify the insert position use {@link IDataModelAttribute#setOrder(double)}.
   *
   * @param attributes
   *          live and mutable collection of configured attributes
   */
  protected void injectAttributesInternal(OrderedCollection<IDataModelAttribute> attributes) {
  }

  /**
   * Override this internal method only in order to make use of dynamic entities<br>
   * Used to add and/or remove entities<br>
   * To change the order or specify the insert position use {@link IDataModelEntity#setOrder(double)}.<br>
   * Note that {@link #initializeChildEntities(Map)} is also called on injected entities
   *
   * @param entities
   *          live and mutable collection of configured entities
   */
  protected void injectEntitiesInternal(OrderedCollection<IDataModelEntity> entities) {
  }

  /**
   * The extension delegating to the local methods. This Extension is always at the end of the chain and will not call
   * any further chain elements.
   */
  protected static class LocalDataModelEntityExtension<OWNER extends AbstractDataModelEntity> extends AbstractSerializableExtension<OWNER> implements IDataModelEntityExtension<OWNER> {
    private static final long serialVersionUID = 1L;

    public LocalDataModelEntityExtension(OWNER owner) {
      super(owner);
    }

    @Override
    public void execInitEntity(DataModelEntityInitEntityChain chain) throws ProcessingException {
      getOwner().execInitEntity();
    }

  }

  protected final void interceptInitEntity() throws ProcessingException {
    List<? extends IDataModelEntityExtension<? extends AbstractDataModelEntity>> extensions = getAllExtensions();
    DataModelEntityInitEntityChain chain = new DataModelEntityInitEntityChain(extensions);
    chain.execInitEntity();
  }
}
