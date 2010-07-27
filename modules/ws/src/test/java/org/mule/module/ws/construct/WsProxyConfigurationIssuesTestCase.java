/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.construct;

import java.net.URL;
import java.util.concurrent.Callable;

import org.mule.MessageExchangePattern;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;

public class WsProxyConfigurationIssuesTestCase extends AbstractMuleTestCase
{
    public void testNullMessageSource()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy(muleContext, "testNullMessageSource", null,
                    MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE,
                        muleContext));
            }
        });
    }

    public void testNullOutboundEndpoint()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy(muleContext, "testNullOutboundEndpoint",
                    getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE), null);
            }
        });
    }

    public void testNullOutboundEndpointWithWsdl()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy(muleContext, "testNullOutboundEndpointWithWsdl",
                    getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE), null, "fake_wsdl");
            }
        });
    }

    public void testBlankWsdlContents()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy(muleContext, "testBlankWsdlContents",
                    getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE),
                    MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE,
                        muleContext), "");
            }
        });
    }

    public void testNullWsdlUrl()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy(muleContext, "testNullWsdlUrl",
                    getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE),
                    MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE,
                        muleContext), (URL) null);
            }
        });
    }

    public void testOneWayInboundEndpoint()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy(muleContext, "testOneWayInboundEndpoint",
                    getTestInboundEndpoint(MessageExchangePattern.ONE_WAY),
                    MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE,
                        muleContext));
            }
        });
    }

    public void testOneWayOutboundEndpoint()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy(muleContext, "testOneWayOutboundEndpoint",
                    getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE),
                    MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.ONE_WAY, muleContext));
            }
        });
    }

    private void runTestFailingWithExpectedFlowConstructInvalidException(Callable<WSProxy> failingStatement)
    {
        try
        {
            failingStatement.call().validateConstruct();
            fail("should have got a FlowConstructInvalidException");
        }
        catch (final Exception e)
        {
            assertTrue(e instanceof FlowConstructInvalidException);
        }
    }
}
