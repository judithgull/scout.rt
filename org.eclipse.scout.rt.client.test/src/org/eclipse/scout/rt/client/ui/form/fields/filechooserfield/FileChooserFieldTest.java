package org.eclipse.scout.rt.client.ui.form.fields.filechooserfield;

import java.util.List;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.action.menu.AbstractMenu;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.form.AbstractForm;
import org.eclipse.scout.rt.client.ui.form.fields.filechooserfield.FileChooserFieldTest.TestForm.MainBox.FileChooserField;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.testing.client.form.FormHandler;
import org.eclipse.scout.testing.client.runner.ScoutClientTestRunner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link AbstractFileChooserField}
 * 
 * @since 3.10.0-M4
 */
@RunWith(ScoutClientTestRunner.class)
public class FileChooserFieldTest {

  public static class TestForm extends AbstractForm {

    public TestForm() throws ProcessingException {
      super();
    }

    @Override
    protected String getConfiguredTitle() {
      return "Test Form";
    }

    public void startForm() throws ProcessingException {
      startInternal(new FormHandler());
    }

    public MainBox getMainBox() {
      return getFieldByClass(MainBox.class);
    }

    public AbstractFileChooserField getFileChooserField() {
      return getFieldByClass(FileChooserField.class);
    }

    @Order(10)
    public class MainBox extends AbstractGroupBox {
      @Order(10.0)
      public class FileChooserField extends AbstractFileChooserField {

        @Order(10.0)
        public class Menu1 extends AbstractMenu {

          @Override
          protected String getConfiguredKeyStroke() {
            return "alt-1";
          }

          @Override
          protected String getConfiguredText() {
            return "&Menu1";
          }

        }

        @Order(20.0)
        public class Menu2 extends AbstractMenu {

          @Override
          protected String getConfiguredKeyStroke() {
            return "alt-2";
          }

          @Override
          protected String getConfiguredText() {
            return "Menu&2";
          }
        }

        @Override
        protected List<String> getConfiguredFileExtensions() {
          return CollectionUtility.arrayList("png", "bmp", "jpg", "jpeg", "gif");
        }

        @Override
        protected boolean getConfiguredAutoAddDefaultMenus() {
          return false;
        }

        @Override
        protected String getConfiguredLabel() {
          return "&choose an image";
        }

        @Override
        protected boolean getConfiguredTypeLoad() {
          return true;
        }
      }
    }
  }

  private TestForm m_form;

  @Before
  public void setUp() throws Throwable {
    m_form = new TestForm();
    m_form.startForm();
  }

  @Test
  public void testLabel() {
    Assert.assertEquals("&choose an image", m_form.getFileChooserField().getLabel());
  }

  @Test
  public void testFileExtensions() {
    List<String> extensions = m_form.getFileChooserField().getFileExtensions();
    Assert.assertEquals("There should be 5 file extensions registered", 5, extensions.size());
    Assert.assertTrue(extensions.contains("png"));
    Assert.assertTrue(extensions.contains("bmp"));
    Assert.assertTrue(extensions.contains("jpg"));
    Assert.assertTrue(extensions.contains("jpeg"));
    Assert.assertTrue(extensions.contains("gif"));
  }

  @Test
  public void testMenusAndKeyStrokes() {
    List<IMenu> fileChooserFieldMenus = m_form.getFileChooserField().getMenus();
    Assert.assertEquals("fileChooserField should have 2 menus", 2, fileChooserFieldMenus.size());

    Assert.assertEquals("Menu1", fileChooserFieldMenus.get(0).getText());
    Assert.assertEquals("&Menu1", fileChooserFieldMenus.get(0).getTextWithMnemonic());
    Assert.assertEquals("alternate-1", fileChooserFieldMenus.get(0).getKeyStroke());

    Assert.assertEquals("Menu2", fileChooserFieldMenus.get(1).getText());
    Assert.assertEquals("Menu&2", fileChooserFieldMenus.get(1).getTextWithMnemonic());
    Assert.assertEquals("alternate-2", fileChooserFieldMenus.get(1).getKeyStroke());

    List<IKeyStroke> fileChooserFieldKeyStrokes = m_form.getFileChooserField().getContributedKeyStrokes();
    Assert.assertNotNull("KeyStrokes of fileChooserField should not be null", fileChooserFieldKeyStrokes);
    Assert.assertEquals("fileChooserField should have 2 keyStrokes registered", 2, fileChooserFieldKeyStrokes.size());
  }

  @After
  public void tearDown() throws Throwable {
    m_form.doClose();
  }
}
