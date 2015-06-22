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

import org.apache.sling.jcr.contentloader.internal.*;
import org.apache.sling.paxexam.util.SlingPaxOptions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.CompositeOption;

import javax.inject.Inject;

import static org.junit.Assert.*;

@RunWith(PaxExam.class)
public class BundleContentLoaderIT {

    @Inject
    ContentReaderWhiteboard whiteboard;

/*
    //ClassNotFoundException as well
    @Inject
    BundleHelper bundleHelper;

    @Inject
    BundleContentLoader contentLoader;

    @Inject
    BaseImportLoader contentLoader;

    @Inject
    JcrXmlImporter contentLoader;
*/

    @Test
    public void dummyTest(){
        CompositeOption test = SlingPaxOptions.slingBundleList("org.apache.sling", "org.apache.sling.jcr.contentloader", "23-SNAPSHOT", "", "");
        assertNotNull(whiteboard);
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return U.paxConfig();
    }
}
