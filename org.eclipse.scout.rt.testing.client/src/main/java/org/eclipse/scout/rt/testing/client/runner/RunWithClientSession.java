package org.eclipse.scout.rt.testing.client.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.scout.rt.client.session.ClientSessionProvider;
import org.eclipse.scout.rt.client.session.ClientSessionProviderWithCache;
import org.eclipse.scout.rt.shared.ISession;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;

/**
 * Annotation to execute test-methods under a particular client session. This is used in conjunction with
 * {@link PlatformTestRunner} and its subclasses.
 *
 * @since 5.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface RunWithClientSession {

  /**
   * @return session class to be used.
   */
  Class<? extends ISession> value();

  /**
   * @return provider to create and start the session; by default, {@link ClientSessionProviderWithCache} is used.
   */
  Class<? extends ClientSessionProvider> provider() default ClientSessionProviderWithCache.class;
}