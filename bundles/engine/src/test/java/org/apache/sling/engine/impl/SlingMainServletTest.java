/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.engine.impl;

import junitx.util.PrivateAccessor;
import org.apache.sling.api.request.SlingRequestEvent;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.engine.impl.helper.RequestListenerManager;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SlingMainServletTest {
    private SlingMainServlet underTest;
    private ResourceResolver resourceResolver;

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_JACKRABBIT);

    @Before
    public void setup() throws LoginException {
        underTest = new SlingMainServlet();
        resourceResolver = context.resourceResolver().clone(null);
    }

    @Test
    public void testServiceWithGetRequest() throws ServletException, NoSuchFieldException, IOException {
        final RequestListenerManager requestListenerManager = mock(RequestListenerManager.class);
        PrivateAccessor.setField(underTest, "requestListenerManager", requestListenerManager);

        final SlingRequestProcessorImpl requestProcessor = mock(SlingRequestProcessorImpl.class);
        PrivateAccessor.setField(underTest, "requestProcessor", requestProcessor);

        final MockSlingHttpServletRequest req = spy(context.request());
        req.setMethod("GET");
        req.setAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER, resourceResolver);

        doReturn("127.0.0.1").when(req).getRemoteAddr();
        doReturn("HTTP/1.1").when(req).getProtocol();
        doReturn("/").when(req).getRequestURI();

        final MockSlingHttpServletResponse res = spy(context.response());

        assertNotEquals(405, res.getStatus());
        assertNotEquals("GET, HEAD, POST, PUT, DELETE, OPTIONS", res.getHeader("Allow"));
        underTest.service(req, res);
        //Could be set to these values only if request's method is TRACE, not GET
        assertNotEquals(405, res.getStatus());
        assertNotEquals("GET, HEAD, POST, PUT, DELETE, OPTIONS", res.getHeader("Allow"));

        //Checking that all required methods where called with correct parameters.
        verify(requestProcessor, times(1)).doProcessRequest(req, res, resourceResolver);
        verify(requestListenerManager, times(1)).sendEvent(req, SlingRequestEvent.EventType.EVENT_INIT);
        verify(requestListenerManager, times(1)).sendEvent(req, SlingRequestEvent.EventType.EVENT_DESTROY);
    }

    @Test
    public void testServiceWithTraceRequest() throws ServletException, NoSuchFieldException, IOException {
        final RequestListenerManager requestListenerManager = mock(RequestListenerManager.class);
        PrivateAccessor.setField(underTest, "requestListenerManager", requestListenerManager);

        final SlingRequestProcessorImpl requestProcessor = mock(SlingRequestProcessorImpl.class);
        PrivateAccessor.setField(underTest, "requestProcessor", requestProcessor);

        final MockSlingHttpServletRequest req = spy(context.request());
        req.setMethod("TRACE");
        req.setAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER, resourceResolver);

        doReturn("127.0.0.1").when(req).getRemoteAddr();
        doReturn("HTTP/1.1").when(req).getProtocol();
        doReturn("/").when(req).getRequestURI();

        final MockSlingHttpServletResponse res = spy(context.response());

        assertNotEquals(405, res.getStatus());
        assertNotEquals("GET, HEAD, POST, PUT, DELETE, OPTIONS", res.getHeader("Allow"));
        underTest.service(req, res);
        //Should be set to these values because of TRACE method
        assertEquals(405, res.getStatus());
        assertEquals("GET, HEAD, POST, PUT, DELETE, OPTIONS", res.getHeader("Allow"));

        //Checking that requestProcessor was not called because of request's method type.
        verify(requestProcessor, times(0)).doProcessRequest(req, res, resourceResolver);
        verify(requestListenerManager, times(1)).sendEvent(req, SlingRequestEvent.EventType.EVENT_INIT);
        verify(requestListenerManager, times(1)).sendEvent(req, SlingRequestEvent.EventType.EVENT_DESTROY);
    }

    @Test
    public void testServiceWithoutResourceResolver() throws ServletException, NoSuchFieldException, IOException {
        final RequestListenerManager requestListenerManager = mock(RequestListenerManager.class);
        PrivateAccessor.setField(underTest, "requestListenerManager", requestListenerManager);

        final SlingRequestProcessorImpl requestProcessor = mock(SlingRequestProcessorImpl.class);
        PrivateAccessor.setField(underTest, "requestProcessor", requestProcessor);

        final MockSlingHttpServletRequest req = spy(context.request());
        req.setMethod("GET");
        doReturn("127.0.0.1").when(req).getRemoteAddr();
        doReturn("HTTP/1.1").when(req).getProtocol();
        doReturn("/").when(req).getRequestURI();

        final MockSlingHttpServletResponse res = spy(context.response());

        assertNotEquals(405, res.getStatus());
        assertNotEquals("GET, HEAD, POST, PUT, DELETE, OPTIONS", res.getHeader("Allow"));
        underTest.service(req, res);
        //There we checking that response object was not changed inside this method.
        //Below are the only values which could be set inside SlingMainServlet#service(...) method
        assertNotEquals(405, res.getStatus());
        assertNotEquals("GET, HEAD, POST, PUT, DELETE, OPTIONS", res.getHeader("Allow"));

        //Checking that all required methods where called with correct parameters.
        verify(requestProcessor, times(1)).doProcessRequest(req, res, null);
        verify(requestListenerManager, times(1)).sendEvent(req, SlingRequestEvent.EventType.EVENT_INIT);
        verify(requestListenerManager, times(1)).sendEvent(req, SlingRequestEvent.EventType.EVENT_DESTROY);
    }
}
