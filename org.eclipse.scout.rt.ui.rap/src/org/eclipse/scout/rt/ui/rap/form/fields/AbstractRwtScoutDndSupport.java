/*******************************************************************************
 * Copyright (c) 2011 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.scout.rt.ui.rap.form.fields;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.rap.rwt.dnd.ClientFileTransfer;
import org.eclipse.scout.commons.beans.IPropertyObserver;
import org.eclipse.scout.commons.dnd.TransferObject;
import org.eclipse.scout.rt.client.ui.IDNDSupport;
import org.eclipse.scout.rt.ui.rap.IRwtEnvironment;
import org.eclipse.scout.rt.ui.rap.dnd.IRwtScoutFileUploadHandler;
import org.eclipse.scout.rt.ui.rap.dnd.RwtScoutFileUploadEvent;
import org.eclipse.scout.rt.ui.rap.dnd.RwtScoutFileUploadHandlerFactory;
import org.eclipse.scout.rt.ui.rap.util.RwtUtility;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Control;

/**
 * <h3>AbstractRwtScoutDndSupport</h3> ...
 * 
 * @since 3.7.0 June 2011
 */
public abstract class AbstractRwtScoutDndSupport implements IRwtScoutDndSupport, IRwtScoutDndUploadCallback {

  private final Control m_control;
  private final IPropertyObserver m_scoutObject;
  private final IDNDSupport m_scoutDndSupportable;
  private IRwtEnvironment m_uiEnvironment;
  private DropTargetListener m_dropTargetListener;
  private DragSourceListener m_dragSourceListener;
  private PropertyChangeListener m_scoutPropertyListener;
  private Transfer[] m_dragTransferTypes;
  private Transfer[] m_dropTransferTypes;
  private boolean m_isClientFileTransferSupported;
  private IRwtScoutFileUploadHandler m_uploadHandler;

  public AbstractRwtScoutDndSupport(IPropertyObserver scoutObject, IDNDSupport scoutDndSupportable, Control control, IRwtEnvironment uiEnvironment) {
    m_scoutObject = scoutObject;
    m_scoutDndSupportable = scoutDndSupportable;
    m_control = control;
    m_uiEnvironment = uiEnvironment;
    attachScout();
    m_control.addDisposeListener(new DisposeListener() {
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetDisposed(DisposeEvent e) {
        detachScout();
      }
    });
  }

  protected void attachScout() {
    m_scoutPropertyListener = new P_ScoutObjectPropertyListener();
    m_scoutObject.addPropertyChangeListener(m_scoutPropertyListener);
    updateDragSupportFromScout();
    updateDropSupportFromScout();
  }

  protected void detachScout() {
    m_scoutObject.removePropertyChangeListener(m_scoutPropertyListener);

    if (m_dragTransferTypes != null) {
      DragSource dragSource = (DragSource) m_control.getData(DND_DRAG_SOURCE);
      if (dragSource != null && !dragSource.isDisposed()) {
        dragSource.removeDragListener(m_dragSourceListener);
        ArrayList<Transfer> types = new ArrayList<Transfer>(Arrays.asList(dragSource.getTransfer()));
        for (Transfer t : m_dragTransferTypes) {
          types.remove(t);
        }
        if (types.size() > 0) {
          dragSource.setTransfer(types.toArray(new Transfer[types.size()]));
        }
        else {
          dragSource.dispose();
          m_control.setData(DND_DRAG_SOURCE, null);
        }
      }
    }
    if (m_dropTransferTypes != null) {
      DropTarget dropTarget = (DropTarget) m_control.getData(DND_DROP_TARGET);
      if (dropTarget != null && !dropTarget.isDisposed()) {
        dropTarget.removeDropListener(m_dropTargetListener);
        ArrayList<Transfer> types = new ArrayList<Transfer>(Arrays.asList(dropTarget.getTransfer()));
        for (Transfer t : m_dropTransferTypes) {
          types.remove(t);
        }
        if (types.size() > 0) {
          dropTarget.setTransfer(types.toArray(new Transfer[types.size()]));
        }
        else {
          dropTarget.dispose();
          m_control.setData(DND_DROP_TARGET, null);
        }
      }
    }
  }

  protected IRwtEnvironment getUiEnvironment() {
    return m_uiEnvironment;
  }

  protected abstract void handleUiDropAction(DropTargetEvent event, TransferObject scoutTransferObject);

  protected void handleUiDropTargetChanged(DropTargetEvent event) {
  }

  protected abstract TransferObject handleUiDragRequest();

  protected void handleUiDragFinished() {
  }

  protected void updateDragSupportFromScout() {
    if (m_scoutObject == null || m_control == null || m_control.isDisposed()) {
      return;
    }
    int scoutType = m_scoutDndSupportable.getDragType();
    Transfer[] transferTypes = RwtUtility.convertScoutTransferTypes(scoutType);
    DragSource dragSource = (DragSource) m_control.getData(DND_DRAG_SOURCE);
    if (dragSource == null) {
      if (transferTypes.length > 0) {
        // create new
        dragSource = createDragSource(m_control);
      }
    }
    if (dragSource != null) {
      // remove old
      ArrayList<Transfer> types = new ArrayList<Transfer>(Arrays.asList(dragSource.getTransfer()));
      if (m_dragTransferTypes != null) {
        for (Transfer t : m_dragTransferTypes) {
          types.remove(t);
        }
        m_dragTransferTypes = null;
      }
      // add new transfer types
      m_dragTransferTypes = transferTypes;
      for (Transfer t : m_dragTransferTypes) {
        types.add(t);
      }
      if (types.size() > 0) {
        dragSource.setTransfer(types.toArray(new Transfer[types.size()]));
        if (m_dragSourceListener == null) {
          m_dragSourceListener = new P_RwtDragSourceListener();
          dragSource.addDragListener(m_dragSourceListener);
        }
      }
      else {
        if (m_dragSourceListener != null) {
          dragSource.removeDragListener(m_dragSourceListener);
          m_dragSourceListener = null;
        }
        dragSource.dispose();
      }
    }
  }

  protected DragSource createDragSource(Control control) {
    return new DragSource(control, DND.DROP_COPY);
  }

  protected void updateDropSupportFromScout() {
    if (m_scoutObject == null || m_control == null || m_control.isDisposed()) {
      return;
    }
    int scoutType = m_scoutDndSupportable.getDropType();
    Transfer[] transferTypes = RwtUtility.convertScoutTransferTypes(scoutType);
    DropTarget dropTarget = (DropTarget) m_control.getData(DND_DROP_TARGET);
    if (dropTarget == null) {
      if (transferTypes.length > 0) {
        // create new
        dropTarget = createDropTarget(m_control);
      }
    }
    if (dropTarget != null) {
      // remove old
      ArrayList<Transfer> types = new ArrayList<Transfer>(Arrays.asList(dropTarget.getTransfer()));
      if (m_dropTransferTypes != null) {
        for (Transfer t : m_dropTransferTypes) {
          types.remove(t);
        }
        m_dropTransferTypes = null;
      }
      // add new transfer types
      m_dropTransferTypes = transferTypes;
      for (Transfer t : m_dropTransferTypes) {
        types.add(t);
      }
      if (types.size() > 0) {
        m_isClientFileTransferSupported = types.contains(ClientFileTransfer.getInstance());
        dropTarget.setTransfer(types.toArray(new Transfer[types.size()]));
        if (m_dropTargetListener == null) {
          m_dropTargetListener = new P_RwtDropTargetListener();
          dropTarget.addDropListener(m_dropTargetListener);
        }
      }
      else {
        m_isClientFileTransferSupported = false;
        if (m_dropTargetListener != null) {
          dropTarget.removeDropListener(m_dropTargetListener);
          m_dropTargetListener = null;
        }
        dropTarget.dispose();
        m_control.setData(DND_DROP_TARGET, null);
      }
    }
  }

  protected DropTarget createDropTarget(Control control) {
    return new DropTarget(control, DND.DROP_MOVE | DND.DROP_COPY);
  }

  protected void handleScoutProperty(String name, Object newValue) {
    if (IDNDSupport.PROP_DRAG_TYPE.equals(name)) {
      updateDragSupportFromScout();
    }
    else if (IDNDSupport.PROP_DROP_TYPE.equals(name)) {
      updateDropSupportFromScout();
    }
  }

  protected boolean handleFileUpload(DropTargetEvent event) {
    if (!m_isClientFileTransferSupported || !ClientFileTransfer.getInstance().isSupportedType(event.currentDataType)) {
      return false;
    }

    if (m_uploadHandler == null) {
      m_uploadHandler = RwtScoutFileUploadHandlerFactory.createFileUploadHandler(this);
    }

    return m_uploadHandler.startFileUpload(event);
  }

  protected static int getPercentage(RwtScoutFileUploadEvent uploadEvent) {
    double bytesRead = uploadEvent.getBytesRead();
    double contentLength = uploadEvent.getContentLength();
    double fraction = bytesRead / contentLength;
    return (int) Math.floor(fraction * 100);
  }

  @Override
  public void uploadProgress(final DropTargetEvent dropEvent, final RwtScoutFileUploadEvent uploadEvent) {
  }

  @Override
  public void uploadFailed(DropTargetEvent dropEvent, RwtScoutFileUploadEvent uploadEvent) {
  }

  @Override
  public void uploadFinished(final DropTargetEvent dropEvent, final RwtScoutFileUploadEvent uploadEvent, final List<File> uploadedFiles) {
    getUiEnvironment().getDisplay().syncExec(new Runnable() {
      @Override
      public void run() {
        TransferObject scoutTransferable = createScoutTransferableObjectFromFileUpload(dropEvent, uploadedFiles);
        if (scoutTransferable != null) {
          handleUiDropAction(dropEvent, scoutTransferable);
        }
      }
    });
  }

  protected TransferObject createScoutTransferableObjectFromFileUpload(DropTargetEvent event, List<File> uploadedFiles) {
    return RwtUtility.createScoutTransferableFromClientFile(event, uploadedFiles);
  }

  protected TransferObject createScoutTransferableObject(DropTargetEvent event) {
    return RwtUtility.createScoutTransferable(event);
  }

  private class P_RwtDropTargetListener extends DropTargetAdapter {
    private static final long serialVersionUID = 1L;

    @Override
    public void drop(DropTargetEvent event) {
      if (handleFileUpload(event)) {
        return;
      }
      TransferObject scoutTransferable = createScoutTransferableObject(event);
      if (scoutTransferable != null) {
        handleUiDropAction(event, scoutTransferable);
      }
    }

    @Override
    public void dragOver(DropTargetEvent event) {
      handleUiDropTargetChanged(event);
    }
  } // end class P_RwtDropTargetListener

  private class P_RwtDragSourceListener extends DragSourceAdapter {
    private static final long serialVersionUID = 1L;
    private Object m_data;

    @Override
    public void dragStart(DragSourceEvent event) {
      super.dragStart(event);
      TransferObject scoutTransfer = handleUiDragRequest();
      if (scoutTransfer != null) {
        m_data = RwtUtility.createUiTransferable(scoutTransfer);
      }
    }

    @Override
    public void dragSetData(DragSourceEvent event) {
      if (m_data != null) {
        event.data = m_data;
      }
    }

    @Override
    public void dragFinished(DragSourceEvent event) {
      super.dragFinished(event);
      handleUiDragFinished();
      m_data = null;
    }
  } // end class P_RwtDragSourceListener

  private class P_ScoutObjectPropertyListener implements PropertyChangeListener {
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
      if (getUiEnvironment().getDisplay() != null && !getUiEnvironment().getDisplay().isDisposed()) {
        Runnable job = new Runnable() {
          @Override
          public void run() {
            handleScoutProperty(evt.getPropertyName(), evt.getNewValue());
          }
        };
        getUiEnvironment().invokeUiLater(job);
      }
    }
  }
}
