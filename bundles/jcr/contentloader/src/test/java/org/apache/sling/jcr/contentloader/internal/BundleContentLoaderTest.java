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
package org.apache.sling.jcr.contentloader.internal;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junitx.util.PrivateAccessor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.jcr.contentloader.internal.readers.JsonReader;
import org.apache.sling.jcr.contentloader.internal.readers.XmlReader;
import org.apache.sling.jcr.contentloader.internal.readers.ZipReader;
import org.apache.sling.testing.mock.osgi.MockBundle;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.*;
import org.osgi.framework.Bundle;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.apache.sling.jcr.contentloader.internal.ContentLoaderService.*;

public class BundleContentLoaderTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    private BundleContentLoader contentLoader;
    private Session session;

    @Before
    public void prepareContentLoader() throws Exception {
        // prepare content readers
        context.registerInjectActivateService(new JsonReader());
        context.registerInjectActivateService(new XmlReader());
        context.registerInjectActivateService(new ZipReader());

        // whiteboard which holds readers
        context.registerInjectActivateService(new ContentReaderWhiteboard());

        // TODO - SlingRepository should be registered out of the box, not after calling context.resourceResolver()
        // TODO - sling node types should _always_ be registered
        session = context.resourceResolver().adaptTo(Session.class);
        RepositoryUtil.registerSlingNodeTypes(session);

        // register the content loader service
        ContentLoaderService contentLoaderService = new ContentLoaderService();
        MockOsgi.injectServices(contentLoaderService, context.bundleContext());
        MockOsgi.activate(contentLoaderService);
        context.registerService(BundleHelper.class, contentLoaderService);

        BundleHelper bundleHelper = context.getService(BundleHelper.class);

        ContentReaderWhiteboard whiteboard = context.getService(ContentReaderWhiteboard.class);

        contentLoader = new BundleContentLoader(bundleHelper, whiteboard);
    }


    @After
    public void cleanup() throws RepositoryException {
        if(session != null) {
            session.save();
            session.logout();
            session = null;
        }
    }

    //------BundleContentLoader#registerBundle(Session, Bundle, boolean)------//

    @Test
    public void loadContentWithSpecificPath() throws Exception {

        Bundle mockBundle = newBundleWithInitialContent("SLING-INF/libs/app;path:=/libs/app");

        contentLoader.registerBundle(session, mockBundle, false);

        Resource imported = context.resourceResolver().getResource("/libs/app");

        assertThat("Resource was not imported", imported, notNullValue());
        assertThat("sling:resourceType was not properly set", imported.getResourceType(), equalTo("sling:Folder"));
    }

    @Test
    public void loadContentWithRootPath() throws Exception {

        Bundle mockBundle = newBundleWithInitialContent("SLING-INF/");

        contentLoader.registerBundle(session, mockBundle, false);

        Resource imported = context.resourceResolver().getResource("/libs/app");

        assertThat("Resource was not imported", imported, notNullValue());
        assertThat("sling:resourceType was not properly set", imported.getResourceType(), equalTo("sling:Folder"));
    }

    @Test
    @Ignore("TODO - unregister or somehow ignore the XmlReader component for this test")
    public void loadXmlAsIs() throws Exception {

        dumpRepo("/", 2);

        Bundle mockBundle = newBundleWithInitialContent("SLING-INF/libs/app;path:=/libs/app;ignoreImportProviders:=xml");

        contentLoader.registerBundle(context.resourceResolver().adaptTo(Session.class), mockBundle, false);

        Resource imported = context.resourceResolver().getResource("/libs/app");

        assertThat("Resource was not imported", imported, notNullValue());
        assertThat("sling:resourceType was not properly set", imported.getResourceType(), equalTo("sling:Folder"));

        Resource xmlFile = context.resourceResolver().getResource("/libs/app.xml");

        dumpRepo("/", 2);

        assertThat("XML file was was not imported", xmlFile, notNullValue());

    }

    //Petr's tests are below

    @Test
    @Ignore
    public void registerBundleWithLockedNode() throws RepositoryException, NoSuchFieldException {
        final List<Bundle> delayedBundles = (List<Bundle>) PrivateAccessor.getField(contentLoader, "delayedBundles");
        final Bundle bundle = createBundleWithLockedNode("SLING-INF/");

        int delayedBundlesAmount = delayedBundles.size();
        contentLoader.registerBundle(session, bundle, false);
        assertEquals("Bundle should be added to list of delayed", delayedBundlesAmount + 1, delayedBundles.size());

        Resource imported = context.resourceResolver().getResource("/libs/app");
        assertNull("Resource was imported, but shouldn't", imported);
    }

    @Test
    @Ignore
    public void registerBundleAndDelayedBundles() throws RepositoryException, NoSuchFieldException {
        final List<Bundle> delayedBundles = (List<Bundle>) PrivateAccessor.getField(contentLoader, "delayedBundles");
        final Bundle bundle = newBundleWithInitialContent("SLING-INF/");

        //These bundles will be added to BundleContent#delayedList
        final Bundle[] bundlesToDelay = {createBundleWithLockedNode(uniqueId()),
                createBundleWithLockedNode(uniqueId()), createBundleWithLockedNode(uniqueId())};
        for(Bundle toDelay: bundlesToDelay){
            contentLoader.registerBundle(session, toDelay, false);
            Node bcNode = getOrCreateBundleContentNode(toDelay);
            bcNode.unlock();
        }
        session.save();
        //Checking that all bundles are in list of delayed
        assertTrue(delayedBundles.containsAll(Arrays.asList(bundlesToDelay)));

        contentLoader.registerBundle(session, bundle, false);
        Resource imported = context.resourceResolver().getResource("/libs/app");
        assertNotNull("Resource was imported, but shouldn't", imported);
        assertEquals("delayedBundles should be empty", 0, delayedBundles.size());
    }

    @Test
    @Ignore
    public void test() throws Exception {
        Bundle mockBundle = newBundleWithInitialContent("SLING-INF/");

        contentLoader.registerBundle(session, mockBundle, false);

        Resource imported = context.resourceResolver().getResource("/libs/app");
        assertThat("Resource was not imported", imported, notNullValue());
        assertThat("sling:resourceType was not properly set", imported.getResourceType(), equalTo("sling:Folder"));

        Node bcNode = getOrCreateBundleContentNode(mockBundle);
        assertTrue(bcNode.hasProperty(PROPERTY_CONTENT_LOADED));
        assertTrue(bcNode.getProperty(PROPERTY_CONTENT_LOADED).getBoolean());

        contentLoader.unregisterBundle(session, mockBundle);
        assertTrue(bcNode.hasProperty(PROPERTY_CONTENT_LOADED));
        assertFalse(bcNode.getProperty(PROPERTY_CONTENT_LOADED).getBoolean());
    }

    private MockBundle newBundleWithInitialContent(String initialContentHeader) {
        MockBundle mockBundle = new MockBundle(context.bundleContext());
        mockBundle.setHeaders(singletonMap("Sling-Initial-Content", initialContentHeader));
        return mockBundle;
    }


    private void dumpRepo(String startPath, int maxDepth) {
        dumpRepo0(startPath, maxDepth, 0);
    }


    private void dumpRepo0(String startPath, int maxDepth, int currentDepth) {
        Resource resource = context.resourceResolver().getResource(startPath);
        StringBuilder format = new StringBuilder();
        for ( int i = 0 ;i  < currentDepth ; i++) {
            format.append("  ");
        }
        format.append("%s [%s]%n");
        String name = resource.getName().length() == 0  ? "/" : resource.getName();
        System.out.format(format.toString(), name, resource.getResourceType());
        currentDepth++;
        if ( currentDepth > maxDepth) {
            return;
        }
        for ( Resource child : resource.getChildren() ) {
            dumpRepo0(child.getPath(), maxDepth, currentDepth);
        }
    }

    private Bundle createBundleWithLockedNode(String initialContentHeader) throws RepositoryException {
        MockBundle b = new MockBundle(context.bundleContext());
        b.setHeaders(singletonMap("Sling-Initial-Content", initialContentHeader));
        b.setSymbolicName(uniqueId());

        final Node parentNode = (Node)session.getItem(ContentLoaderService.BUNDLE_CONTENT_NODE);
        final Node bcNode = parentNode.hasNode(b.getSymbolicName()) ?
                parentNode.getNode(b.getSymbolicName()) : parentNode.addNode(b.getSymbolicName(), "nt:unstructured");
        bcNode.addMixin("mix:lockable");
        session.save();
        bcNode.lock(false, true);

        return b;
    }

    private final String uniqueId() {
        return getClass().getSimpleName() + UUID.randomUUID();
    }

    private Node getOrCreateBundleContentNode(Bundle bundle) throws RepositoryException {
        final String name = bundle.getSymbolicName();
        final Node parent = (Node)session.getItem(ContentLoaderService.BUNDLE_CONTENT_NODE);

        if(!parent.hasNode(name)){
            parent.addNode(name, "nt:unstructured");
        }

        return parent.getNode(name);
    }
}
