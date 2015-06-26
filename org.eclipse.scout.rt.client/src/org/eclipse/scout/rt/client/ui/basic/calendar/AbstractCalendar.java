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
package org.eclipse.scout.rt.client.ui.basic.calendar;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.CompositeObject;
import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.DateUtility;
import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.Range;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.annotations.OrderedCollection;
import org.eclipse.scout.commons.beans.AbstractPropertyObserver;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.extension.ui.action.tree.MoveActionNodesHandler;
import org.eclipse.scout.rt.client.extension.ui.basic.calendar.CalendarChains.CalendarDisposeCalendarChain;
import org.eclipse.scout.rt.client.extension.ui.basic.calendar.CalendarChains.CalendarFilterCalendarItemsChain;
import org.eclipse.scout.rt.client.extension.ui.basic.calendar.CalendarChains.CalendarInitCalendarChain;
import org.eclipse.scout.rt.client.extension.ui.basic.calendar.ICalendarExtension;
import org.eclipse.scout.rt.client.ui.action.ActionUtility;
import org.eclipse.scout.rt.client.ui.action.menu.CalendarMenuType;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.menu.root.ICalendarContextMenu;
import org.eclipse.scout.rt.client.ui.action.menu.root.internal.CalendarContextMenu;
import org.eclipse.scout.rt.client.ui.basic.calendar.provider.ICalendarItemProvider;
import org.eclipse.scout.rt.client.ui.basic.cell.Cell;
import org.eclipse.scout.rt.shared.extension.AbstractExtension;
import org.eclipse.scout.rt.shared.extension.ContributionComposite;
import org.eclipse.scout.rt.shared.extension.IContributionOwner;
import org.eclipse.scout.rt.shared.extension.IExtensibleObject;
import org.eclipse.scout.rt.shared.extension.IExtension;
import org.eclipse.scout.rt.shared.extension.ObjectExtensions;
import org.eclipse.scout.rt.shared.services.common.calendar.ICalendarItem;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.service.SERVICES;

/**
 * {@link ICalendarItemProducer} are defined as inner classes<br>
 */
public abstract class AbstractCalendar extends AbstractPropertyObserver implements ICalendar, IContributionOwner, IExtensibleObject {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractCalendar.class);

  private boolean m_initialized;
  private List<ICalendarItemProvider> m_providers;
  private final HashMap<Class<? extends ICalendarItemProvider>, Collection<CalendarComponent>> m_componentsByProvider;
  private ICalendarUIFacade m_uiFacade;
  private int m_calendarChanging;
  private final DateTimeFormatFactory m_dateTimeFormatFactory;
  private List<CalendarEvent> m_calendarEventBuffer;
  private final EventListenerList m_listenerList;
  private IContributionOwner m_contributionHolder;
  private final ObjectExtensions<AbstractCalendar, ICalendarExtension<? extends AbstractCalendar>> m_objectExtensions;

  // internal usage of menus temporarily added of the current item provider
  private List<IMenu> m_inheritedMenusOfSelectedProvider;

  public AbstractCalendar() {
    this(true);
  }

  public AbstractCalendar(boolean callInitializer) {
    m_calendarEventBuffer = new ArrayList<CalendarEvent>();
    m_listenerList = new EventListenerList();
    m_dateTimeFormatFactory = new DateTimeFormatFactory();
    m_componentsByProvider = new HashMap<Class<? extends ICalendarItemProvider>, Collection<CalendarComponent>>();
    m_objectExtensions = new ObjectExtensions<AbstractCalendar, ICalendarExtension<? extends AbstractCalendar>>(this);
    if (callInitializer) {
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

  protected void callInitializer() {
    if (!m_initialized) {
      interceptInitConfig();
      m_initialized = true;
    }
  }

  /*
   * Configuration
   */
  @ConfigProperty(ConfigProperty.TEXT)
  @Order(10)
  protected String getConfiguredTitle() {
    return null;
  }

  @ConfigProperty(ConfigProperty.INTEGER)
  @Order(500)
  protected int getConfiguredStartHour() {
    return 6;
  }

  @ConfigProperty(ConfigProperty.INTEGER)
  @Order(510)
  protected int getConfiguredEndHour() {
    return 19;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(520)
  protected boolean getConfiguredUseOverflowCells() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(530)
  protected boolean getConfiguredShowDisplayModeSelection() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(540)
  protected boolean getConfiguredMarkNoonHour() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(550)
  protected boolean getConfiguredMarkOutOfMonthDays() {
    return true;
  }

  private List<Class<? extends ICalendarItemProvider>> getConfiguredProducers() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    List<Class<ICalendarItemProvider>> filtered = ConfigurationUtility.filterClasses(dca, ICalendarItemProvider.class);
    List<Class<? extends ICalendarItemProvider>> foca = ConfigurationUtility.sortFilteredClassesByOrderAnnotation(filtered, ICalendarItemProvider.class);
    return ConfigurationUtility.removeReplacedClasses(foca);
  }

  protected List<Class<? extends IMenu>> getDeclaredMenus() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    List<Class<IMenu>> filtered = ConfigurationUtility.filterClasses(dca, IMenu.class);
    return ConfigurationUtility.removeReplacedClasses(filtered);
  }

  @ConfigOperation
  @Order(10)
  protected void execInitCalendar() throws ProcessingException {
  }

  @ConfigOperation
  @Order(15)
  protected void execDisposeCalendar() throws ProcessingException {
  }

  /**
   * Filter and resolve item conflicts after some {@link ICalendarItemProvider} changed their items
   *
   * @param changedProviders
   *          is the list of provider types that changed their provided items
   * @param componentsByProvider
   *          is the life map of all provider types with their provided items.
   *          <p>
   *          Changes to the componentsByProvider map are life applied to the calendar model.<br>
   *          Often the convenience method {@link #findConflictingItems(Map, Class...)} is used to calculate conflicting
   *          items of two or more providers for removal from the map.
   *          <p>
   *          The {@link ICalendarItem}s are wrapped into {@link CalendarComponent}s to hold their originating provider
   *          and other common info.<br>
   *          Use {@link CalendarComponent#getItem()} to access the {@link ICalendarItem}.
   */
  @ConfigOperation
  @Order(20)
  protected void execFilterCalendarItems(Set<Class<? extends ICalendarItemProvider>> changedProviderTypes, Map<Class<? extends ICalendarItemProvider>, Collection<CalendarComponent>> componentsByProvider) {
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
    m_uiFacade = new P_UIFacade();
    m_contributionHolder = new ContributionComposite(this);
    setTitle(getConfiguredTitle());
    setSelectedDate(new Date());
    setStartHour(getConfiguredStartHour());
    setEndHour(getConfiguredEndHour());
    setUseOverflowCells(getConfiguredUseOverflowCells());
    setShowDisplayModeSelection(getConfiguredShowDisplayModeSelection());
    setMarkNoonHour(getConfiguredMarkNoonHour());
    setMarkOutOfMonthDays(getConfiguredMarkOutOfMonthDays());

    // menus
    List<Class<? extends IMenu>> declaredMenus = getDeclaredMenus();
    OrderedCollection<IMenu> menus = new OrderedCollection<IMenu>();
    for (Class<? extends IMenu> menuClazz : declaredMenus) {
      try {
        IMenu menu = ConfigurationUtility.newInnerInstance(this, menuClazz);
        menus.addOrdered(menu);
      }
      catch (Exception e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("error creating instance of class '" + menuClazz.getName() + "'.", e));
      }
    }
    List<IMenu> contributedMenus = m_contributionHolder.getContributionsByClass(IMenu.class);
    menus.addAllOrdered(contributedMenus);

    // producers
    List<Class<? extends ICalendarItemProvider>> configuredProducers = getConfiguredProducers();
    List<ICalendarItemProvider> contributedProducers = m_contributionHolder.getContributionsByClass(ICalendarItemProvider.class);

    List<ICalendarItemProvider> producerList = new ArrayList<ICalendarItemProvider>(configuredProducers.size() + contributedProducers.size());
    for (Class<? extends ICalendarItemProvider> itemProviderClazz : configuredProducers) {
      try {
        ICalendarItemProvider provider = ConfigurationUtility.newInnerInstance(this, itemProviderClazz);
        producerList.add(provider);
        // add empty space menus to the context menu
        menus.addAllOrdered(ActionUtility.getActions(provider.getMenus(), ActionUtility.createMenuFilterMenuTypes(CollectionUtility.hashSet(CalendarMenuType.EmptySpace), false)));
      }
      catch (Exception e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("error creating instance of class '" + itemProviderClazz.getName() + "'.", e));
      }
    }
    producerList.addAll(contributedProducers);
    m_providers = producerList;

    try {
      injectMenusInternal(menus);
    }
    catch (Exception e) {
      LOG.error("error occured while dynamically contributing menus.", e);
    }
    new MoveActionNodesHandler<IMenu>(menus).moveModelObjects();
    ICalendarContextMenu contextMenu = new CalendarContextMenu(this, menus.getOrderedList());
    setContextMenu(contextMenu);

    // attach change listener for item updates
    for (final ICalendarItemProvider p : m_providers) {
      p.addPropertyChangeListener(
          new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
              if (e.getPropertyName().equals(ICalendarItemProvider.PROP_ITEMS)) {
                List<ICalendarItemProvider> modified = new ArrayList<ICalendarItemProvider>(1);
                modified.add(p);
                updateComponentsInternal(modified);
              }
              else if (e.getPropertyName().equals(ICalendarItemProvider.PROP_LOAD_IN_PROGRESS)) {
                updateLoadInProgressInternal();
              }
            }
          }
          );
    }
  }

  @Override
  public final List<? extends ICalendarExtension<? extends AbstractCalendar>> getAllExtensions() {
    return m_objectExtensions.getAllExtensions();
  }

  protected ICalendarExtension<? extends AbstractCalendar> createLocalExtension() {
    return new LocalCalendarExtension<AbstractCalendar>(this);
  }

  @Override
  public <T extends IExtension<?>> T getExtension(Class<T> c) {
    return m_objectExtensions.getExtension(c);
  }

  /**
   * Override this internal method only in order to make use of dynamic menus<br>
   * Used to manage menu list and add/remove menus.<br>
   * To change the order or specify the insert position use {@link IMenu#setOrder(double)}.
   *
   * @param menus
   *          live and mutable collection of configured menus
   */
  protected void injectMenusInternal(OrderedCollection<IMenu> menus) {
  }

  /*
   * Runtime
   */

  /**
   * This is the init of the runtime model after the table and columns are built
   * and configured
   */
  @Override
  public void initCalendar() throws ProcessingException {
    // init menus
    ActionUtility.initActions(getMenus());
    interceptInitCalendar();
    /*
     * add property change listener to - reload calendar items when view range
     * changes
     */
    addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals(PROP_VIEW_RANGE)) {
          updateComponentsInternal(m_providers);
        }
      }
    });
    updateComponentsInternal(m_providers);
  }

  private void disposeCalendarInternal() {
    for (ICalendarItemProvider p : m_providers) {
      try {
        p.disposeProvider();
      }
      catch (Throwable t) {
        LOG.warn(p.getClass().getName(), t);
      }
    }
  }

  @Override
  public void disposeCalendar() {
    disposeCalendarInternal();
    try {
      interceptDisposeCalendar();
    }
    catch (Throwable t) {
      LOG.warn(getClass().getName(), t);
    }
  }

  @Override
  public String getTitle() {
    return propertySupport.getPropertyString(PROP_TITLE);
  }

  @Override
  public void setTitle(String s) {
    propertySupport.setPropertyString(PROP_TITLE, s);
  }

  @Override
  public int getEndHour() {
    return (Integer) propertySupport.getProperty(PROP_END_HOUR);
  }

  @Override
  public void setEndHour(int hour) {
    propertySupport.setProperty(PROP_END_HOUR, hour);
  }

  @Override
  public int getStartHour() {
    return (Integer) propertySupport.getProperty(PROP_START_HOUR);
  }

  @Override
  public void setStartHour(int hour) {
    propertySupport.setProperty(PROP_START_HOUR, hour);
  }

  @Override
  public boolean getUseOverflowCells() {
    return (Boolean) propertySupport.getProperty(PROP_USE_OVERFLOW_CELLS);
  }

  @Override
  public void setUseOverflowCells(boolean useOverflowCells) {
    propertySupport.setProperty(PROP_USE_OVERFLOW_CELLS, useOverflowCells);
  }

  @Override
  public boolean getShowDisplayModeSelection() {
    return (Boolean) propertySupport.getProperty(PROP_SHOW_DISPLAY_MODE_SELECTION);
  }

  @Override
  public void setShowDisplayModeSelection(boolean showDisplayModeSelection) {
    propertySupport.setProperty(PROP_SHOW_DISPLAY_MODE_SELECTION, showDisplayModeSelection);
  }

  @Override
  public boolean getMarkNoonHour() {
    return (Boolean) propertySupport.getProperty(PROP_MARK_NOON_HOUR);
  }

  @Override
  public void setMarkNoonHour(boolean markNoonHour) {
    propertySupport.setProperty(PROP_MARK_NOON_HOUR, markNoonHour);
  }

  @Override
  public boolean getMarkOutOfMonthDays() {
    return (Boolean) propertySupport.getProperty(PROP_MARK_OUT_OF_MONTH_DAYS);
  }

  @Override
  public void setMarkOutOfMonthDays(boolean markOutOfMonthDays) {
    propertySupport.setProperty(PROP_MARK_OUT_OF_MONTH_DAYS, markOutOfMonthDays);
  }

  @Override
  public boolean isLoadInProgress() {
    return propertySupport.getPropertyBool(PROP_LOAD_IN_PROGRESS);
  }

  @Override
  public void setLoadInProgress(boolean b) {
    propertySupport.setPropertyBool(PROP_LOAD_IN_PROGRESS, b);
  }

  @Override
  public boolean isCalendarChanging() {
    return m_calendarChanging > 0;
  }

  @Override
  public void setCalendarChanging(boolean b) {
    // use a stack counter because setTableChanging might be called in nested
    // loops
    if (b) {
      m_calendarChanging++;
      if (m_calendarChanging == 1) {
        // 0 --> 1
        propertySupport.setPropertiesChanging(true);
      }
    }
    else {
      if (m_calendarChanging > 0) {
        m_calendarChanging--;
        if (m_calendarChanging == 0) {
          try {
            processChangeBuffer();
          }
          finally {
            propertySupport.setPropertiesChanging(false);
          }
        }
      }
    }
  }

  private void processChangeBuffer() {
    /*
     * fire events tree changes are finished now, fire all buffered events and
     * call lookups
     */
    m_calendarEventBuffer = new ArrayList<CalendarEvent>();
    // coalesce ITEMS_CHANGED,ITEM_SELECTED events
    Set<Integer> types = new HashSet<Integer>();
    List<CalendarEvent> coalescedEvents = new LinkedList<CalendarEvent>();
    // reverse traversal
    CalendarEvent[] a = m_calendarEventBuffer.toArray(new CalendarEvent[0]);
    for (int i = a.length - 1; i >= 0; i--) {
      switch (a[i].getType()) {
        case CalendarEvent.TYPE_COMPONENT_ACTION: {
          if (!types.contains(a[i].getType())) {
            coalescedEvents.add(0, a[i]);
            types.add(a[i].getType());
          }
          break;
        }
        default: {
          coalescedEvents.add(0, a[i]);
        }
      }
    }
    // fire the batch
    fireCalendarEventBatchInternal(coalescedEvents);
  }

  @Override
  public List<IMenu> getMenus() {
    return getContextMenu().getChildActions();
  }

  protected void setContextMenu(ICalendarContextMenu contextMenu) {
    propertySupport.setProperty(PROP_CONTEXT_MENU, contextMenu);
  }

  @Override
  public ICalendarContextMenu getContextMenu() {
    return (ICalendarContextMenu) propertySupport.getProperty(PROP_CONTEXT_MENU);
  }

  @Override
  public List<ICalendarItemProvider> getCalendarItemProviders() {
    return CollectionUtility.arrayList(m_providers);
  }

  @Override
  public int getDisplayMode() {
    return propertySupport.getPropertyInt(PROP_DISPLAY_MODE);
  }

  @Override
  public void setDisplayMode(int mode) {
    propertySupport.setPropertyInt(PROP_DISPLAY_MODE, mode);
  }

  @Override
  public boolean isDisplayCondensed() {
    return propertySupport.getPropertyBool(PROP_DISPLAY_CONDENSED);
  }

  @Override
  public void setDisplayCondensed(boolean condensed) {
    propertySupport.setPropertyBool(PROP_DISPLAY_CONDENSED, condensed);
  }

  @Override
  public Range<Date> getViewRange() {
    @SuppressWarnings("unchecked")
    Range<Date> propValue = (Range<Date>) propertySupport.getProperty(PROP_VIEW_RANGE);
    // return a copy
    return new Range<Date>(propValue);
  }

  @Override
  public void setViewRange(Date minDate, Date maxDate) {
    setViewRangeInternal(new Range<Date>(minDate, maxDate));
  }

  @Override
  public void setViewRange(Range<Date> viewRange) {
    setViewRangeInternal(new Range<Date>(viewRange));
  }

  private void setViewRangeInternal(Range<Date> viewRange) {
    propertySupport.setProperty(PROP_VIEW_RANGE, viewRange);
  }

  @Override
  public Date getSelectedDate() {
    return (Date) propertySupport.getProperty(PROP_SELECTED_DATE);
  }

  @Override
  public void setSelectedDate(Date d) {
    propertySupport.setProperty(PROP_SELECTED_DATE, d);
  }

  @Override
  public CalendarComponent getSelectedComponent() {
    return (CalendarComponent) propertySupport.getProperty(PROP_SELECTED_COMPONENT);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ICalendarItem> T getSelectedItem(Class<T> c) {
    CalendarComponent comp = getSelectedComponent();
    if (comp != null && comp.getItem() != null) {
      if (c.isAssignableFrom(comp.getItem().getClass())) {
        return (T) comp.getItem();
      }
    }
    return null;
  }

  @Override
  public void setSelectedComponent(CalendarComponent comp) {
    comp = resolveComponent(comp);
    // update temporarily added menus of current content provider
    ICalendarItemProvider provider = null;
    if (comp != null) {
      provider = comp.getProvider();
    }
    updateContentProviderMenus(provider);
    propertySupport.setProperty(PROP_SELECTED_COMPONENT, comp);
  }

  /**
   * @param provider
   */
  protected void updateContentProviderMenus(ICalendarItemProvider provider) {
    // remove old
    if (m_inheritedMenusOfSelectedProvider != null) {
      getContextMenu().removeChildActions(m_inheritedMenusOfSelectedProvider);
      m_inheritedMenusOfSelectedProvider = null;
    }
    // add menus of provider
    if (provider != null) {
      m_inheritedMenusOfSelectedProvider = ActionUtility.getActions(provider.getMenus(), ActionUtility.createMenuFilterMenuTypes(CollectionUtility.hashSet(CalendarMenuType.CalendarComponent), false));
      getContextMenu().addChildActions(m_inheritedMenusOfSelectedProvider);
    }
  }

  private CalendarComponent resolveComponent(CalendarComponent comp) {
    return comp;
  }

  @Override
  public DateTimeFormatFactory getDateTimeFormatFactory() {
    return m_dateTimeFormatFactory;
  }

  @Override
  public Set<CalendarComponent> getComponents() {
    return CollectionUtility.hashSet(propertySupport.<CalendarComponent> getPropertySet(PROP_COMPONENTS));
  }

  private void updateComponentsInternal(List<ICalendarItemProvider> changedProviders) {
    Range<Date> d = getViewRange();
    if (d.getFrom() != null && d.getTo() != null) {
      for (ICalendarItemProvider p : changedProviders) {
        LinkedList<CalendarComponent> components = new LinkedList<CalendarComponent>();
        for (ICalendarItem item : p.getItems(d.getFrom(), d.getTo())) {
          Cell cell = new Cell();
          p.decorateCell(cell, item);
          components.add(new CalendarComponent(this, p, item, cell));
        }
        m_componentsByProvider.put(p.getClass(), components);
      }
      // filter and resolve item conflicts
      Set<Class<? extends ICalendarItemProvider>> providerTypes = new HashSet<Class<? extends ICalendarItemProvider>>(changedProviders.size());
      for (ICalendarItemProvider provider : changedProviders) {
        providerTypes.add(provider.getClass());
      }
      interceptFilterCalendarItems(providerTypes, m_componentsByProvider);
      // complete list
      TreeMap<CompositeObject, CalendarComponent> sortMap = new TreeMap<CompositeObject, CalendarComponent>();
      int index = 0;
      for (Collection<CalendarComponent> c : m_componentsByProvider.values()) {
        for (CalendarComponent comp : c) {
          sortMap.put(new CompositeObject(comp.getFromDate(), index++), comp);
        }
      }
      propertySupport.setPropertySet(PROP_COMPONENTS, CollectionUtility.hashSet(sortMap.values()));
      // validate selection
      setSelectedComponent(getSelectedComponent());
    }
  }

  @Override
  public Object getContainer() {
    return propertySupport.getProperty(PROP_CONTAINER);
  }

  /**
   * do not use this internal method unless you are implementing a container that holds and controls an
   * {@link ICalendar}
   */
  public void setContainerInternal(Object container) {
    propertySupport.setProperty(PROP_CONTAINER, container);
  }

  /**
   * @param componentsByProvider
   * @param providerTypes
   *          {@link ICalendarItemProvider} classes
   * @return
   */
  public Collection<CalendarItemConflict> findConflictingItems(Map<Class<? extends ICalendarItemProvider>, Collection<CalendarComponent>> componentsByProvider, Class<?>... providerTypes) {
    if (providerTypes != null && providerTypes.length >= 2) {
      Map<String, List<CalendarComponent>> classificationMap = new HashMap<String, List<CalendarComponent>>();
      for (int i = 0; i < providerTypes.length; i++) {
        Collection<CalendarComponent> a = componentsByProvider.get(providerTypes[i]);
        if (a != null) {
          for (CalendarComponent comp : a) {
            String key = StringUtility.emptyIfNull(comp.getItem().getSubject()).toLowerCase().trim();
            List<CalendarComponent> list = classificationMap.get(key);
            if (list == null) {
              list = new ArrayList<CalendarComponent>();
              classificationMap.put(key, list);
            }
            list.add(comp);
          }
        }
      }
      List<CalendarItemConflict> conflicts = new ArrayList<CalendarItemConflict>();
      for (Map.Entry<String, List<CalendarComponent>> e : classificationMap.entrySet()) {
        if (e.getValue().size() >= 2) {
          List<CalendarComponent> list = e.getValue();
          // find CalendarComponents with same Provider, break them up in separate groups for duplicate check
          // reason: all CalendarComponents of the same provider are assumed to be distinct
          Map<ICalendarItemProvider, ArrayList<CalendarComponent>> groups = new HashMap<ICalendarItemProvider, ArrayList<CalendarComponent>>();
          for (CalendarComponent c : list) {
            if (groups.containsKey(c.getProvider())) {
              groups.get(c.getProvider()).add(c);
            }
            else {
              ArrayList<CalendarComponent> tmp = new ArrayList<CalendarComponent>();
              tmp.add(c);
              groups.put(c.getProvider(), tmp);
            }
          }
          List<CalendarComponent> groupComp = new ArrayList<CalendarComponent>();
          for (ArrayList<CalendarComponent> g : groups.values()) {
            if (g.size() > 1) {
              groupComp.addAll(g);
            }
          }
          if (groupComp.size() == 0) {
            // no duplicate records of a provider found, start with first item
            groupComp.add(list.get(0));
          }
          for (CalendarComponent ref : groupComp) {
            List<CalendarComponent> matchList = new ArrayList<CalendarComponent>();
            double matchSum = 0;
            matchList.add(ref);
            for (CalendarComponent test : list) {
              if (ref == test || test.getProvider() == ref.getProvider()) {
                continue;
              }
              if (DateUtility.intersects(test.getFromDate(), test.getToDate(), ref.getFromDate(), ref.getToDate())) {
                matchList.add(test);
                double minOfStart = Math.min(test.getFromDate().getTime(), ref.getFromDate().getTime());
                double maxOfStart = Math.max(test.getFromDate().getTime(), ref.getFromDate().getTime());
                double minOfEnd = Math.min(test.getToDate().getTime(), ref.getToDate().getTime());
                double maxOfEnd = Math.max(test.getToDate().getTime(), ref.getToDate().getTime());
                if (maxOfEnd - minOfStart > 1e-6) {
                  matchSum += (minOfEnd - maxOfStart) / (maxOfEnd - minOfStart);
                }
                else {
                  matchSum += 1.0d;
                }
              }
            }
            if (matchList.size() >= 2) {
              conflicts.add(new CalendarItemConflict(componentsByProvider, matchList, matchSum / (matchList.size() - 1)));
            }
          }
        }
      }
      return conflicts;
    }
    else {
      return CollectionUtility.emptyArrayList();
    }
  }

  private void updateLoadInProgressInternal() {
    boolean b = false;
    for (ICalendarItemProvider p : m_providers) {
      if (p.isLoadInProgress()) {
        b = true;
        break;
      }
    }
    setLoadInProgress(b);
  }

  @Override
  public void reloadCalendarItems() {
    for (ICalendarItemProvider p : m_providers) {
      p.reloadProvider();
    }
  }

  /*
   * Property Observer
   */

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(listener);
  }

  @Override
  public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(propertyName, listener);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(listener);
  }

  @Override
  public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(propertyName, listener);
  }

  /**
   * Model Observer
   */
  @Override
  public void addCalendarListener(CalendarListener listener) {
    m_listenerList.add(CalendarListener.class, listener);
  }

  @Override
  public void removeCalendarListener(CalendarListener listener) {
    m_listenerList.remove(CalendarListener.class, listener);
  }

  private void fireCalendarComponentAction() {
    CalendarComponent comp = getSelectedComponent();
    if (comp != null) {
      // single observer exec
      try {
        comp.getProvider().onItemAction(comp.getItem());
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
      fireCalendarEventInternal(new CalendarEvent(this, CalendarEvent.TYPE_COMPONENT_ACTION, comp));
    }
  }

  // main handler
  private void fireCalendarEventInternal(CalendarEvent e) {
    if (isCalendarChanging()) {
      // buffer the event for later batch firing
      m_calendarEventBuffer.add(e);
    }
    else {
      EventListener[] listeners = m_listenerList.getListeners(CalendarListener.class);
      if (listeners != null && listeners.length > 0) {
        for (int i = 0; i < listeners.length; i++) {
          ((CalendarListener) listeners[i]).calendarChanged(e);
        }
      }
    }
  }

  // batch handler
  private void fireCalendarEventBatchInternal(List<CalendarEvent> batch) {
    if (isCalendarChanging()) {
      LOG.error("Illegal State: firing a event batch while calendar is changing");
    }
    else {
      EventListener[] listeners = m_listenerList.getListeners(CalendarListener.class);
      if (listeners != null && listeners.length > 0) {
        for (int i = 0; i < listeners.length; i++) {
          ((CalendarListener) listeners[i]).calendarChangedBatch(batch);
        }
      }
    }
  }

  @Override
  public ICalendarUIFacade getUIFacade() {
    return m_uiFacade;
  }

  /*
   * UI Notifications
   */
  private class P_UIFacade implements ICalendarUIFacade {
    private int m_uiProcessorCount = 0;

    protected void pushUIProcessor() {
      m_uiProcessorCount++;
    }

    protected void popUIProcessor() {
      m_uiProcessorCount--;
    }

    @Override
    public boolean isUIProcessing() {
      return m_uiProcessorCount > 0;
    }

    @Override
    public void setSelectionFromUI(Date d, CalendarComponent comp) {
      try {
        pushUIProcessor();
        //
        setSelectedDate(d);
        setSelectedComponent(comp);
      }
      finally {
        popUIProcessor();
      }
    }

    @Override
    public void setVisibleRangeFromUI(Range<Date> dateRange) {
      setVisibleRangeFromUI(dateRange.getFrom(), dateRange.getTo());
    }

    @Override
    public void setVisibleRangeFromUI(Date minDate, Date maxDate) {
      try {
        pushUIProcessor();
        //
        setViewRange(minDate, maxDate);
      }
      finally {
        popUIProcessor();
      }
    }

    @Override
    public void fireReloadFromUI() {
      try {
        pushUIProcessor();
        //
        reloadCalendarItems();
      }
      finally {
        popUIProcessor();
      }
    }

    @Override
    public void fireComponentActionFromUI() {
      try {
        pushUIProcessor();
        //
        fireCalendarComponentAction();
      }
      finally {
        popUIProcessor();
      }
    }

    @Override
    public void fireComponentMovedFromUI(CalendarComponent comp, Date newDate) {
      try {
        pushUIProcessor();
        //
        comp = resolveComponent(comp);
        if (comp != null) {
          try {
            comp.getProvider().onItemMoved(comp.getItem(), newDate);
          }
          catch (ProcessingException e) {
            SERVICES.getService(IExceptionHandlerService.class).handleException(e);
          }
          catch (Throwable e) {
            SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Unexpected", e));
          }
        }
        fireCalendarComponentAction();
      }
      finally {
        popUIProcessor();
      }
    }
  }

  /**
   * The extension delegating to the local methods. This Extension is always at the end of the chain and will not call
   * any further chain elements.
   */
  protected static class LocalCalendarExtension<OWNER extends AbstractCalendar> extends AbstractExtension<OWNER> implements ICalendarExtension<OWNER> {

    public LocalCalendarExtension(OWNER owner) {
      super(owner);
    }

    @Override
    public void execFilterCalendarItems(CalendarFilterCalendarItemsChain chain, Set<Class<? extends ICalendarItemProvider>> changedProviderTypes, Map<Class<? extends ICalendarItemProvider>, Collection<CalendarComponent>> componentsByProvider) {
      getOwner().execFilterCalendarItems(changedProviderTypes, componentsByProvider);
    }

    @Override
    public void execDisposeCalendar(CalendarDisposeCalendarChain chain) throws ProcessingException {
      getOwner().execDisposeCalendar();
    }

    @Override
    public void execInitCalendar(CalendarInitCalendarChain chain) throws ProcessingException {
      getOwner().execInitCalendar();
    }

  }

  protected final void interceptFilterCalendarItems(Set<Class<? extends ICalendarItemProvider>> changedProviderTypes, Map<Class<? extends ICalendarItemProvider>, Collection<CalendarComponent>> componentsByProvider) {
    List<? extends ICalendarExtension<? extends AbstractCalendar>> extensions = getAllExtensions();
    CalendarFilterCalendarItemsChain chain = new CalendarFilterCalendarItemsChain(extensions);
    chain.execFilterCalendarItems(changedProviderTypes, componentsByProvider);
  }

  protected final void interceptDisposeCalendar() throws ProcessingException {
    List<? extends ICalendarExtension<? extends AbstractCalendar>> extensions = getAllExtensions();
    CalendarDisposeCalendarChain chain = new CalendarDisposeCalendarChain(extensions);
    chain.execDisposeCalendar();
  }

  protected final void interceptInitCalendar() throws ProcessingException {
    List<? extends ICalendarExtension<? extends AbstractCalendar>> extensions = getAllExtensions();
    CalendarInitCalendarChain chain = new CalendarInitCalendarChain(extensions);
    chain.execInitCalendar();
  }
}
