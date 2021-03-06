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

import org.apache.sling.jcr.contentloader.ContentImportListener;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.jcr.contentloader.ContentTypeUtil;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.apache.sling.jcr.contentloader.internal.readers.JsonReader;
import org.apache.sling.jcr.contentloader.internal.readers.XmlReader;
import org.apache.sling.jcr.contentloader.internal.readers.ZipReader;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.apache.sling.jcr.contentloader.internal.DefaultContentImporterTest.CustomContentImportListener.*;

public class DefaultContentImporterTest {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final String XML_PATH = "src/test/resources/xml-node.xml";
    private final String JSON_PATH = "src/test/resources/json-node.json";
    private final ImportOptions importOptions = U.createImportOptions(false, false, false, false, false);

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    private DefaultContentImporter contentImporter;

    private Session session;
    private Node parentNode;

    @Before
    public void setup() throws RepositoryException {
        session = context.resourceResolver().adaptTo(Session.class);
        parentNode = session.getRootNode().addNode(getClass().getSimpleName()).addNode(uniqueId());

        // prepare content readers
        context.registerInjectActivateService(new JsonReader());
        context.registerInjectActivateService(new XmlReader());
        context.registerInjectActivateService(new ZipReader());

        // whiteboard which holds readers
        context.registerInjectActivateService(new ContentReaderWhiteboard());
        context.registerInjectActivateService(new DefaultContentImporter());

        contentImporter = (DefaultContentImporter) context.getService(ContentImporter.class);
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

    @Test
    public void createNodeFromJcrXml() throws IOException, RepositoryException {
        final String nodeName = "xmlNode";
        final CustomContentImportListener listener = new CustomContentImportListener();

        final FileInputStream nodeContent = new FileInputStream(XML_PATH);
        try {
            assertFalse(parentNode.hasNode(nodeName));
            assertFalse(listener.callBackData.containsKey(ON_CREATE));
            contentImporter.importContent(parentNode, "xmlNode.jcr.xml", nodeContent, importOptions, listener);
        } catch(IOException e) {
            LOGGER.error("IOException", e);
        } finally {
            nodeContent.close();
        }

        //Checking that node was created and callback was called
        assertTrue("Node wasn't created.", parentNode.hasNode(nodeName));
        assertTrue(listener.callBackData.containsKey(ON_CREATE));
    }

    @Test
    public void createNodeFromJson() throws IOException, RepositoryException {
        final String nodeName = "jsonNode";

        final FileInputStream nodeContent = new FileInputStream(JSON_PATH);
        try {
            assertFalse(parentNode.hasNode(nodeName));
            contentImporter.importContent(parentNode, "jsonNode.json",
                    nodeContent, importOptions, new CustomContentImportListener());
        } catch(IOException e) {
            LOGGER.error("IOException", e);
        } finally {
            nodeContent.close();
        }

        //Checking that node was created
        assertTrue("Node wasn't created.", parentNode.hasNode(nodeName));
    }

    @Test
    public void createNodeWithJcrXmlType() throws IOException, RepositoryException {
        final String nodeName = "xmlNode";
        final CustomContentImportListener listener = new CustomContentImportListener();

        final FileInputStream nodeContent = new FileInputStream(XML_PATH);
        try {
            assertFalse(parentNode.hasNode(nodeName));
            assertFalse(listener.callBackData.containsKey(ON_CREATE));
            contentImporter.importContent(parentNode, "xmlNode.jcr.xml",
                    ContentTypeUtil.TYPE_JCR_XML, nodeContent, importOptions, listener);
        } catch(IOException e) {
            LOGGER.error("IOException", e);
        } finally {
            nodeContent.close();
        }

        //Checking that node was created and callback was called
        assertTrue("Node wasn't created.", parentNode.hasNode(nodeName));
        assertTrue(listener.callBackData.containsKey(ON_CREATE));
    }

    @Test
    public void createNodeWithJsonType() throws IOException, RepositoryException {
        final String nodeName = "jsonNode";

        final FileInputStream nodeContent = new FileInputStream(JSON_PATH);
        try {
            assertFalse(parentNode.hasNode(nodeName));
            contentImporter.importContent(parentNode, nodeName,
                    ContentTypeUtil.TYPE_JSON, nodeContent, importOptions, new CustomContentImportListener());
        } catch(IOException e) {
            LOGGER.error("IOException", e);
        } finally {
            nodeContent.close();
        }

        //Checking that node was created
        assertTrue("Node wasn't created.", parentNode.hasNode(nodeName));
    }

    private final String uniqueId() {
        return getClass().getSimpleName() + UUID.randomUUID();
    }

    protected static class CustomContentImportListener implements ContentImportListener {
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
