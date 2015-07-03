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

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.impl.request.RequestData;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class SlingHttpServletRequestImplTest {

    private SlingHttpServletRequestImpl underTest;
    private HttpServletRequest request;
    private RequestData requestData;
    private ResourceResolver resourceResolver;


    @Rule
    public final SlingContext slingContext = new SlingContext();

    @Before
    public void setup(){
        //Required mocks to create SlingHttpServletRequestImpl object
        resourceResolver = spy(slingContext.resourceResolver());
        requestData = mock(RequestData.class);
        doReturn(resourceResolver).when(requestData).getResourceResolver();

        request = spy(slingContext.request());
        doReturn("/path").when(request).getServletPath();
        doReturn("/path").when(request).getPathInfo();

        underTest = new SlingHttpServletRequestImpl(requestData, request);
    }

    @Test
    public void getUserPrincipal_test() {
        doReturn("remoteUser").when(request).getRemoteUser();
        doReturn(null).when(resourceResolver).adaptTo(Principal.class);

        Assert.assertEquals("UserPrincipal: remoteUser", underTest.getUserPrincipal().toString());
    }

    @Test
    public void getUserPrincipal_test2() {
        doReturn(null).when(request).getRemoteUser();
        doReturn(null).when(resourceResolver).adaptTo(Principal.class);

        Assert.assertNull(underTest.getUserPrincipal());
    }
    
    @Test
    public void getUserPrincipal_test3() {
        final Principal principal = mock(Principal.class);
        doReturn(principal).when(resourceResolver).adaptTo(Principal.class);

        Assert.assertEquals(principal, underTest.getUserPrincipal());
    }
}
