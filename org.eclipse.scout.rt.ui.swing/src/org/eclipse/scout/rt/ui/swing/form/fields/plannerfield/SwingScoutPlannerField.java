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
package org.eclipse.scout.rt.ui.swing.form.fields.plannerfield;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import javax.swing.table.JTableHeader;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.OptimisticLock;
import org.eclipse.scout.rt.client.ui.basic.activitymap.IActivityMap;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.form.fields.plannerfield.IPlannerField;
import org.eclipse.scout.rt.ui.swing.SwingUtility;
import org.eclipse.scout.rt.ui.swing.basic.activitymap.SwingScoutActivityMap;
import org.eclipse.scout.rt.ui.swing.basic.table.ISwingScoutTable;
import org.eclipse.scout.rt.ui.swing.ext.JPanelEx;
import org.eclipse.scout.rt.ui.swing.ext.calendar.CalendarViewEvent;
import org.eclipse.scout.rt.ui.swing.ext.calendar.CalendarViewListener;
import org.eclipse.scout.rt.ui.swing.ext.calendar.DateChooser;
import org.eclipse.scout.rt.ui.swing.form.fields.SwingScoutFieldComposite;
import org.eclipse.scout.rt.ui.swing.form.fields.plannerfield.layout.PlannerFieldLayout;
import org.eclipse.scout.rt.ui.swing.form.fields.plannerfield.layout.PlannerFieldLayoutConstraints;

public class SwingScoutPlannerField extends SwingScoutFieldComposite<IPlannerField<?, ?, ?, ?>> implements ISwingScoutPlannerField {
  private ISwingScoutTable m_resourceTableComposite;
  private SwingScoutActivityMap m_activityMapComposite;
  private DateChooser[] m_miniDateChooser;
  private JPanel m_miniCalPanel;
  private JSplitPane m_hsplit;
  private P_SwingMiniCalendarChangeListener m_swingMiniCalendarChangeListener;
  private static final String SPLITTER_PROPERTY_NAME = "dividerLocation";

  public SwingScoutPlannerField() {
  }

  @Override
  @SuppressWarnings("serial")
  protected void initializeSwing() {
    JPanel container = new JPanel(new PlannerFieldLayout(getSwingEnvironment(), getScoutObject().getGridData()));
    container.setOpaque(false);
    ITable scoutTable = getScoutObject().getResourceTable();
    m_resourceTableComposite = getSwingEnvironment().createTable(scoutTable);
    m_resourceTableComposite.createField(scoutTable, getSwingEnvironment());
    JTableHeader h = m_resourceTableComposite.getSwingTable().getTableHeader();
    if (h != null) {
      h.setPreferredSize(new Dimension(h.getPreferredSize().width, h.getFontMetrics(h.getFont()).getHeight() * 5 / 2));
      h.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
          if (m_activityMapComposite != null) {
            m_activityMapComposite.getSwingActivityMap().revalidate();
            m_activityMapComposite.getSwingActivityMap().repaint();
          }
        }
      });
    }
    m_activityMapComposite = new SwingScoutActivityMap(m_resourceTableComposite.getSwingTable());
    m_activityMapComposite.createField(getScoutObject().getActivityMap(), getSwingEnvironment());
    m_activityMapComposite.setSwingPlannerField(this);
    m_hsplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_resourceTableComposite.getSwingScrollPane(), m_activityMapComposite.getSwingScrollPane());
    m_hsplit.setBorder(null);
    m_hsplit.setDividerLocation(getScoutObject().getSplitterPosition());
    m_hsplit.setDividerSize(3);

    m_hsplit.addPropertyChangeListener(new P_SplitterChangeListener());

    container.add(m_hsplit, new PlannerFieldLayoutConstraints(PlannerFieldLayoutConstraints.PLANNER));
    // mediate v-scrolling
    JScrollBar resourceTableScrollBar = m_resourceTableComposite.getSwingScrollPane().getVerticalScrollBar();
    JScrollBar activityMapScrollBar = m_activityMapComposite.getSwingScrollPane().getVerticalScrollBar();
    P_SwingScrollSyncListener sl = new P_SwingScrollSyncListener(resourceTableScrollBar, activityMapScrollBar);
    resourceTableScrollBar.addAdjustmentListener(sl);
    activityMapScrollBar.addAdjustmentListener(sl);
    // disable activity h-bar
    m_activityMapComposite.getSwingScrollPane().getHorizontalScrollBar().setEnabled(false);
    // mini cals
    m_miniDateChooser = new DateChooser[0];
    m_miniCalPanel = new JPanelEx();
    container.add(m_miniCalPanel, new PlannerFieldLayoutConstraints(PlannerFieldLayoutConstraints.MINI_CALENDARS));
    // F5 key for refresh
    container.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(SwingUtility.createKeystroke("F5"), "refresh");
    container.getActionMap().put("refresh",
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            // notify Scout
            Runnable t = new Runnable() {
              @Override
              public void run() {
                getScoutObject().getUIFacade().refreshFromUI();
              }
            };
            getSwingEnvironment().invokeScoutLater(t, 0);
            // end notify
          }
        }
        );
    //
    setSwingContainer(container);
    setSwingField(container);
  }

  @Override
  protected void attachScout() {
    super.attachScout();
    setMiniCalendarCountFromScout(getScoutObject().getMiniCalendarCount());
  }

  @Override
  public ISwingScoutTable getResourceTableComposite() {
    return m_resourceTableComposite;
  }

  @Override
  public SwingScoutActivityMap getActivityMapComposite() {
    return m_activityMapComposite;
  }

  protected void setMiniCalendarCountFromScout(int calCount) {
    // changed anyway?
    if (m_miniDateChooser.length == calCount) {
      return;
    }
    // remove old
    if (m_swingMiniCalendarChangeListener != null) {
      for (int i = 0; i < m_miniDateChooser.length; i++) {
        m_miniDateChooser[i].removeCalendarViewListener(m_swingMiniCalendarChangeListener);
      }
    }
    m_miniCalPanel.removeAll();
    m_miniCalPanel.setLayout(new GridLayout(calCount, 1));
    // create new
    m_miniDateChooser = new DateChooser[calCount];
    if (m_miniDateChooser.length > 0) {
      if (m_swingMiniCalendarChangeListener == null) {
        m_swingMiniCalendarChangeListener = new P_SwingMiniCalendarChangeListener();
      }
      int workdayCount = getScoutObject().getActivityMap().getWorkDayCount();
      for (int i = 0; i < m_miniDateChooser.length; i++) {
        m_miniDateChooser[i] = new DateChooser(false);
        m_miniDateChooser[i].setMultiSelect(true);
        m_miniDateChooser[i].addCalendarViewListener(m_swingMiniCalendarChangeListener);
        m_miniDateChooser[i].setWorkDayCount(workdayCount);
        m_miniCalPanel.add(m_miniDateChooser[i].getContainer());
        // master/detail
        if (i > 0) {
          m_miniDateChooser[i - 1].setChildCalendar(m_miniDateChooser[i]);
        }
      }
      updateSelectedDatesInMiniCalendar();
    }
  }

  protected void setWorkDayCountFromScout(int workDayCount) {
    for (int i = 0; i < m_miniDateChooser.length; i++) {
      m_miniDateChooser[i].setWorkDayCount(workDayCount);
    }
  }

  protected void setSplitterLocation(int splitterLocation) {
    m_hsplit.setDividerLocation(splitterLocation);
  }

  public DateChooser[] getMiniCalendar() {
    return m_miniDateChooser;
  }

  /**
   * Updates the selected dates in the miniCalendar. The selected dates from the ActivityMap are synchronized to the
   * miniCalendars.
   *
   * @since 4.1.0
   */
  public void updateSelectedDatesInMiniCalendar() {
    // set selection on first calendar
    Date[] days = m_activityMapComposite.getScoutActivityMap().getDays();
    if (CollectionUtility.hasElements(m_miniDateChooser)) {
      m_miniDateChooser[0].setSelectedDates(days);
    }
  }

  /**
   * scout property handler override
   */
  @Override
  protected void handleScoutPropertyChange(String name, Object newValue) {
    super.handleScoutPropertyChange(name, newValue);
    if (IPlannerField.PROP_MINI_CALENDAR_COUNT.equals(name)) {
      setMiniCalendarCountFromScout((Integer) newValue);
    }
    if (IPlannerField.PROP_SPLITTER_POSITION.equals(name)) {
      setSplitterLocation((Integer) newValue);
    }
  }

  private class P_SwingScrollSyncListener implements AdjustmentListener {
    private OptimisticLock m_syncLock = new OptimisticLock();
    private JScrollBar m_bar1;
    private JScrollBar m_bar2;

    public P_SwingScrollSyncListener(JScrollBar bar1, JScrollBar bar2) {
      m_bar1 = bar1;
      m_bar2 = bar2;
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
      try {
        if (m_syncLock.acquire()) {
          if (e.getSource() == m_bar1) {
            m_bar2.setValue(e.getValue());
          }
          else if (e.getSource() == m_bar2) {
            m_bar1.setValue(e.getValue());
          }
        }
      }
      finally {
        m_syncLock.release();
      }
    }
  }// end private class

  private class P_SwingMiniCalendarChangeListener implements CalendarViewListener {
    @Override
    public void viewChanged(CalendarViewEvent e) {
      switch (e.getType()) {
        case CalendarViewEvent.TYPE_SELECTION_CHANGED: {
          if (getUpdateSwingFromScoutLock().isAcquired()) {
            return;
          }
          //
          final Date[] dates = e.getSource().getSelectedDates();
          // check swing-side lock
          // notify scout
          Runnable t = new Runnable() {
            @Override
            public void run() {
              m_activityMapComposite.getScoutActivityMap().getUIFacade().setDaysFromUI(dates);
            }
          };

          getSwingEnvironment().invokeScoutLater(t, 0);
          break;
        }
      }
    }
  }// end private class

  /*
   * in addition to planner property changes, also listen on activity map
   * property changes
   */
  private class P_ScoutActivityMapPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void propertyChange(final PropertyChangeEvent e) {
      if (e.getPropertyName().equals(IActivityMap.PROP_WORK_DAY_COUNT)) {
        Runnable t = new Runnable() {
          @Override
          public void run() {
            setWorkDayCountFromScout((Integer) e.getNewValue());
          }
        };
        getSwingEnvironment().invokeSwingLater(t);
      }
    }
  }// end private class

  private class P_SplitterChangeListener implements PropertyChangeListener {
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
      if (SPLITTER_PROPERTY_NAME.equals(event.getPropertyName())) {
        final Integer splitterLocation = (Integer) event.getNewValue();
        Runnable t = new Runnable() {
          @Override
          public void run() {
            getScoutObject().getUIFacade().setSplitterPositionFromUI(splitterLocation);
          }
        };
        getSwingEnvironment().invokeScoutLater(t, 0);
      }
    }
  }
}
