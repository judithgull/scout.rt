package org.eclipse.scout.rt.client.extension.ui.form.fields.pagefield;

import java.util.List;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.extension.ui.form.fields.IFormFieldExtension;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractFormField;
import org.eclipse.scout.rt.client.ui.form.fields.pagefield.AbstractPageField;
import org.eclipse.scout.rt.shared.extension.AbstractExtensionChain;

public final class PageFieldChains {

  private PageFieldChains() {
  }

  protected abstract static class AbstractPageFieldChain<PAGE extends IPage> extends AbstractExtensionChain<IPageFieldExtension<PAGE, ? extends AbstractPageField<PAGE>>> {

    public AbstractPageFieldChain(List<? extends IFormFieldExtension<? extends AbstractFormField>> extensions) {
      super(extensions, IPageFieldExtension.class);
    }
  }

  public static class PageFieldPageChangedChain<PAGE extends IPage> extends AbstractPageFieldChain<PAGE> {

    public PageFieldPageChangedChain(List<? extends IFormFieldExtension<? extends AbstractFormField>> extensions) {
      super(extensions);
    }

    public void execPageChanged(final PAGE oldPage, final PAGE newPage) throws ProcessingException {
      MethodInvocation<Void> methodInvocation = new MethodInvocation<Void>() {
        @Override
        protected void callMethod(IPageFieldExtension<PAGE, ? extends AbstractPageField<PAGE>> next) throws ProcessingException {
          next.execPageChanged(PageFieldPageChangedChain.this, oldPage, newPage);
        }
      };
      callChain(methodInvocation, oldPage, newPage);
      if (methodInvocation.getException() instanceof ProcessingException) {
        throw (ProcessingException) methodInvocation.getException();
      }
    }
  }
}