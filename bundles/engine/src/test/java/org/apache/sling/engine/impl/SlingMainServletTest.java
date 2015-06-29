package org.apache.sling.engine.impl;

import junitx.util.PrivateAccessor;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.engine.impl.helper.RequestListenerManager;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockServletContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class SlingMainServletTest {
    private SlingMainServlet underTest;

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Before
    public void setup(){
        underTest = new SlingMainServlet();
    }

    @Test
    public void testService() throws ServletException, NoSuchFieldException, IOException {
        RequestListenerManager r = new RequestListenerManager(context.bundleContext(), new MockServletContext());
        PrivateAccessor.setField(underTest, "requestListenerManager", r);

        final MockSlingHttpServletRequest req = spy(context.request());
        req.setAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER, context.resourceResolver());

        final MockSlingHttpServletResponse res = spy(context.response());

        final SlingRequestProcessorImpl requestProcessor = mock(SlingRequestProcessorImpl.class);
        PrivateAccessor.setField(underTest, "requestProcessor", requestProcessor);

        doReturn("127.0.0.1").when(req).getRemoteAddr();
        doReturn("HTTP/1.1").when(req).getProtocol();
        doReturn("/system/console/config").when(req).getRequestURI();

        underTest.service(req, res);
        verify(requestProcessor, times(1)).doProcessRequest(req,
                res, context.resourceResolver());
    }
}
