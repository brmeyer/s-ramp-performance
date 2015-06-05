/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.overlord.sramp.performance;

import org.apache.commons.io.IOUtils;
import org.artificer.atom.archive.ArtificerArchive;
import org.artificer.common.ArtifactType;
import org.artificer.server.core.api.ArtifactService;
import org.artificer.server.core.api.BatchService;
import org.artificer.server.core.api.QueryService;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.BaseArtifactEnum;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.BaseArtifactType;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.XsdDocument;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.List;
import java.util.Properties;

public class BasicPerformanceTest {

    public static void main(String[] args) {
        try {
            Properties jndiProps = new Properties();
            jndiProps.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
            jndiProps.put(Context.PROVIDER_URL, "http-remoting://localhost:8080");
            jndiProps.put(Context.SECURITY_PRINCIPAL, "admin");
            jndiProps.put(Context.SECURITY_CREDENTIALS, "artificer1!");
            jndiProps.put("jboss.naming.client.ejb.context", true);
            Context context = new InitialContext(jndiProps);

            final ArtifactService artifactService = (ArtifactService) context.lookup(
                    "artificer-server/ArtifactService!" + ArtifactService.class.getName());
            artifactService.login("artificer", "artificer1!");

            final BatchService batchService = (BatchService) context.lookup(
                    "artificer-server/BatchService!" + BatchService.class.getName());
            batchService.login("artificer", "artificer1!");

            final QueryService queryService = (QueryService) context.lookup(
                    "artificer-server/QueryService!" + QueryService.class.getName());
            queryService.login("artificer", "artificer1!");

            xsdTest(artifactService);
            xsdBatchTest(batchService);
            queryTest(artifactService, queryService);
            fullTextSearchTest(queryService);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void xsdTest(ArtifactService artifactService) throws Exception {
        long start = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            if (i % 100 == 0) {
                System.out.println("i: " + i);
            }

            XsdDocument xsdDocument = new XsdDocument();
            xsdDocument.setName("PO" + i);
            xsdDocument.setArtifactType(BaseArtifactEnum.XSD_DOCUMENT);
            artifactService.upload("PO" + i + ".xsd",
                    IOUtils.toByteArray(BasicPerformanceTest.class.getResourceAsStream("/PO.xsd")));
        }
        long end = System.currentTimeMillis();
        System.out.printf("Uploads completed in %dms%n", end - start);
    }

    private static void xsdBatchTest(BatchService batchService) throws Exception {
        long start = System.currentTimeMillis();
        ArtificerArchive archive = new ArtificerArchive();

        for (int i = 0; i < 1000; i++) {
            if (i % 100 == 0) {
                System.out.println("i: " + i);
                if (i > 0) {
                    batchService.upload(archive);
                }
                archive = new ArtificerArchive();
            }

            XsdDocument xsdDocument = new XsdDocument();
            xsdDocument.setName("PO" + i);
            xsdDocument.setArtifactType(BaseArtifactEnum.XSD_DOCUMENT);
            archive.addEntry("PO" + i, xsdDocument, BasicPerformanceTest.class.getResourceAsStream("/PO.xsd"));
        }
        long end = System.currentTimeMillis();
        System.out.printf("Batch uploads completed in %dms%n", end - start);
    }

    private static void queryTest(ArtifactService artifactService, QueryService queryService) throws Exception {
        long start = System.currentTimeMillis();
        List<BaseArtifactType> artifactResults = queryService.query("/s-ramp/xsd/XsdDocument");
        BaseArtifactType artifact = artifactResults.get(0);
        long end = System.currentTimeMillis();
        System.out.printf("Find all completed in %dms%n", end - start);

        start = System.currentTimeMillis();
        artifactResults = queryService.query("/s-ramp/xsd/ComplexTypeDeclaration/relatedDocument");
        end = System.currentTimeMillis();
        System.out.printf("Find all through relationship completed in %dms%n", end - start);

        start = System.currentTimeMillis();
        artifactResults = queryService.query("/s-ramp/xsd/XsdDocument[@uuid='" + artifact.getUuid() + "']");
        end = System.currentTimeMillis();
        System.out.printf("Query by UUID completed in %dms%n", end - start);

        start = System.currentTimeMillis();
        artifact = artifactService.getMetaData(ArtifactType.valueOf(artifact), artifact.getUuid());
        end = System.currentTimeMillis();
        System.out.printf("Get metadata by UUID completed in %dms%n", end - start);

//        for (int i = 0; i < 1000; i++) {
//            if (i % 100 == 0) {
//                System.out.println("i: " + i);
//            }
//
//            List<BaseArtifactType> artifactResults = queryService.query("/s-ramp/xsd/XsdDocument");
////            System.out.println("XsdDocument: " + artifactResults.size());
//            String uuid = artifactResults.get(0).getUuid();
//            artifactResults = queryService.query("/s-ramp/xsd/XsdDocument[@uuid='" + uuid + "']");
////            System.out.println("XsdDocument w/ UUID: " + artifactResults.size());
//            artifactResults = queryService.query("/s-ramp/xsd[relatedDocument[@uuid='" + uuid + "']]");
////            System.out.println("derived: " + artifactResults.size());
//            artifactResults = queryService.query("/s-ramp/xsd/relatedDocument");
//        }
    }

    private static void fullTextSearchTest(QueryService queryService) throws Exception {
        long start = System.currentTimeMillis();
        List<BaseArtifactType> artifactResults = queryService.query("/s-ramp[xp2:matches(., 'Purchase order schema')]");
        long end = System.currentTimeMillis();
        System.out.printf("Full text search completed in %dms%n", end - start);

//        for (int i = 0; i < 1000; i++) {
//            if (i % 100 == 0) {
//                System.out.println("i: " + i);
//            }
//
//            List<BaseArtifactType> artifactResults = queryService.query("/s-ramp[xp2:matches(., 'Purchase order schema')]");
////            System.out.println("XsdDocument: " + artifactResults.size());
//        }
    }
}
