package org.eclipse.scout.rt.client.extension.ui.basic.table.columns;

import java.util.List;

import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractMixedSmartColumn;
import org.eclipse.scout.rt.shared.extension.AbstractExtensionChain;

public final class MixedSmartColumnChains {

  private MixedSmartColumnChains() {
  }

  protected abstract static class AbstractMixedSmartColumnChain<VALUE_TYPE, LOOKUP_CALL_KEY_TYPE> extends AbstractExtensionChain<IMixedSmartColumnExtension<VALUE_TYPE, LOOKUP_CALL_KEY_TYPE, ? extends AbstractMixedSmartColumn<VALUE_TYPE, LOOKUP_CALL_KEY_TYPE>>> {

    public AbstractMixedSmartColumnChain(List<? extends IColumnExtension<VALUE_TYPE, ? extends AbstractColumn<VALUE_TYPE>>> extensions) {
      super(extensions, IMixedSmartColumnExtension.class);
    }
  }

  public static class MixedSmartColumnConvertKeyToValueChain<VALUE_TYPE, LOOKUP_CALL_KEY_TYPE> extends AbstractMixedSmartColumnChain<VALUE_TYPE, LOOKUP_CALL_KEY_TYPE> {

    public MixedSmartColumnConvertKeyToValueChain(List<? extends IColumnExtension<VALUE_TYPE, ? extends AbstractColumn<VALUE_TYPE>>> extensions) {
      super(extensions);
    }

    public VALUE_TYPE execConvertKeyToValue(final LOOKUP_CALL_KEY_TYPE key) {
      MethodInvocation<VALUE_TYPE> methodInvocation = new MethodInvocation<VALUE_TYPE>() {
        @Override
        protected void callMethod(IMixedSmartColumnExtension<VALUE_TYPE, LOOKUP_CALL_KEY_TYPE, ? extends AbstractMixedSmartColumn<VALUE_TYPE, LOOKUP_CALL_KEY_TYPE>> next) {
          setReturnValue(next.execConvertKeyToValue(MixedSmartColumnConvertKeyToValueChain.this, key));
        }
      };
      callChain(methodInvocation, key);
      return methodInvocation.getReturnValue();
    }
  }
}
