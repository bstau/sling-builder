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
package org.apache.sling.jcr.contentloader.internal.it;

import org.apache.sling.jcr.contentloader.it.P;
import org.junit.*;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jcr.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(PaxExam.class)
public abstract class AbstractContentLoaderIT {
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    protected static final int RETRY_TIMEOUT = 5000;
    protected static final int RETRY_INTERVAL = 100;
    protected static final String DEFAULT_PATH_IN_BUNDLE = "initial-content";
    protected static final String SLING_INITIAL_CONTENT_HEADER = "Sling-Initial-Content";

    private static final Map<String, Bundle> installedBundles = new HashMap<String, Bundle>();

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected SlingRepository repository;

    protected Bundle installBundle(String symbolicName, InputStream bundleAsStream) throws BundleException {
        Bundle b = null;
        if(symbolicName != null && !installedBundles.containsKey(symbolicName)) {
            b = bundleContext.installBundle(symbolicName, bundleAsStream);
            installedBundles.put(symbolicName, b);
            b.start();
        }
        return b;
    }

    protected Bundle uninstallBundle(String symbolicName) throws BundleException {
        Bundle bundleToReturn = null;
        for(Bundle b: bundleContext.getBundles()){
            if(b.getSymbolicName().equals(symbolicName)){
                bundleToReturn = b;
                bundleToReturn.uninstall();
            }
        }
        return bundleToReturn;
    }

    protected InputStream createBundleStream(String symbolicName, String contentPath, String props) throws IOException {
        InputStream is = null;
        if(symbolicName != null && !installedBundles.containsKey(symbolicName)) {
            TinyBundle b = TinyBundles.bundle()
                    .set(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
            if(contentPath != null) {
                b = b.set(SLING_INITIAL_CONTENT_HEADER, DEFAULT_PATH_IN_BUNDLE + (props == null ? "" : props));
                addContent(b, contentPath);
                is = b.build(TinyBundles.withBnd());
            }
        }
        return is;
    }

    private void addContent(TinyBundle b, String resourcePath) throws IOException {
        final InputStream is = getClass().getResourceAsStream("/initial-content/" + resourcePath);

        //We store different files in different directories to save them same name.
        int i = resourcePath.lastIndexOf("/");
        String fileName = i < 0 ? "/"+resourcePath : resourcePath.substring(i);
        String pathInBundle = DEFAULT_PATH_IN_BUNDLE + fileName;

        try {
            assertNotNull("Expecting resource to be found:" + resourcePath, is);
            LOGGER.info("Adding resource to bundle, resource={}", pathInBundle, resourcePath);
            b.add(pathInBundle, is);
        } finally {
            if(is != null) {
                is.close();
            }
        }
    }

    protected void printNode(Node n) throws RepositoryException {
        NodeIterator i = n.getNodes();
        while (i.hasNext()){
            printNode(i.nextNode());
        }
        LOGGER.info("POP: {}", n.getPath());
    }

    protected void printProps(Node n) throws RepositoryException {
        PropertyIterator i = n.getProperties();
        while (i.hasNext()) {
            Property p = i.nextProperty();
            LOGGER.info("POOP: {} -> {}", p.getName(), (p.isMultiple() ? "[...]" : p.getString()));
        }
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return P.paxConfig();
    }
}
