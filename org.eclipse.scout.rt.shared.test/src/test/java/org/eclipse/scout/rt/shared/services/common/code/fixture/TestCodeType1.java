package org.eclipse.scout.rt.shared.services.common.code.fixture;

import org.eclipse.scout.rt.shared.services.common.code.AbstractCodeType;

public class TestCodeType1 extends AbstractCodeType<Long, String> {
  private static final long serialVersionUID = 1L;

  public static final Long ID = Long.valueOf(42);

  @Override
  public Long getId() {
    return ID;
  }
}