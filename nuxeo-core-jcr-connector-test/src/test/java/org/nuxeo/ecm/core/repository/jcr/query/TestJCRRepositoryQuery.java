/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.repository.jcr.query;

import org.nuxeo.ecm.core.query.test.QueryTestCase;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;

/**
 * @author Florent Guillaume
 */
public class TestJCRRepositoryQuery extends QueryTestCase {

    @Override
    public void deployRepository() throws Exception {
        deployContrib(CoreJCRConnectorTestConstants.TESTS_BUNDLE,
                "query-repository-contrib.xml");
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "CustomVersioningService.xml");
        deployBundle("org.nuxeo.ecm.core.event");
    }

    @Override
    public void undeployRepository() throws Exception {
    }

    @Override
    public void testQueryNegativeMultiple() {
        // JCR cannot do negative queries on multi-valued properties
    }

    @Override
    public void testQueryWithProxies() {
        // JCR cannot do queries on proxies or versions
    }

    @Override
    public void testSQLFulltextBlob() {
        // TODO blob indexing configuration
    }

    @Override
    public void testBatching() throws Exception {
        // XXX JCR doesn't seem to order by jcr:name correctly...
        // XXX also total size computation is not done yet
    }

    @Override
    public void testOrderByPath() throws Exception {
        // XXX JCR doesn't allow order by ecm:path yet
    }
}
