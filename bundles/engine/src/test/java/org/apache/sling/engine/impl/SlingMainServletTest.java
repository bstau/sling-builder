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
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.engine.impl.filter.ServletFilterManager;
import org.apache.sling.engine.impl.helper.RequestListenerManager;
import org.apache.sling.engine.impl.helper.SlingServletContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockServletContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class SlingMainServletTest {
    private SlingMainServlet underTest;

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_JACKRABBIT);

    @Before
    public void setup(){
        underTest = new SlingMainServlet();
    }

    @Test
    public void testService() throws ServletException, NoSuchFieldException, IOException {
        final SlingServletContext servletContext = new SlingServletContext(context.bundleContext(), underTest);
        final RequestListenerManager r = new RequestListenerManager(context.bundleContext(), servletContext);
        PrivateAccessor.setField(underTest, "requestListenerManager", r);


        ServletFilterManager filterManager = new ServletFilterManager(context.bundleContext(), servletContext, true);
        SlingRequestProcessor requestProcessor = (SlingRequestProcessor) PrivateAccessor.getField(underTest, "requestProcessor");
        PrivateAccessor.setField(requestProcessor, "filterManager", filterManager);

        ServletResolver resolver = mock(ServletResolver.class);

        underTest.setServletResolver(resolver);

        final MockSlingHttpServletRequest req = spy(context.request());
        req.setAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER, context.resourceResolver());

        final MockSlingHttpServletResponse res = spy(context.response());

        doReturn("127.0.0.1").when(req).getRemoteAddr();
        doReturn("HTTP/1.1").when(req).getProtocol();
        doReturn("/").when(req).getRequestURI();
        doReturn("/").when(req).getServletPath();
        doReturn(null).when(req).getPathInfo();

        underTest.service(req, res);
    }
}
