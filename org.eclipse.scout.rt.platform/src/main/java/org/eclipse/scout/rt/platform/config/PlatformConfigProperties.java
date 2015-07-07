/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.platform.config;

/**
 *
 */
public final class PlatformConfigProperties {

  private PlatformConfigProperties() {
  }

  /**
   * Property to specify if the application is running in development mode. Default is <code>false</code>.
   */
  public static class PlatformDevModeProperty extends AbstractBooleanConfigProperty {

    @Override
    public String getKey() {
      return "scout.dev.mode";
    }

    @Override
    protected Boolean getDefaultValue() {
      return Boolean.FALSE;
    }
  }

  public static class ApplicationVersionProperty extends AbstractStringConfigProperty {

    @Override
    public String getKey() {
      return "scout.application.version";
    }

    @Override
    protected String getDefaultValue() {
      return "0.0.0";
    }
  }

  public static class ApplicationNameProperty extends AbstractStringConfigProperty {

    @Override
    public String getKey() {
      return "scout.application.name";
    }

    @Override
    protected String getDefaultValue() {
      return "unknown";
    }
  }

  /**
   * Specifies if jandex indexes should be rebuilt.
   */
  public static class JandexRebuildProperty extends AbstractBooleanConfigProperty {

    @Override
    public String getKey() {
      return "jandex.rebuild";
    }

    @Override
    protected Boolean getDefaultValue() {
      return Boolean.FALSE;
    }
  }

  /**
   * The number of threads to keep in the pool, even if they are idle.
   */
  public static class JobManagerCorePoolSizeProperty extends AbstractPositiveIntegerConfigProperty {

    @Override
    public String getKey() {
      return "scout.jobmanager.corePoolSize";
    }

    @Override
    protected Integer getDefaultValue() {
      return Integer.valueOf(10);
    }
  }

  /**
   * The maximal number of threads to be created once the core-pool-size is exceeded.
   */
  public static class JobManagerMaximumPoolSizeProperty extends AbstractPositiveIntegerConfigProperty {

    @Override
    public String getKey() {
      return "scout.jobmanager.maximumPoolSize";
    }

    @Override
    protected Integer getDefaultValue() {
      return Integer.MAX_VALUE;
    }
  }

  /**
   * The time limit (in seconds) for which threads, which are created upon exceeding the 'core-pool-size' limit, may
   * remain idle before being terminated.
   */
  public static class JobManagerKeepAliveTimeProperty extends AbstractPositiveIntegerConfigProperty {

    @Override
    public String getKey() {
      return "scout.jobmanager.keepAliveTime";
    }

    @Override
    protected Integer getDefaultValue() {
      return Integer.valueOf(60);
    }
  }

  /**
   * Specifies whether threads of the core-pool should be terminated after being idle for longer than 'keepAliveTime'.
   */
  public static class JobManagerAllowCoreThreadTimeoutProperty extends AbstractBooleanConfigProperty {

    @Override
    public String getKey() {
      return "scout.jobmanager.allowCoreThreadTimeOut";
    }

    @Override
    protected Boolean getDefaultValue() {
      return Boolean.FALSE;
    }
  }

  /**
   * The number of dispatcher threads to be used to dispatch delayed jobs, meaning jobs scheduled with a delay or
   * periodic jobs.
   */
  public static class JobManagerDispatcherThreadCountProperty extends AbstractPositiveIntegerConfigProperty {

    @Override
    public String getKey() {
      return "scout.jobmanager.dispatcherThreadCount";
    }

    @Override
    protected Integer getDefaultValue() {
      return Integer.valueOf(1);
    }
  }

}
