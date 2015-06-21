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

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.*;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.Privilege;

import junitx.util.PrivateAccessor;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.value.BinaryValue;
import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.contentloader.ContentImportListener;
import org.apache.sling.jcr.contentloader.ContentReader;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class DefaultContentCreatorTest {

    static final String DEFAULT_NAME = "default-name";
    final Mockery mockery = new JUnit4Mockery();
    DefaultContentCreator contentCreator;

    Session session;
    Node parentNode;
    Property prop;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        final SlingRepository repo = RepositoryProvider.instance().getRepository();
        session = repo.loginAdministrative(null);
        contentCreator = new DefaultContentCreator(new DefaultContentImporter());
        contentCreator.init(U.createImportOptions(true, true, true, false, false),
                new HashMap<String, ContentReader>(), null, null);
        parentNode = session.getRootNode().addNode(getClass().getSimpleName()).addNode(uniqueId());
    }

    @After
    public void cleanup() throws RepositoryException {
        if(session != null) {
            session.save();
            session.logout();
            session = null;
        }
    }

    @Test
    public void willRewriteUndefinedPropertyType() throws RepositoryException {
        parentNode = mockery.mock(Node.class);
        prop = mockery.mock(Property.class);
        contentCreator.init(U.createImportOptions(true, true, true, false, false),
                new HashMap<String, ContentReader>(), null, null);

        contentCreator.prepareParsing(parentNode, null);
        this.mockery.checking(new Expectations() {{
        	allowing(parentNode).isNodeType("mix:versionable"); will(returnValue(Boolean.FALSE));
        	allowing(parentNode).getParent(); will(returnValue(null));
            oneOf (parentNode).hasProperty("foo"); will(returnValue(Boolean.TRUE));
            oneOf (parentNode).setProperty(with(equal("foo")), with(equal("bar")));
        }});
        contentCreator.createProperty("foo", PropertyType.UNDEFINED, "bar");
    }

    @Test
    public void willNotRewriteUndefinedPropertyType() throws RepositoryException {
        parentNode = mockery.mock(Node.class);
        prop = mockery.mock(Property.class);
        contentCreator.init(U.createImportOptions(false, false, true, false, false),
                new HashMap<String, ContentReader>(), null, null);

        contentCreator.prepareParsing(parentNode, null);
        this.mockery.checking(new Expectations() {{
            oneOf (parentNode).hasProperty("foo"); will(returnValue(Boolean.TRUE));
            oneOf (parentNode).getProperty("foo"); will(returnValue(prop));
            oneOf (prop).isNew(); will(returnValue(Boolean.FALSE));
        }});
        contentCreator.createProperty("foo", PropertyType.UNDEFINED, "bar");
    }

    @Test
    public void testDoesNotCreateProperty() throws RepositoryException {
        final String propertyName = "foo";
        prop = mockery.mock(Property.class);
        parentNode = mockery.mock(Node.class);

        this.mockery.checking(new Expectations(){{
            oneOf(parentNode).hasProperty(propertyName); will(returnValue(true));
            oneOf(parentNode).getProperty(propertyName); will(returnValue(prop));
            oneOf(prop).isNew(); will(returnValue(false));
        }});
        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);
        //By calling this method we expect that it will returns on first if-statement
        contentCreator.createProperty("foo", PropertyType.REFERENCE, "bar");
        //Checking that
        mockery.assertIsSatisfied();
    }

    @Test
    public void testCreateReferenceProperty() throws RepositoryException {
        final String propertyName = "foo";
        final String propertyValue = "bar";
        final String rootNodeName = uniqueId();
        final String uuid = "1b8c88d37f0000020084433d3af4941f";
        final Session session = mockery.mock(Session.class);
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);
        parentNode = mockery.mock(Node.class);
        prop = mockery.mock(Property.class);

        this.mockery.checking(new Expectations(){{
            oneOf(session).itemExists(with(any(String.class))); will(returnValue(true));
            oneOf(session).getItem(with(any(String.class))); will(returnValue(parentNode));

            exactly(2).of(parentNode).getPath(); will(returnValue("/" + rootNodeName));
            oneOf(parentNode).isNode(); will(returnValue(true));
            oneOf(parentNode).isNodeType("mix:referenceable"); will(returnValue(true));
            oneOf(parentNode).getUUID(); will(returnValue(uuid));
            oneOf(parentNode).getSession(); will(returnValue(session));
            oneOf(parentNode).hasProperty(with(any(String.class)));
            oneOf(parentNode).setProperty(propertyName, uuid, PropertyType.REFERENCE);
            oneOf(parentNode).getProperty(with(any(String.class)));
            oneOf(listener).onCreate(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode,null);
        contentCreator.createProperty(propertyName, PropertyType.REFERENCE, propertyValue);
        //The only way I found how to test this method is to check numbers of methods calls
        mockery.assertIsSatisfied();
    }

    @Test
    public void testCreateFalseCheckedOutPreperty() throws RepositoryException {
        parentNode = mockery.mock(Node.class);

        this.mockery.checking(new Expectations(){{
            oneOf(parentNode).hasProperty(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        int numberOfVersionablesNodes = contentCreator.getVersionables().size();
        contentCreator.createProperty("jcr:isCheckedOut", PropertyType.UNDEFINED, "false");
        //Checking that list of versionables was changed
        assertEquals(numberOfVersionablesNodes + 1, contentCreator.getVersionables().size());
        mockery.assertIsSatisfied();
    }

    @Test
    public void testCreateTrueCheckedOutPreperty() throws RepositoryException {
        parentNode = mockery.mock(Node.class);

        this.mockery.checking(new Expectations(){{
            oneOf(parentNode).hasProperty(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        int numberOfVersionablesNodes = contentCreator.getVersionables().size();
        contentCreator.createProperty("jcr:isCheckedOut", PropertyType.UNDEFINED, "true");
        //Checking that list of versionables doesn't changed
        assertEquals(numberOfVersionablesNodes, contentCreator.getVersionables().size());
        mockery.assertIsSatisfied();
    }

    @Test
    public void testCreateDateProperty() throws RepositoryException, ParseException {
        final String propertyName = "dateProp";
        final String propertyValue = "2012-10-01T09:45:00.000+02:00";
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);
        parentNode = mockery.mock(Node.class);
        prop = mockery.mock(Property.class);

        this.mockery.checking(new Expectations(){{
            oneOf(parentNode).hasProperty(with(any(String.class)));
            oneOf(parentNode).setProperty(with(any(String.class)), with(any(Calendar.class)));
            oneOf(parentNode).getProperty(with(any(String.class))); will(returnValue(prop));
            oneOf(prop).getPath(); will(returnValue(""));
            oneOf(listener).onCreate(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, null);

        contentCreator.createProperty(propertyName, PropertyType.DATE, propertyValue);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testCreatePropertyWithNewPropertyType() throws RepositoryException, ParseException {
        final String propertyName = "foo";
        final String propertyValue = "bar";
        final Integer propertyType = -1;
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);
        parentNode = mockery.mock(Node.class);
        prop = mockery.mock(Property.class);

        this.mockery.checking(new Expectations(){{
            oneOf(parentNode).hasProperty(with(any(String.class)));
            oneOf(parentNode).getProperty(propertyName); will(returnValue(prop));
            oneOf(parentNode).setProperty(propertyName, propertyValue, propertyType);
            oneOf(prop).getPath(); will(returnValue(""));
            oneOf(listener).onCreate(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, null);

        contentCreator.createProperty(propertyName, propertyType, propertyValue);
        mockery.assertIsSatisfied();
    }

    //----- DefaultContentCreator#createNode(String, String, String[])-------//

    @Test
    public void createNodeWithoutNameAndTwoInStack() throws RepositoryException {
        contentCreator.init(U.createImportOptions(true, true, true, false, false),
                new HashMap<String, ContentReader>(), null, null);
        //Making parentNodeStack.size() == 1
        contentCreator.prepareParsing(parentNode, DEFAULT_NAME);
        //Making parentNodeStack.size() == 2
        contentCreator.createNode(uniqueId(), null, null);

        thrown.expect(RepositoryException.class);
        contentCreator.createNode(null, null, null);
    }

    @Test
    public void createNodeWithoutProvidedNames() throws RepositoryException, NoSuchFieldException {
        Stack nodesStack = (Stack)PrivateAccessor.getField(contentCreator, "parentNodeStack");

        contentCreator.init(U.createImportOptions(true, true, true, false, false),
                new HashMap<String, ContentReader>(), null, null);

        contentCreator.prepareParsing(parentNode, null);
        //Contains parentNode only
        assertEquals(1, nodesStack.size());
        contentCreator.createNode(null, null, null);
        //Node has not been created since any name not provided
        assertEquals(1, nodesStack.size());
    }

    @Test
    public void createNodeWithOverwrite() throws RepositoryException {
        final String newNodeName = uniqueId();
        final String propertyName = uniqueId();
        final String propertyValue = uniqueId();
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);

        this.mockery.checking(new Expectations(){{
            oneOf(listener).onCreate(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(true, false, true, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, DEFAULT_NAME);

        Node nodeToOverwrite = parentNode.addNode(newNodeName);
        nodeToOverwrite.setProperty(propertyName, propertyValue);

        assertTrue(parentNode.getNode(newNodeName).hasProperty(propertyName));
        contentCreator.createNode(newNodeName, null, null);
        //If node was overwritten(as we expect) it will not contain this property
        assertFalse(parentNode.getNode(newNodeName).hasProperty(propertyName));
        mockery.assertIsSatisfied();
    }

    @Test
    public void addMixinsToExistingNode() throws RepositoryException, NoSuchFieldException {
        final String newNodeName = uniqueId();
        final String[] mixins = {"mix:versionable"};
        final List<Node> versionables = (List<Node>) PrivateAccessor.getField(contentCreator, "versionables");

        contentCreator.init(U.createImportOptions(false, false, false, true, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, DEFAULT_NAME);

        Node newNode = parentNode.addNode(newNodeName);
        assertEquals(0, versionables.size());
        assertFalse(newNode.isNodeType(mixins[0]));
        contentCreator.createNode(newNodeName, null, mixins);
        assertEquals(1, versionables.size());
        assertTrue(newNode.isNodeType(mixins[0]));
    }

    @Test
    public void createNodeWithPrimaryType() throws RepositoryException {
        final String newNodeName = uniqueId();
        final List<String> createdNodes = new ArrayList<String>();
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);

        this.mockery.checking(new Expectations(){{
            oneOf(listener).onCreate(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(true, false, true, false, false),
                new HashMap<String, ContentReader>(), createdNodes, listener);
        contentCreator.prepareParsing(parentNode, DEFAULT_NAME);

        int createdNodesSize = createdNodes.size();
        contentCreator.createNode(newNodeName, NodeType.NT_UNSTRUCTURED, null);
        assertEquals(createdNodesSize + 1, createdNodes.size());
        Node createdNode = parentNode.getNode(newNodeName);
        assertNotNull(createdNode);
        assertTrue(createdNode.getPrimaryNodeType().isNodeType(NodeType.NT_UNSTRUCTURED));
        mockery.assertIsSatisfied();
    }

    //----- DefaultContentCreator#createProperty(String, int, String[])-------//

    @Test
    public void propertyDoesntOverwritten() throws RepositoryException {
        final String newPropertyName = uniqueId();
        final String newPropertyValue = uniqueId();
        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, DEFAULT_NAME);

        parentNode.setProperty(newPropertyName, newPropertyValue);
        session.save();
        contentCreator.createProperty(newPropertyName, PropertyType.REFERENCE, new String[]{"bar1", "bar2"});
        //Checking that property is old
        assertTrue(!parentNode.getProperty(newPropertyName).isNew());
        assertEquals(newPropertyValue, parentNode.getProperty(newPropertyName).getString());
    }


    @Test
    public void createReferenceProperty() throws RepositoryException, NoSuchFieldException {
        final String propName = uniqueId();
        final String[] propValues = {uniqueId(), uniqueId()};
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);
        final Map<String, String[]> delayedMultipleReferences =
                (Map<String, String[]>)PrivateAccessor.getField(contentCreator, "delayedMultipleReferences");
        this.mockery.checking(new Expectations(){{
            oneOf(listener).onCreate(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, DEFAULT_NAME);

        contentCreator.createProperty(propName, PropertyType.REFERENCE, propValues);
        assertTrue(parentNode.hasProperty(propName));
        assertTrue(parentNode.getProperty(propName).isNew());

        String referencesKey = parentNode.getPath() + "/" + propName;
        String[] uuidsOrPaths = delayedMultipleReferences.get(referencesKey);
        assertNotNull(uuidsOrPaths);
        assertEquals(propValues.length, uuidsOrPaths.length);
        mockery.assertIsSatisfied();
    }

    @Test
    public void createDateProperty() throws RepositoryException, ParseException {
        final String propName = "dateProp";
        final String[] propValues = {"2012-10-01T09:45:00.000+02:00", "2011-02-13T09:45:00.000+02:00"};
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);

        this.mockery.checking(new Expectations(){{
            oneOf(listener).onCreate(with(any(String.class)));
        }});
        parentNode.addMixin("mix:versionable");

        contentCreator.init(U.createImportOptions(false, false, true, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, null);

        assertFalse(parentNode.hasProperty(propName));
        contentCreator.createProperty(propName, PropertyType.DATE, propValues);
        assertTrue(parentNode.hasProperty(propName));

        Property dateProp = parentNode.getProperty(propName);
        assertTrue(dateProp.isNew());
        assertEquals(propValues.length, dateProp.getValues().length);

        mockery.assertIsSatisfied();
    }

    @Test
    public void createUndefinedProperty() throws RepositoryException {
        final String propName = uniqueId();
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);

        this.mockery.checking(new Expectations(){{
            oneOf(listener).onCreate(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, null);

        assertFalse(parentNode.hasProperty(propName));
        contentCreator.createProperty(propName, PropertyType.UNDEFINED, new String[]{});
        assertTrue(parentNode.hasProperty(propName));
        mockery.assertIsSatisfied();
    }

    @Test
    public void createOtherProperty() throws RepositoryException {
        final String propName = uniqueId();

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        assertFalse(parentNode.hasProperty(propName));
        contentCreator.createProperty(propName, PropertyType.STRING, new String[]{});
        assertTrue(parentNode.hasProperty(propName));
    }

    //------DefaultContentCreator#finishNode()------//

    @Test
    public void finishNodeWithMultipleProperty() throws RepositoryException, NoSuchFieldException {
        final String propName = uniqueId();
        final String underTestNodeName = uniqueId();
        final Map<String, List<String>> delayedMultipleReferences =
                (Map<String, List<String>>) PrivateAccessor.getField(contentCreator, "delayedMultipleReferences");
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);

        this.mockery.checking(new Expectations(){{
            exactly(3).of(listener).onCreate(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, null);

        contentCreator.createProperty(propName, PropertyType.REFERENCE, new String[]{underTestNodeName});
        contentCreator.createNode(underTestNodeName, null, null);
        assertEquals(1, delayedMultipleReferences.size());

        Node underTest = parentNode.getNode(underTestNodeName);
        underTest.addMixin("mix:referenceable");

        contentCreator.finishNode();
        assertEquals(0, delayedMultipleReferences.size());
        mockery.assertIsSatisfied();
    }

    @Test
    public void finishNodeWithSingleProperty() throws RepositoryException, NoSuchFieldException {
        final String propName = uniqueId();
        final String underTestNodeName = uniqueId();
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);

        this.mockery.checking(new Expectations(){{
            exactly(2).of(listener).onCreate(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, null);

        contentCreator.createProperty(propName, PropertyType.REFERENCE, underTestNodeName);
        contentCreator.createNode(underTestNodeName, null, null);

        Node underTest = parentNode.getNode(underTestNodeName);
        underTest.addMixin("mix:referenceable");

        contentCreator.finishNode();
        assertEquals(underTest.getUUID(), parentNode.getProperty(propName).getString());
        mockery.assertIsSatisfied();
    }

    @Test
    public void finishNodeWithoutProperties() throws RepositoryException, NoSuchFieldException {
        final String propName = uniqueId();
        final String underTestNodeName = uniqueId();

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        contentCreator.createProperty(propName, PropertyType.UNDEFINED, underTestNodeName);
        contentCreator.createNode(underTestNodeName, null, null);

        contentCreator.finishNode();
        assertEquals(underTestNodeName, parentNode.getProperty(propName).getString());
    }

    @Test
    public void finishNotReferenceableNode() throws RepositoryException, NoSuchFieldException {
        final String propName = uniqueId();
        final String underTestNodeName = uniqueId();

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        contentCreator.createProperty(propName, PropertyType.REFERENCE, underTestNodeName);
        contentCreator.createNode(underTestNodeName, null, null);

        contentCreator.finishNode();
        //False since it doesn't have referenceable mixin
        assertFalse(parentNode.hasProperty(propName));
    }

    //------DefaultContentCreator#createFileAndResourceNode(String, InputStream, String, long)------//

    @Test
    public void createNewFileAndResourceNode() throws RepositoryException, NoSuchFieldException {
        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        final String name = uniqueId();
        final Long lastModified = 234248432L;
        contentCreator.createFileAndResourceNode(name, new ByteArrayInputStream(new byte[0]), null, lastModified);

        assertNotNull(parentNode.getNode(name));

        Node contentNode = parentNode.getNode(name).getNode("jcr:content");
        assertNotNull(contentNode);

        final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
        assertEquals(DEFAULT_CONTENT_TYPE, contentNode.getProperty("jcr:mimeType").getString());
        assertEquals(lastModified, contentNode.getProperty("jcr:lastModified").getLong(), 0);
        assertNotNull(contentNode.getProperty("jcr:data"));
    }

    @Test
    public void overwriteExistingFileAndResourceNode() throws RepositoryException, NoSuchFieldException {
        final String name = uniqueId();
        final Long lastModified = 1L;

        contentCreator.init(U.createImportOptions(true, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        Node contentNode = parentNode.addNode(name, "nt:file").addNode("jcr:content", "nt:resource");
        //Below will check that these values will be overwritten
        contentNode.setProperty("jcr:lastModified", lastModified);
        contentNode.setProperty("jcr:mimeType", "foo");

        assertFalse(contentNode.hasProperty("jcr:data"));
        contentCreator.createFileAndResourceNode("prefixWithSlash/" + name, new ByteArrayInputStream(new byte[0]), null, 0L);
        assertTrue(contentNode.hasProperty("jcr:data"));

        final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
        assertEquals(DEFAULT_CONTENT_TYPE, contentNode.getProperty("jcr:mimeType").getString());
        //Checking that passed in method 0-value was not set.
        assertTrue(contentNode.getProperty("jcr:lastModified").getLong() > 0);
        //Checking that property was overwritten after it was set above
        assertNotEquals(lastModified, contentNode.getProperty("jcr:lastModified").getLong(), 0);
    }

    @Test
    public void willNotOverwriteExistingFileAndResourceNode() throws RepositoryException, NoSuchFieldException {
        final String mimeType = "foo";
        final String name = uniqueId();
        final Long lastModified = 32493421L;

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        Node contentNode = parentNode.addNode(name, "nt:file").addNode("jcr:content", "nt:resource");
        //Below will check that these values will be overwritten
        contentNode.setProperty("jcr:lastModified", lastModified);
        contentNode.setProperty("jcr:mimeType", mimeType);

        assertFalse(contentNode.hasProperty("jcr:data"));
        contentCreator.createFileAndResourceNode(name, new ByteArrayInputStream(new byte[0]), null, lastModified - 1);
        assertFalse(contentNode.hasProperty("jcr:data"));

        //Checking that properties were not overwritten after it was set above
        assertEquals(lastModified, contentNode.getProperty("jcr:lastModified").getLong(), 0);
        assertEquals(mimeType, contentNode.getProperty("jcr:mimeType").getString());

        //Required property to save session
        contentNode.setProperty("jcr:data", new BinaryValue(new ByteArrayInputStream(new byte[0])));
    }

    //------DefaultContentCreator#createGroup(String, String[], Map<String, Object>)------//

    @Test
    public void createGroupWithMembersAndExtraOptions() throws RepositoryException {
        final String groupName = uniqueId();
        final UserManager userManager = AccessControlUtil.getUserManager(session);

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        final String[] groupMembers = {uniqueId(), uniqueId(), uniqueId()};
        for(String groupMember: groupMembers){
            userManager.createUser(groupMember, groupMember);
        }

        final Map<String, Object> extraProperties = new HashMap<String, Object>();
        for(int i = 0; i < 3; i++){
            extraProperties.put(uniqueId(), uniqueId());
        }

        assertNull(userManager.getAuthorizable(groupName));
        contentCreator.createGroup(groupName, groupMembers, extraProperties);
        Group group = (Group) userManager.getAuthorizable(groupName);
        assertNotNull(group);

        List<String> groupMembersList = Arrays.asList(groupMembers);
        Iterator<Authorizable> members = group.getMembers();
        while(members.hasNext()){
            assertTrue(groupMembersList.contains(members.next().getID()));
        }

        for(String propertyName: extraProperties.keySet()) {
            assertTrue(group.hasProperty(propertyName));
        }
    }

    @Test
    public void fillGroupWithExtraOptions() throws RepositoryException {
        final String groupName = uniqueId();
        final UserManager userManager = AccessControlUtil.getUserManager(session);

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        final Map<String, Object> extraProperties = new HashMap<String, Object>();
        for(int i = 0; i < 3; i++){
            extraProperties.put(uniqueId(), uniqueId());
        }

        Group createdGroup = userManager.createGroup(groupName);
        assertNotNull(createdGroup);
        for(String optionName: extraProperties.keySet()){
            assertFalse(createdGroup.hasProperty(optionName));
        }

        contentCreator.createGroup(groupName, null, extraProperties);

        createdGroup = (Group) userManager.getAuthorizable(groupName);
        assertNotNull(createdGroup);
        for(String propertyName: extraProperties.keySet()) {
            assertTrue(createdGroup.hasProperty(propertyName));
        }
    }

    @Test
    public void createGroupWithExistingName() throws RepositoryException {
        final String groupName = uniqueId();

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        final UserManager userManager = AccessControlUtil.getUserManager(session);
        //Create a user with name of new group
        userManager.createUser(groupName, groupName);

        thrown.expect(RepositoryException.class);
        thrown.expectMessage("A user already exists with the requested name: " + groupName);
        contentCreator.createGroup(groupName, null, null);
    }

    //------DefaultContentCreator#createProperty(String, String[])------//

    @Test
    public void willNotOverwritePropertyValues() throws RepositoryException {
        final String propertyName = uniqueId();
        final String propertyValue = uniqueId();

        parentNode.setProperty(propertyName, propertyValue);
        session.save();
        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        contentCreator.createProperty(propertyName, new String[]{uniqueId(), uniqueId()});
        //Checking that property was not overwritten
        assertEquals(propertyValue, parentNode.getProperty(propertyName).getString());
    }

    @Test
    public void deleteProperty() throws RepositoryException {
        final String propertyName = uniqueId();
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);


        this.mockery.checking(new Expectations(){{
            oneOf(listener).onDelete(with(any(String.class)));
        }});

        parentNode.setProperty(propertyName, propertyName);
        contentCreator.init(U.createImportOptions(false, false, true, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, null);

        assertTrue(parentNode.hasProperty(propertyName));
        contentCreator.createProperty(propertyName, null);
        assertFalse(parentNode.hasProperty(propertyName));

        mockery.assertIsSatisfied();
    }

    @Test
    public void createProperty() throws RepositoryException {
        final String propertyName = uniqueId();
        final String[] propertyValues = {uniqueId(), uniqueId(), uniqueId()};
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);

        this.mockery.checking(new Expectations(){{
            oneOf(listener).onModify(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(false, false, true, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, null);

        contentCreator.createProperty(propertyName, propertyValues);
        assertTrue(parentNode.hasProperty(propertyName));

        Value[] v = parentNode.getProperty(propertyName).getValues();
        Map<String, Value> values = new HashMap<String, Value>();
        for (Value val : v) {
            values.put(val.getString(), val);
        }

        for(String value: propertyValues){
            assertTrue(values.containsKey(value));
        }

        mockery.assertIsSatisfied();
    }

    //------DefaultContentCreator#createUser(String, String, Map<String, Object>)------//

    @Test
    public void createNewUser() throws RepositoryException {
        final String userName = uniqueId();
        final UserManager userManager = AccessControlUtil.getUserManager(session);

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        assertNull(userManager.getAuthorizable(userName));
        contentCreator.createUser(userName, userName, null);
        assertNotNull(userManager.getAuthorizable(userName));
    }

    @Test
    public void fillExistingUserExtraOptions() throws RepositoryException {
        final String userName = uniqueId();
        final UserManager userManager = AccessControlUtil.getUserManager(session);

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);
        User createdUser = userManager.createUser(userName, userName);

        final Map<String, Object> extraProperties = new HashMap<String, Object>();
        for(int i = 0; i < 3; i++){
            extraProperties.put(uniqueId(), uniqueId());
        }

        for(String prop: extraProperties.keySet()){
            assertFalse(createdUser.hasProperty(prop));
        }

        contentCreator.createUser(userName, userName, extraProperties);
        createdUser = (User) userManager.getAuthorizable(userName);
        assertNotNull(createdUser);
        for(String prop: extraProperties.keySet()){
            assertTrue(createdUser.hasProperty(prop));
        }
    }

    @Test
    public void createNewUserWithSameNameAsExistingGroup() throws RepositoryException {
        final String userName = uniqueId();
        final UserManager userManager = AccessControlUtil.getUserManager(session);

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);
        userManager.createGroup(userName);

        thrown.expect(RepositoryException.class);
        thrown.expectMessage("A group already exists with the requested name: " + userName);
        contentCreator.createUser(userName, userName, null);
    }

    //------DefaultContentCreator#switchCurrentNode(String, String)------//

    @Test
    public void switchParentNodeToANewOne() throws RepositoryException, NoSuchFieldException {
        final String[] nodeNames = {uniqueId(), uniqueId()};
        final StringBuilder pathBuilder = new StringBuilder();
        for(String newNodeName: nodeNames){
            pathBuilder.append("/").append(newNodeName);
        }
        final String subNodes = pathBuilder.toString();
        final String newNodeType = "nt:unstructured";
        final Stack<Node> parentNodeStack = (Stack<Node>) PrivateAccessor.getField(contentCreator, "parentNodeStack");
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);

        this.mockery.checking(new Expectations(){{
            exactly(nodeNames.length).of(listener).onCreate(with(any(String.class)));
        }});

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, null);

        boolean result = contentCreator.switchCurrentNode(subNodes, newNodeType);
        assertTrue(result);
        //Checking that nodes for all names were created
        Node current = parentNode;
        for(String nodeName: nodeNames){
            current = current.getNode(nodeName);
            assertNotNull(current);
            assertTrue(current.getPrimaryNodeType().isNodeType(newNodeType));
        }

        //Checking that last node is on top of the stack
        assertEquals(current.getPath(), parentNodeStack.peek().getPath());
        mockery.assertIsSatisfied();
    }

    @Test
    public void switchNodeWithFirstExisting() throws NoSuchFieldException, RepositoryException {
        final String[] nodeNames = {uniqueId(), uniqueId()};
        final StringBuilder pathBuilder = new StringBuilder();
        for(String newNodeName: nodeNames){
            pathBuilder.append("/").append(newNodeName);
        }
        final String subNodes = pathBuilder.toString();
        final String newNodeType = "nt:folder";
        final String existingNodeType = "nt:unstructured";
        final Stack<Node> parentNodeStack = (Stack<Node>) PrivateAccessor.getField(contentCreator, "parentNodeStack");

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        parentNode.addNode(nodeNames[0], existingNodeType);
        boolean result = contentCreator.switchCurrentNode(subNodes, newNodeType);
        assertTrue(result);

        Node current = parentNode.getNode(nodeNames[0]);
        assertNotNull(current);
        //Checking that first node has saved old nodetype
        assertTrue(current.getPrimaryNodeType().isNodeType(existingNodeType));
        for(int i = 1; i < nodeNames.length; i++){
            current = current.getNode(nodeNames[i]);
            assertNotNull(current);
            assertTrue(current.getPrimaryNodeType().isNodeType(newNodeType));
        }

        //Checking that last node is on top of the stack
        assertEquals(current.getPath(), parentNodeStack.peek().getPath());
    }

    @Test
    public void switchNodeWithNullNodeType() throws RepositoryException, NoSuchFieldException {
        final String[] nodeNames = {uniqueId(), uniqueId()};
        final StringBuilder pathBuilder = new StringBuilder();
        for(String newNodeName: nodeNames){
            pathBuilder.append("/").append(newNodeName);
        }
        final String subNodes = pathBuilder.toString();

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        boolean result = contentCreator.switchCurrentNode(subNodes, null);
        assertFalse(result);
        assertFalse(parentNode.hasNodes());
    }

    //------DefaultContentCreator#createAce(String, String[], String[], String)------//

    @Test
    public void createAccessControlEntryForNonExistingPrincipal() throws RepositoryException {
        final String newPrincipalId = uniqueId();
        final String[] grantedPrivilegesNames = {Privilege.JCR_READ};
        final String[] deniedPrivilegesNames = {Privilege.JCR_WRITE};

        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
        assertNull(principalManager.getPrincipal(newPrincipalId));

        thrown.expect(RepositoryException.class);
        thrown.expectMessage("No principal found for id: " + newPrincipalId);
        contentCreator.createAce(newPrincipalId, grantedPrivilegesNames, deniedPrivilegesNames, null);
    }

    private final String uniqueId() {
        return getClass().getSimpleName() + UUID.randomUUID();
    }
}
