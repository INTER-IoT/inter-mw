/*
 * Copyright 2016-2018 Universitat Politècnica de València
 * Copyright 2016-2018 Università della Calabria
 * Copyright 2016-2018 Prodevelop, SL
 * Copyright 2016-2018 Technische Universiteit Eindhoven
 * Copyright 2016-2018 Fundación de la Comunidad Valenciana para la
 * Investigación, Promoción y Estudios Comerciales de Valenciaport
 * Copyright 2016-2018 Rinicom Ltd
 * Copyright 2016-2018 Association pour le développement de la formation
 * professionnelle dans le transport
 * Copyright 2016-2018 Noatum Ports Valenciana, S.A.U.
 * Copyright 2016-2018 XLAB razvoj programske opreme in svetovanje d.o.o.
 * Copyright 2016-2018 Systems Research Institute Polish Academy of Sciences
 * Copyright 2016-2018 Azienda Sanitaria Locale TO5
 * Copyright 2016-2018 Alessandro Bassi Consulting SARL
 * Copyright 2016-2018 Neways Technologies B.V.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.interiot.intermw.services.registry;

import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.commons.requests.RegisterPlatformReq;
import eu.interiot.message.Message;
import eu.interiot.message.exceptions.MessageException;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * @author Gasper Vrhovsek
 */
public class ParliamentRDFIntegrationTest {

    private static final String PREFIXES = "PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>\n" +
            "PREFIX fn: <http://www.w3.org/2005/xpath-functions#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX par: <http://parliament.semwebcentral.org/parliament#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX time: <http://www.w3.org/2006/time#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX iiotex: <http://inter-iot.eu/GOIoTPex#>\n" +
            "PREFIX geosparql: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX iiot: <http://inter-iot.eu/GOIoTP#>\n" +
            "PREFIX InterIoT: <http://inter-iot.eu/>\n" +
            "PREFIX ssn: <http://www.w3.org/ns/ssn/>\n" +
            "PREFIX sosa: <http://www.w3.org/ns/sosa/>\n";

    private static final String PLATFORM_GRAPH = "http://inter-iot.eu/platform";
    private static final String DEVICE_GRAPH = "http://inter-iot.eu/device";

    private RDFConnection connect() {
        return RDFConnectionFactory.connect("http://localhost:8089/parliament", "sparql", "sparql", "sparql");
    }

    @Test
    public void registerPlatformAndDevices() throws IOException, MessageException {
        try (RDFConnection connect = connect()) {
            // register platform
            Message msgPlatformRegister = getPlatformRegisterMessage();
            String registerPlatformInsert = buildSparqlInsert(PLATFORM_GRAPH, msgPlatformRegister);
            connect.update(registerPlatformInsert);

            // device discovery
            Message deviceDiscovery = getDeviceRegistryInitializeMessage();
            String deviceDiscoveryInsert = buildSparqlInsert(DEVICE_GRAPH, deviceDiscovery);
            connect.update(deviceDiscoveryInsert);
        }
    }

    @Test
    public void constructQueryDevices() {
        try (RDFConnection conn = connect()) {
            String construct = buildSparqlSimpleDeviceConstruct();

            Model model = conn.queryConstruct(construct);
            RDFDataMgr.write(System.out, model, Lang.JSONLD);
        }
    }

    @Test
    public void selectQueryDevices() throws IOException, MessageException {
        try (RDFConnection conn = connect()) {
            String select = buildSparqlDeviceSelect();

            QueryExecution queryExecution = conn.query(select);
            ResultSet resultSet = queryExecution.execSelect();

            while (resultSet.hasNext()) {
                QuerySolution next = resultSet.next();

//                System.out.println("===========================");
//                RDFNode subject = next.get("s");
//                RDFNode predicate = next.get("p");
//                RDFNode object = next.get("o");
//
//                System.out.println(subject.toString() + " isAnon = " + subject.isAnon());
//                System.out.println(predicate.toString());
//                System.out.println(object.toString() + " isAnon = " + object.isAnon());
//                System.out.println("===========================\n");
            }
        }
    }

    private String buildSparqlDeviceSelect() {
        StringBuilder sb = new StringBuilder(PREFIXES);

        sb.append("SELECT ?s ?p ?o \n")
                .append("WHERE {\n")
                .append("GRAPH <").append(DEVICE_GRAPH).append("> {\n")
                .append("?s ?p ?o")
                .append("}")
                .append("}");

        return sb.toString();
    }

    private String buildSparqlSimpleDeviceConstruct() {
        StringBuilder sb = new StringBuilder(PREFIXES);

        sb.append("CONSTRUCT { ?s ?p ?o } \n")
                .append("WHERE {\n")
                .append("GRAPH <").append(DEVICE_GRAPH).append("> {\n")
                .append("?deviceId rdf:type ?type .")
                .append("?s ?p ?o .")
                .append("?s sosa:observes ?o.")
                .append("}")
                .append("}");

        return sb.toString();
    }

    private String buildSparqlInsert(String graph, Message message) {
        StringWriter sw = new StringWriter();

        Map<String, String> nsPrefixMap = message.getPrefixes();

        for (Map.Entry<String, String> entry : nsPrefixMap.entrySet()) {
            if (!entry.getKey().isEmpty()) {
                sw.append("PREFIX ").append(entry.getKey()).append(": <").append(entry.getValue()).append(">").append("\n");
            }
        }
        sw.append("INSERT DATA\n").append("{\n").append("\tGRAPH <").append(graph).append("> {\n");
        RDFDataMgr.write(sw, message.getPayload().getJenaModel(), Lang.TTL);
        sw.append("\t}\n}");
        return sw.toString();
    }

    private Message getPlatformRegisterMessage() throws MalformedURLException {
        Platform platform = new Platform();

        platform.setBaseEndpoint(new URL("http://inter-iot.eu"));
        platform.setClientId("http://inter-iot.eu/client/myclient");
        platform.setEncryptedPassword("someEncryptedPassword");
        platform.setEncryptionAlgorithm("encryptionAlgorithm");
        platform.setLocationId("http://inter-iot.eu/location/platform1location");
        platform.setName("testPlatform");
        platform.setPlatformId("http://www.inter-iot.eu/wso2port");
        platform.setType("http://inter-iot.eu/platformtype/amazingPlatform");
        platform.setUsername("myclient");

        RegisterPlatformReq registerPlatformReq = new RegisterPlatformReq(platform);
        return registerPlatformReq.toMessage();
    }

    private Message getDeviceRegistryInitializeMessage() throws IOException, MessageException {
        ClassLoader classLoader = ParliamentRDFIntegrationTest.class.getClassLoader();
        String deviceInitJson = IOUtils.toString(classLoader.getResource("device_init_response.json"), "UTF-8");
        Message msgDeviceInit = new Message(deviceInitJson);
        return msgDeviceInit;
    }


}
