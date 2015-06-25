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
package org.apache.sling.jcr.contentloader.it;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.testing.junit.Retry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/** Basic test of a bundle that provides initial content */
@RunWith(PaxExam.class)
public class BasicInitialContentIT extends ContentBundleTestBase {
    private final String testNodePath = contentRootPath + "/basic-content/test-node";

    protected TinyBundle setupTestBundle(TinyBundle b) throws IOException {
        b.set(SLING_INITIAL_CONTENT_HEADER, DEFAULT_PATH_IN_BUNDLE + ";path:=" + contentRootPath);
        addContent(b, DEFAULT_PATH_IN_BUNDLE, "0/basic-content.json");
        return b;
    }
    
    @Test
    @Retry(intervalMsec=RETRY_INTERVAL, timeoutMsec=RETRY_TIMEOUT)
    public void bundleStarted() {
        final Bundle b = P.findBundle(bundleContext, bundleSymbolicName);
        assertNotNull("Expecting bundle to be found:" + bundleSymbolicName, b);
        assertEquals("Expecting bundle to be active:" + bundleSymbolicName, Bundle.ACTIVE, b.getState());
    }
    
    @Test
    @Retry(intervalMsec=RETRY_INTERVAL, timeoutMsec=RETRY_TIMEOUT)
    public void initialContentInstalled() throws RepositoryException {
        assertTrue("Expecting initial content to be installed", session.itemExists(testNodePath));
        assertEquals("Expecting foo=bar", "bar", session.getNode(testNodePath).getProperty("foo").getString()); 
    }

    @Test
    @Retry(intervalMsec=RETRY_INTERVAL, timeoutMsec=RETRY_TIMEOUT)
    public void newContentAdded() throws Exception {
        if(bundlesToRemove.size() == 1) {
            final TinyBundle tiny = createBundleWithContent("1/basic-content.json", ";path:=" + contentRootPath);
            final String symbolicName = tiny.getHeader(Constants.BUNDLE_SYMBOLICNAME);
            final InputStream is = tiny.build(TinyBundles.withBnd());
            assertFalse("Property foo1 should not be created yet", session.getNode(testNodePath).hasProperty("foo1"));
            Bundle bundle = bundleContext.installBundle(symbolicName, is);
            bundle.start();
        }

        assertTrue("Expecting initial content to be installed", session.itemExists(testNodePath));
        assertEquals("Expecting foo=bar", "bar", session.getNode(testNodePath).getProperty("foo").getString());
    }

    private void printNode(Node n) throws RepositoryException {
        NodeIterator i = n.getNodes();
        while (i.hasNext()){
            printNode(i.nextNode());
        }
        log.info("POP: " + n.getPath());
    }

}