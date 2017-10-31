/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_POLICY_LOGS_TO_APP_DEPLOYMENT_PROPERTY;

import org.mule.functional.logging.TestAppender;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.size.SmallTest;

import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.MessageFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MuleLoggerContextTestCase extends AbstractMuleTestCase {

  private static final String DEFAULT_CONTEXT_NAME = "Default";
  private static final String MESSAGE = "Do you wanna build a snowman?";
  private static final String CATEGORY = MuleLoggerContextTestCase.class.getName();
  private static final String TEST_APPENDER = "testAppender";
  private static final String APP_NAME = "appName";
  private static final String POLICY_NAME = "policyName";
  private static final Level LEVEL = Level.ERROR;

  @Mock
  private ContextSelector contextSelector;

  @Mock
  private MessageFactory messageFactory;

  @Mock
  private ArtifactDescriptor appDescriptor;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ArtifactDescriptor policyDescriptor;

  @Mock
  private RegionClassLoader appClassLoader;

  @Mock
  private ClassLoaderLookupPolicy lookupPolicy;

  private MuleLoggerContext context;
  private TestAppender testAppender;

  @Before
  public void before() {
    context = getDefaultContext();
    testAppender = new TestAppender(TEST_APPENDER, null, null);
    context.getConfiguration().addAppender(testAppender);

    LoggerConfig loggerConfig =
        AsyncLoggerConfig.createLogger("false", LEVEL.name(), CATEGORY, "true",
                                       new AppenderRef[] {AppenderRef.createAppenderRef(TEST_APPENDER, null, null)}, null,
                                       context.getConfiguration(), null);

    loggerConfig.addAppender(testAppender, null, null);
    context.getConfiguration().addLogger(CATEGORY, loggerConfig);
    context.getConfiguration().start();
    context.updateLoggers();

    when(appDescriptor.getName()).thenReturn(APP_NAME);
    when(appClassLoader.getArtifactDescriptor()).thenReturn(appDescriptor);
    when(policyDescriptor.getName()).thenReturn(POLICY_NAME);
    when(policyDescriptor.getBundleDescriptor().isPolicy()).thenReturn(true);
  }

  @Test
  public void dispatchingLogger() {
    assertThat(context.newInstance(context, "", messageFactory), instanceOf(DispatchingLogger.class));
  }

  @Test
  public void reconfigureAsyncLoggers() {
    Logger logger = context.getLogger(CATEGORY);
    logger.error(MESSAGE);

    assertLogged();
    testAppender.clear();

    context.updateLoggers(context.getConfiguration());
    logger.error(MESSAGE);
    assertLogged();
  }

  @Test
  public void policyLoggerContextWithEmptyProperties() {
    RegionClassLoader policyClassLoader = new RegionClassLoader(POLICY_NAME, policyDescriptor, appClassLoader, lookupPolicy);
    when(policyDescriptor.getDeploymentProperties()).thenReturn(empty());

    context = new MuleLoggerContext(DEFAULT_CONTEXT_NAME, null, policyClassLoader, contextSelector, true);

    assertThat(context.getArtifactName(), is(POLICY_NAME));
    assertThat(context.getArtifactDescriptor(), is(policyDescriptor));
    assertThat(context.isApplicationClassloader(), is(true));
    assertThat(context.isArtifactClassloader(), is(true));
  }

  @Test
  public void policyLoggerContextWithPropertyEnabled() {
    RegionClassLoader policyClassLoader = new RegionClassLoader(POLICY_NAME, policyDescriptor, appClassLoader, lookupPolicy);
    when(policyDescriptor.getDeploymentProperties()).thenReturn(of(getPolicyLogsToAppDeploymentProperty("true")));

    context = new MuleLoggerContext(DEFAULT_CONTEXT_NAME, null, policyClassLoader, contextSelector, true);

    assertThat(context.getArtifactName(), is(APP_NAME));
    assertThat(context.getArtifactDescriptor(), is(appDescriptor));
    assertThat(context.isApplicationClassloader(), is(true));
    assertThat(context.isArtifactClassloader(), is(true));
  }

  @Test
  public void policyLoggerContextWithPropertyDisabled() {
    RegionClassLoader policyClassLoader = new RegionClassLoader(POLICY_NAME, policyDescriptor, appClassLoader, lookupPolicy);
    when(policyDescriptor.getDeploymentProperties()).thenReturn(of(getPolicyLogsToAppDeploymentProperty("false")));

    context = new MuleLoggerContext(DEFAULT_CONTEXT_NAME, null, policyClassLoader, contextSelector, true);

    assertThat(context.getArtifactName(), is(POLICY_NAME));
    assertThat(context.getArtifactDescriptor(), is(policyDescriptor));
    assertThat(context.isApplicationClassloader(), is(true));
    assertThat(context.isArtifactClassloader(), is(true));
  }

  private void assertLogged() {
    PollingProber pollingProber = new PollingProber(5000, 500);
    pollingProber.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        testAppender.ensure(new TestAppender.Expectation(LEVEL.name(), CATEGORY, MESSAGE));
        return true;
      }

      @Override
      public String describeFailure() {
        return "message was not logged";
      }
    });

  }

  private MuleLoggerContext getDefaultContext() {
    return new MuleLoggerContext(DEFAULT_CONTEXT_NAME, null, Thread.currentThread().getContextClassLoader(), contextSelector,
                                 true);
  }

  private Properties getPolicyLogsToAppDeploymentProperty(String value) {
    Properties properties = new Properties();
    properties.put(MULE_POLICY_LOGS_TO_APP_DEPLOYMENT_PROPERTY, value);
    return properties;
  }
}
