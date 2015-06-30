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

import org.apache.sling.commons.testing.junit.Retry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BundleContentLoaderIT extends AbstractContentLoaderIT {
    private static final Integer THREAD_SLEEP = 300;
    private Session session;

    @Before
    public void setup() throws RepositoryException {
        session = repository.loginAdministrative(null);
    }



    private static final String FIRST_SYMBOLIC = UUID.randomUUID().toString();
    private static final String SECOND_SYMBOLIC = UUID.randomUUID().toString();

    @Test
    @Retry(intervalMsec=RETRY_INTERVAL, timeoutMsec=RETRY_TIMEOUT)
    /**
     * Test creates first bundle with initial content and then the second one, with pretty same initial content.
     * The only difference between content nodes are properties. After both nodes are installed, all properties should
     * exists.
     * Then, when we uninstall these bundles, test checks that initial content was NOT REMOVED, because 'uninstall' and
     * 'overwrite' properties are not set.
     */
    public void createContentForTwoBundles() throws IOException, RepositoryException, BundleException, InterruptedException {
        final String testNodeName = "/" + FIRST_SYMBOLIC + "/content/test-node";
        final String props = ";path:=/" + FIRST_SYMBOLIC;

        InputStream is = createBundleStream(FIRST_SYMBOLIC, "0/content.json", props);
        if(is != null) { //is==null if bundle was installed on previous method run
            try {
                assertFalse("Node should not exist before bundle is installed", session.itemExists(testNodeName));
                installBundle(FIRST_SYMBOLIC, is);
            } catch (BundleException e) {
                LOGGER.error("BundleException: {}", e);
            } finally {
                is.close();
            }
        }
        assertTrue("Node should be created after bundle is installed", session.itemExists(testNodeName));

        is = createBundleStream(SECOND_SYMBOLIC, "1/content.json", props);
        if(is != null) { //is==null if bundle was installed on previous method run
            try {
                assertTrue("Property 'foo' should be equal to 'bar'",
                        session.getNode(testNodeName).getProperty("foo").getString().equals("bar"));
                assertFalse("Property 'foo1' should not exist before node is installed",
                        session.getNode(testNodeName).hasProperty("foo1"));
                installBundle(SECOND_SYMBOLIC, is);
            } catch (BundleException e) {
                LOGGER.error("BundleException: {}", e);
            } finally {
                is.close();
            }
        }
        assertTrue("Property 'foo' should not be overwritten",
                session.getNode(testNodeName).getProperty("foo").getString().equals("bar"));
        assertTrue("Property 'foo1' should be equal to 'bar1'",
                session.getNode(testNodeName).getProperty("foo1").getString().equals("bar1"));

        uninstallBundle(FIRST_SYMBOLIC);
        uninstallBundle(SECOND_SYMBOLIC);

        Thread.sleep(THREAD_SLEEP);

        //Since uninstall and overwrite parameters are not set, content should not be deleted
        assertTrue("Property foo should not be deleted",
                session.getNode(testNodeName).getProperty("foo").getString().equals("bar"));
        assertTrue("Property foo1 should not be deleted",
                session.getNode(testNodeName).getProperty("foo1").getString().equals("bar1"));
    }



    private static final String THIRD_SYMBOLIC = UUID.randomUUID().toString();
    private static final String FOURTH_SYMBOLIC = UUID.randomUUID().toString();

    @Test
    @Retry(intervalMsec=RETRY_INTERVAL, timeoutMsec=RETRY_TIMEOUT)
    /**
     * Test creates first bundle with initial content and then the second one, with same destination path for initial content.
     * After first bundle is installed we check that initial content was added.
     * After second bundle is installed with overwrite=true property the initial content of first node should overwritten
     * Then after both bundles are uninstalled we checking that their content was REMOVED from repository.
     */
    public void overwriteExistingBundleContent() throws IOException, RepositoryException, BundleException, InterruptedException {
        final String testNodeName = "/" + THIRD_SYMBOLIC + "/content/test-node";
        final String props = ";overwrite:=true;path:=/" + THIRD_SYMBOLIC; //'uninstall' flag by default equal to 'overwrite' flag value

        InputStream is = createBundleStream(THIRD_SYMBOLIC, "0/content.json", props);
        if(is != null) { //is==null if bundle was installed on previous method run
            try {
                assertFalse("Node should not exists before bundle is installed", session.itemExists(testNodeName));
                installBundle(THIRD_SYMBOLIC, is);
            } catch (BundleException e) {
                LOGGER.error("BundleException: {}", e);
            } finally {
                is.close();
            }
        }
        assertTrue("Node should be created after bundle is installed", session.itemExists(testNodeName));

        is = createBundleStream(FOURTH_SYMBOLIC, "1/content.json", props);
        if(is != null) { //is==null if bundle was installed on previous method run
            try {
                assertTrue("Property 'foo' should be equal to bar",
                        session.getNode(testNodeName).getProperty("foo").getString().equals("bar"));
                assertFalse("Property 'foo1' should not be created before node is installed",
                        session.getNode(testNodeName).hasProperty("foo1"));
                installBundle(FOURTH_SYMBOLIC, is);
            } catch (BundleException e) {
                e.printStackTrace();
            } finally {
                is.close();
            }
        }
        assertFalse("Property 'foo' should be deleted because of overwrite flag",
                session.getNode(testNodeName).hasProperty("foo"));
        assertTrue("Property 'foo1' should be equal to bar1",
                session.getNode(testNodeName).getProperty("foo1").getString().equals("bar1"));

        uninstallBundle(THIRD_SYMBOLIC);
        uninstallBundle(FOURTH_SYMBOLIC);

        Thread.sleep(THREAD_SLEEP);

        assertFalse("Node should be deleted", session.itemExists(testNodeName));
    }



    private static final String FIFTH_SYMBOLIC = UUID.randomUUID().toString();
    private static final String SIXTH_SYMBOLIC = UUID.randomUUID().toString();

    @Test
    @Retry(intervalMsec=RETRY_INTERVAL, timeoutMsec=RETRY_TIMEOUT)
    /**
     * Test creates first bundle with initial content and then the second one, with same destination path for initial content.
     * After first bundle is installed we check that initial content was added.
     * After second bundle is installed with overwrite=true property the initial content of first node should overwritten
     * Then after both bundles are uninstalled we checking that their content was NOT REMOVED from repository.
     */
    public void testOverwritePropertyFlag() throws IOException, RepositoryException, BundleException, InterruptedException {
        final String testNodeName = "/" + FIFTH_SYMBOLIC + "/content/test-node";
        final String props = ";overwriteProperties:=true;path:=/" + FIFTH_SYMBOLIC;

        InputStream is = createBundleStream(FIFTH_SYMBOLIC, "0/content.json", props);
        if(is != null) { //is==null if bundle was installed on previous method run
            try {
                assertFalse("Node should not exists before bundle is installed", session.itemExists(testNodeName));
                installBundle(FIFTH_SYMBOLIC, is);
            } catch (BundleException e) {
                e.printStackTrace();
            } finally {
                is.close();
            }
        }
        assertTrue("Node should be created after bundle is installed", session.itemExists(testNodeName));

        is = createBundleStream(SIXTH_SYMBOLIC, "0/content.xml", props);
        if(is != null) { //is==null if bundle was installed on previous method run
            try {
                assertTrue("Property should be equal to bar", session.getNode(testNodeName).getProperty("foo").getString().equals("bar"));
                assertFalse("Node should not be created before node is installed", session.getNode(testNodeName).hasProperty("foo-new"));
                installBundle(SIXTH_SYMBOLIC, is);
            } catch (BundleException e) {
                e.printStackTrace();
            } finally {
                is.close();
            }
        }
        assertTrue("Property should be equal to bar-new", session.getNode(testNodeName).getProperty("foo").getString().equals("bar-new"));

        uninstallBundle(FIFTH_SYMBOLIC);
        uninstallBundle(SIXTH_SYMBOLIC);

        Thread.sleep(THREAD_SLEEP);

        assertTrue("Node should be deleted", session.itemExists(testNodeName));
    }



    private static final String SEVENTH_SYMBOLIC = UUID.randomUUID().toString();

    @Test
    @Retry(intervalMsec=RETRY_INTERVAL, timeoutMsec=RETRY_TIMEOUT)
    /**
     * Test creates bundle with initial content and then checks it in.
     * After bundle is uninstalled node should NOT be REMOVED.
     */
    public void loadAndCheckinContent() throws IOException, RepositoryException, BundleException, InterruptedException {
        final String testNodeName = "/" + SEVENTH_SYMBOLIC + "/content/test-node";
        final String props = ";checkin:=true;path:=/" + SEVENTH_SYMBOLIC;

        InputStream is = createBundleStream(SEVENTH_SYMBOLIC, "2/content.json", props);
        if(is != null) { //is==null if bundle was installed on previous method run
            try {
                assertFalse("Node should not exists before bundle is installed", session.itemExists(testNodeName));
                installBundle(SEVENTH_SYMBOLIC, is);
            } catch (BundleException e) {
                e.printStackTrace();
            } finally {
                is.close();
            }
        }
        final Node initContent = session.getNode(testNodeName);
        assertTrue("Property from file should not be added, since import provider ignored",
                initContent.getProperty("foo2").getString().equals("bar2"));
        assertFalse("If node was checked in, value of jcr:isCheckedOut chould be equals to FALSE",
                initContent.getProperty("jcr:isCheckedOut").getBoolean());

        uninstallBundle(SEVENTH_SYMBOLIC);

        Thread.sleep(THREAD_SLEEP);

        assertTrue("Node should not be deleted", session.itemExists(testNodeName));
    }



    private static final String EIGHTH_SYMBOLIC = UUID.randomUUID().toString();

    @Test
    @Retry(intervalMsec=RETRY_INTERVAL, timeoutMsec=RETRY_TIMEOUT)
    /**
     * Test creates bundle's initial content with unextracted json data
     * After bundle is uninstalled node should NOT be REMOVED.
     */
    public void checkinNodeWithoutImportProvider() throws IOException, RepositoryException, BundleException, InterruptedException {
        final String testNodeName = "/" + EIGHTH_SYMBOLIC + "/content";
        final String props = ";ignoreImportProviders:=\"json\";path:=/" + EIGHTH_SYMBOLIC;

        InputStream is = createBundleStream(EIGHTH_SYMBOLIC, "0/content.json", props);
        if(is != null) { //is==null if bundle was installed on previous method run
            try {
                assertFalse("Node should not exist", session.itemExists(testNodeName));
                assertFalse("Node should not exist", session.itemExists(testNodeName + ".json/jcr:content"));
                installBundle(EIGHTH_SYMBOLIC, is);
            } catch (BundleException e) {
                e.printStackTrace();
            } finally {
                is.close();
            }
        }
        assertFalse("Extracted node should not exist", session.itemExists(testNodeName));
        assertTrue("Node should exist as raw data", session.itemExists(testNodeName + ".json/jcr:content"));

        uninstallBundle(EIGHTH_SYMBOLIC);

        Thread.sleep(THREAD_SLEEP);

        assertTrue("Node should not be deleted after bundle was uninstalled", session.itemExists(testNodeName + ".json/jcr:content"));
    }
}
