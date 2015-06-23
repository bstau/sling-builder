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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleException;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.junit.Assert.*;

@Ignore
@RunWith(PaxExam.class)
public class BundleContentLoaderIT {
    private static final String BUNDLE_PATH = "/Users/Petr/Development/sling/bundles/jcr/contentloader/src/test/resources/org.apache.sling.installer.it-3.6.7-SNAPSHOT-testbundle-1.0.jar";

    @Inject
    private BundleContext bundleContext;

    @Inject
    private SlingRepository repository;

    @Test
    public void dummyTest() throws BundleException, RepositoryException {
        Session s = repository.loginAdministrative(null);
        bundleContext.installBundle("file:" + BUNDLE_PATH);
        assertTrue("Ndode doesn't created", s.itemExists("/home"));
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return U.paxConfig();
    }
}
