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

import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.engine.servlets.ErrorHandler;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;

import static org.apache.sling.api.SlingConstants.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DefaultErrorHandlerTest {
    private PrintWriter pw;
    private final int errorStatus = 500;
    private final String errorMessage = "Internal error";
    private final IllegalStateException e = new IllegalStateException("Test message");

    private DefaultErrorHandler underTest;
    private MockSlingHttpServletRequest req;
    private MockSlingHttpServletResponse res;

    @Rule
    public final SlingContext context = new SlingContext();

    @Before
    public void setup(){
        underTest = new DefaultErrorHandler();

        req = spy(context.request());

        req.setAttribute(ERROR_SERVLET_NAME, "test");
        req.setAttribute(ERROR_REQUEST_URI, "/");
        RequestProgressTracker progressTracker = mock(RequestProgressTracker.class);
        doReturn(progressTracker).when(req).getRequestProgressTracker();
        doReturn("/").when(req).getRequestURI();

        res = spy(context.response());

        res.setStatus(200);
        pw = mock(PrintWriter.class);
        doReturn(pw).when(res).getWriter();
    }

    @Test
    public void handleErrorTest() throws IOException {
        assertFalse(res.getStatus() == errorStatus);
        underTest.handleError(errorStatus, errorMessage, req, res);
        assertTrue("Status code should be updated", res.getStatus() == errorStatus);
        assertTrue("Response should be commited", res.isCommitted());
        verify(pw, atLeastOnce()).print(anyString());
    }

    @Test
    public void handleErrorWithoutProvidedMessage() throws IOException {
        assertFalse(res.getStatus() == errorStatus);
        underTest.handleError(errorStatus, null, req, res);
        assertTrue("Status code should be updated", res.getStatus() == errorStatus);
        assertTrue("Response should be commited", res.isCommitted());

        //Checking that message was built internally from response status code
        verify(pw, atLeastOnce()).print(anyString());
    }

    @Test
    public void writeErrorToCommittedResponse() throws IOException {
        //Commit the response so it cannot be changed
        res.flushBuffer();

        assertFalse(res.getStatus() == errorStatus);
        underTest.handleError(errorStatus, errorMessage, req, res);
        assertFalse("Status code should be updated because of committed resoponse",
                res.getStatus() == errorStatus);
        //Nothing was written to the response
        verify(pw, times(0)).print(anyString());
    }

    @Test
    public void delegateToAnotherHandler() throws IOException {
        final ErrorHandler delegate = mock(ErrorHandler.class);
        underTest.setDelegate(delegate);

        underTest.handleError(errorStatus, errorMessage, req, res);
        //Checking that error handling was delegated
        verify(delegate, times(1)).handleError(errorStatus, errorMessage, req, res);
    }

    @Test
    public void handleFailedDelegation() throws IOException {
        final ErrorHandler delegate = mock(ErrorHandler.class);
        doThrow(Exception.class).when(delegate).handleError(errorStatus, errorMessage, req, res);
        underTest.setDelegate(delegate);

        underTest.handleError(errorStatus, errorMessage, req, res);

        //Response content was written even after exception thrown by delegated object
        verify(pw, times(1)).print(anyString());
    }

    @Test
    public void handleThrowableError() throws IOException {
        assertFalse(res.getStatus() == errorStatus);
        underTest.handleError(e, req, res);
        assertTrue("Status code should be updated", res.getStatus() == errorStatus);
        assertTrue("Response should be commited", res.isCommitted());

        verify(pw, atLeastOnce()).print(anyString());
        verify(req, times(1)).getRequestProgressTracker();
    }

    @Test
    public void delegateThrowableToAnotherHandler() throws IOException {
        final ErrorHandler delegate = mock(ErrorHandler.class);
        underTest.setDelegate(delegate);

        underTest.handleError(e, req, res);
        //Checking that error handling was delegated
        verify(delegate, times(1)).handleError(e, req, res);
    }
}
