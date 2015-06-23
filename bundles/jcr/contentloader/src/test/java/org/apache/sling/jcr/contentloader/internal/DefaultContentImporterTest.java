/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.contentloader.internal;

import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.contentloader.ContentImportListener;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.jcr.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.apache.sling.jcr.contentloader.internal.DefaultContentImporterTest.CustomContentImportListener.*;

public class DefaultContentImporterTest {

    private final String XML_PATH = "src/test/resources/reader/node.xml";
    private final String JSON_PATH = "src/test/resources/reader/content.json";
    private DefaultContentImporter contentImporter;

    private Session session;
    private Node parentNode;

    @Before
    public void setup() throws RepositoryException {
        final SlingRepository repo = RepositoryProvider.instance().getRepository();
        session = repo.loginAdministrative(null);
        parentNode = session.getRootNode().addNode(getClass().getSimpleName()).addNode(uniqueId());

        contentImporter = new DefaultContentImporter();
    }

    //-----DefaultContentImporter#importContent(Node, String, InputStream, ImportOptions, ContentImportListener)-----//

    @Test
    @Ignore
    public void createNewNodeFromJcrXml() throws IOException, RepositoryException {
        final String nodeName = "node";
        final File jcrXml = new File(XML_PATH);
        final FileInputStream is = new FileInputStream(jcrXml);
        final ImportOptions options = U.createImportOptions(false, false, false, false, false);
        final CustomContentImportListener listener = new CustomContentImportListener();
        final Map<String, String> callback = listener.callBackData;

        assertFalse(parentNode.hasNode(nodeName));
        assertFalse(callback.containsKey(ON_CREATE));
        contentImporter.importContent(parentNode, "node.jcr.xml", is, options, listener);
        //Checking that node was created and callback was called
        assertTrue("Node wasn't created.", parentNode.hasNode(nodeName));
        assertTrue(callback.containsKey(ON_CREATE));
    }

    @Test
    @Ignore
    public void createNewNodeFromXml() throws IOException, RepositoryException {
        final String nodeName = "node";
        final File jcrXml = new File(JSON_PATH);
        final FileInputStream is = new FileInputStream(jcrXml);
        final ImportOptions options = U.createImportOptions(false, false, false, false, false);
        final CustomContentImportListener listener = new CustomContentImportListener();
        final Map<String, String> callback = listener.callBackData;

        assertFalse(parentNode.hasNode(nodeName));
        assertFalse(callback.containsKey(ON_CREATE));
        contentImporter.importContent(parentNode, "node.json", is, options, listener);
        //Checking that node was created and callback was called
        assertTrue("Node wasn't created.", parentNode.hasNode(nodeName));
        assertTrue(callback.containsKey(ON_CREATE));
    }



    @After
    public void shutdown() throws RepositoryException {
        contentImporter = null;
        if(session != null) {
            session.save();
            session.logout();
            session = null;
        }
    }

    private final String uniqueId() {
        return getClass().getSimpleName() + UUID.randomUUID();
    }

    public static class CustomContentImportListener implements ContentImportListener {
        public static final String ON_MODIFY = "ON_MODIFY";
        public static final String ON_DELETE = "ON_DELETE";
        public static final String ON_MOVE = "ON_MOVE";
        public static final String ON_COPY = "ON_COPY";
        public static final String ON_CREATE = "ON_CREATE";
        public static final String ON_REORDER = "ON_REORDER";
        public static final String ON_CHECKIN = "ON_CHECKIN";
        public static final String ON_CHECKOUT = "ON_CHECKOUT";

        public Map<String, String> callBackData = new HashMap<String, String>();

        @Override
        public void onModify(String srcPath) {
            callBackData.put(ON_MODIFY, srcPath);
        }

        @Override
        public void onDelete(String srcPath) {
            callBackData.put(ON_DELETE, srcPath);
        }

        @Override
        public void onMove(String srcPath, String destPath) {
            callBackData.put(ON_MOVE, srcPath);
        }

        @Override
        public void onCopy(String srcPath, String destPath) {
            callBackData.put(ON_COPY, srcPath);
        }

        @Override
        public void onCreate(String srcPath) {
            callBackData.put(ON_CREATE, srcPath);
        }

        @Override
        public void onReorder(String orderedPath, String beforeSibbling) {
            callBackData.put(ON_REORDER, orderedPath);
        }

        @Override
        public void onCheckin(String srcPath) {
            callBackData.put(ON_CHECKIN, srcPath);
        }

        @Override
        public void onCheckout(String srcPath) {
            callBackData.put(ON_CHECKOUT, srcPath);
        }
    }
}
