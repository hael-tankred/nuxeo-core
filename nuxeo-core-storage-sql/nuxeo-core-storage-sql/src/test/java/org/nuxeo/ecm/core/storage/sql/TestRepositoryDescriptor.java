/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FieldDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FulltextIndexDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;

public class TestRepositoryDescriptor {

    protected RepositoryDescriptor desc;

    protected static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(
                resource);
    }

    @Before
    public void setUp() throws Exception {
        XMap xmap = new XMap();
        xmap.register(RepositoryDescriptor.class);
        xmap.register(FulltextIndexDescriptor.class);
        desc = (RepositoryDescriptor) xmap.load(getResource("test-repository-descriptor.xml"));
    }

    @Test
    public void testBasic() throws Exception {
        assertEquals("foo", desc.name);
        assertTrue(desc.clusteringEnabled);
        assertEquals(1234, desc.clusteringDelay);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFulltext() throws Exception {
        assertEquals("french", desc.fulltextAnalyzer);
        assertEquals("nuxeo", desc.fulltextCatalog);
        assertNotNull(desc.fulltextIndexes);
        assertEquals(4, desc.fulltextIndexes.size());
        FulltextIndexDescriptor fti;

        fti = desc.fulltextIndexes.get(0);
        assertNull(fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet(), fti.fields);
        assertEquals(new HashSet(Collections.singleton("dc:creator")),
                fti.excludeFields);

        fti = desc.fulltextIndexes.get(1);
        assertEquals("titraille", fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet(Arrays.asList("dc:title", "dc:description")),
                fti.fields);
        assertEquals(new HashSet(), fti.excludeFields);

        fti = desc.fulltextIndexes.get(2);
        assertEquals("blobs", fti.name);
        assertEquals("blob", fti.fieldType);
        assertEquals(new HashSet(), fti.fields);
        assertEquals(new HashSet(Collections.singleton("foo:bar")),
                fti.excludeFields);

        fti = desc.fulltextIndexes.get(3);
        assertEquals("pictures", fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet(
                Collections.singleton("picture:views/*/filename")), fti.fields);
        assertEquals(new HashSet(), fti.excludeFields);
        assertEquals("martian", fti.analyzer);
        assertEquals("other", fti.catalog);

        assertTrue(desc.fulltextExcludedTypes.contains("Folder"));
        assertTrue(desc.fulltextExcludedTypes.contains("Workspace"));
        assertTrue(desc.fulltextIncludedTypes.contains("File"));
        assertTrue(desc.fulltextIncludedTypes.contains("Note"));
    }

    @Test
    public void testSchemaFields() throws Exception {
        assertNotNull(desc.schemaFields);
        assertEquals(3, desc.schemaFields.size());
        FieldDescriptor fd;
        fd = desc.schemaFields.get(0);
        assertEquals("my:bignote", fd.field);
        assertEquals("biig", fd.type);
        assertNull(fd.table);
        assertNull(fd.column);
        fd = desc.schemaFields.get(1);
        assertEquals("foo", fd.field);
        assertEquals("xyz", fd.type);
        assertNull(fd.table);
        assertNull(fd.column);
        fd = desc.schemaFields.get(2);
        assertEquals("bar", fd.field);
        assertEquals("bartype", fd.type);
        assertEquals("bartable", fd.table);
        assertEquals("barcol", fd.column);
    }

    @Test
    public void testBinaryStorePath() throws Exception {
        assertEquals("/foo/bar", desc.binaryStorePath);
    }

    @Test
    public void testListenConnect() throws Exception {
        ServerDescriptor s;
        s = desc.listen;
        assertEquals("localhost0", s.host);
        assertEquals(81810, s.port);
        assertEquals("/nuxeo0", s.path);

        assertNotNull(desc.connect);
        assertEquals(2, desc.connect.size());
        s = desc.connect.get(0);
        assertEquals("localhost1", s.host);
        assertEquals(81811, s.port);
        assertEquals("/nuxeo1", s.path);
        s = desc.connect.get(1);
        assertEquals("localhost2", s.host);
        assertEquals(81812, s.port);
        assertEquals("/nuxeo2", s.path);
    }

}
