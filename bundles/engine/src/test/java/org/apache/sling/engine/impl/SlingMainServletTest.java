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
import org.apache.sling.engine.impl.request.RequestData;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import javax.servlet.ServletException;

import java.io.IOException;
import java.util.Dictionary;
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
        //We need to clone ResourceResolver because every SlingMainServlet#service(...) call closes it.
        resourceResolver = context.resourceResolver().clone(null);
    }



    //-----SlingMainServlet#service(ServletRequest req, ServletResponse res)----//

    private RequestListenerManager requestListenerManager;
    private SlingRequestProcessorImpl requestProcessor;

    @Test
    public void testServiceWithGetRequest() throws ServletException, NoSuchFieldException, IOException {
        prepareToTestServiceMethod(underTest);

        final MockSlingHttpServletRequest req = createPreconfiguredRequest();
        req.setMethod("GET"); //Method should not be of type TRACE
        req.setAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER, resourceResolver);

        final MockSlingHttpServletResponse res = spy(context.response());

        underTest.service(req, res);

        //Checking that all required methods where called with correct parameters.
        verify(requestProcessor, times(1)).doProcessRequest(req, res, resourceResolver);
        verify(requestListenerManager, times(1)).sendEvent(req, SlingRequestEvent.EventType.EVENT_INIT);
        verify(requestListenerManager, times(1)).sendEvent(req, SlingRequestEvent.EventType.EVENT_DESTROY);
    }

    @Test
    public void testServiceWithTraceRequest() throws ServletException, NoSuchFieldException, IOException {
        prepareToTestServiceMethod(underTest);
        //If TRACE doesn't allowed request will not be processed.
        //TRACE doesn't available by default, but let's set it explicit
        PrivateAccessor.setField(underTest, "allowTrace", false);

        final MockSlingHttpServletRequest req = createPreconfiguredRequest();
        req.setMethod("TRACE"); //TRACE doesn't allowed for processing

        final MockSlingHttpServletResponse res = spy(context.response());

        underTest.service(req, res);
        //Should be set to these values because of TRACE method type
        assertEquals(405, res.getStatus());
        assertTrue("Allow Header should be added after service call", res.containsHeader("Allow"));

        //Checking that requestProcessor was not called because of request's method type.
        verify(requestProcessor, times(0)).doProcessRequest(req, res, null);
        verify(requestListenerManager, times(1)).sendEvent(req, SlingRequestEvent.EventType.EVENT_INIT);
        verify(requestListenerManager, times(1)).sendEvent(req, SlingRequestEvent.EventType.EVENT_DESTROY);
    }

    @Test
    public void testServiceWithoutResourceResolver() throws ServletException, NoSuchFieldException, IOException {
        prepareToTestServiceMethod(underTest);

        final MockSlingHttpServletRequest req = createPreconfiguredRequest();
        req.setMethod("GET");

        final MockSlingHttpServletResponse res = spy(context.response());

        underTest.service(req, res);

        //Checking that all required methods where called with correct parameters.
        verify(requestProcessor, times(1)).doProcessRequest(req, res, null);
        verify(requestListenerManager, times(1)).sendEvent(req, SlingRequestEvent.EventType.EVENT_INIT);
        verify(requestListenerManager, times(1)).sendEvent(req, SlingRequestEvent.EventType.EVENT_DESTROY);
    }

    @Test
    public void testServletActivation() throws NoSuchFieldException, ServletException, NamespaceException {
        final SlingHttpContext slingHttpContext = (SlingHttpContext) PrivateAccessor.getField(underTest, "slingHttpContext");

        Map<String, Object> componentConfig = new HashMap<String, Object>();
        componentConfig.put("sling.additional.response.headers", new String[]{"foo=bar"});
        componentConfig.put(SlingMainServlet.PROP_ALLOW_TRACE, "true");
        componentConfig.put(SlingMainServlet.PROP_MAX_INCLUSION_COUNTER, 40);
        componentConfig.put(SlingMainServlet.PROP_MAX_CALL_COUNTER, 800);

        HttpService httpService = mock(HttpService.class);
        PrivateAccessor.setField(underTest, "httpService", httpService);

        underTest.activate(context.bundleContext(), componentConfig);
        verify(httpService, times(1)).registerServlet("/", underTest, any(Dictionary.class), slingHttpContext);
    }

    //Mocks which are required to call SlingMainServlet#service(...) method
    private void prepareToTestServiceMethod(SlingMainServlet mainServlet) throws NoSuchFieldException {
        requestListenerManager = mock(RequestListenerManager.class);
        PrivateAccessor.setField(mainServlet, "requestListenerManager", requestListenerManager);

        requestProcessor = mock(SlingRequestProcessorImpl.class);
        PrivateAccessor.setField(mainServlet, "requestProcessor", requestProcessor);
    }

    private MockSlingHttpServletRequest createPreconfiguredRequest(){
        final MockSlingHttpServletRequest req = spy(context.request());
        doReturn("127.0.0.1").when(req).getRemoteAddr();
        doReturn("HTTP/1.1").when(req).getProtocol();
        doReturn("/").when(req).getRequestURI();

        return req;
    }
}
