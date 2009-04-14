/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 */
public class TestBlobsExtractor extends NXRuntimeTestCase {

    public TestBlobsExtractor(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core.api.tests",
                "OSGI-INF/test-blobsextractor-types-contrib.xml");
    }

    public void test() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ComplexDoc");

        List<Map<String, Object>> vignettes = new ArrayList<Map<String, Object>>();
        Map<String, Object> vignette = new HashMap<String, Object>();
        vignette.put("width", Long.valueOf(0));
        vignette.put("height", Long.valueOf(0));
        Blob blob1 = new ByteArrayBlob("foo1 bar1".getBytes("UTF-8"),
                "text/plain");
        blob1.setFilename("file1.txt");
        vignette.put("content", blob1);
        vignettes.add(vignette);

        vignette = new HashMap<String, Object>();
        vignette.put("width", Long.valueOf(0));
        vignette.put("height", Long.valueOf(0));
        Blob blob2 = new ByteArrayBlob("foo2 bar2".getBytes("UTF-8"),
                "text/plain");
        blob2.setFilename("file2.txt");
        vignette.put("content", blob2);
        vignettes.add(vignette);

        Map<String, Object> attachedFile = new HashMap<String, Object>();
        attachedFile.put("name", "some name");
        attachedFile.put("vignettes", vignettes);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);

        BlobsExtractor extractor = new BlobsExtractor();
        List<Blob> blobs = extractor.getBlobs(doc);
        assertEquals(2, blobs.size());
        assertTrue(blobs.contains(blob1));
        assertTrue(blobs.contains(blob2));
    }

}
