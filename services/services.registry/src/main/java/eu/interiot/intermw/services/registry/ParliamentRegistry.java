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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.*;
import eu.interiot.intermw.commons.model.enums.IoTDeviceType;
import eu.interiot.intermw.commons.model.enums.QueryType;
import eu.interiot.intermw.commons.model.extractors.IoTDeviceExtractor;
import eu.interiot.intermw.commons.responses.DeviceRegistryInitializeRes;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.payload.types.IoTDevicePayload;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphExtract;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.TripleBoundary;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class ParliamentRegistry {
    private final static Logger logger = LoggerFactory.getLogger(ParliamentRegistry.class);
    private static final String SUBSCRIPTION_PREFIX = "subscriptions:";

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
            "PREFIX sosa: <http://www.w3.org/ns/sosa/>\n" +
            "PREFIX mdw: <http://interiot.eu/ont/middleware.owl#>\n" +
            "PREFIX clients: <http://interiot.eu/clients#>";

    private static final String CLIENT_PREFIX = "http://inter-iot.eu/clients#";
    private static final String CLIENTS_GRAPH = "http://inter-iot.eu/clients";
    private static final String MIDDLEWARE_NAMESPACE = "http://interiot.eu/ont/middleware.owl#";

    private Configuration conf;

    public ParliamentRegistry(Configuration conf) {
        this.conf = conf;
    }

    public void registerClient(Client client) throws MiddlewareException {
        logger.debug("Registering client {}...", client.getClientId());
        // Insert client id
        // TODO combine both insertions into same template file!
        String clientRefInsert = getRefInsert(CLIENTS_GRAPH, getClientGraphId(client.getClientId()));
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", clientRefInsert);
        }

        // Insert client
        ParameterizedSparqlString pss = getPSSfromTemplate("client-create.rq");
        pss.setIri("?graph", getClientGraphId(client.getClientId()));
        pss.setIri("?clientId", getClientGraphId(client.getClientId()));
        pss.setLiteral("?callbackUrl", client.getCallbackUrl() != null ? client.getCallbackUrl().toString() : "");
        pss.setLiteral("?receivingCapacity", client.getReceivingCapacity() != null ? client.getReceivingCapacity() : 0);
        pss.setLiteral("?responseDelivery", client.getResponseDelivery() != null ? client.getResponseDelivery().name() : "");
        pss.setLiteral("?responseFormat", client.getResponseFormat().name());

        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", pss.toString());
        }

        UpdateRequest updateRequest = pss.asUpdate();
        try (RDFConnection conn = connect()) {
            conn.update(clientRefInsert);
            conn.update(updateRequest);
        }
    }

    public boolean isClientRegistered(String clientId) throws MiddlewareException {
        logger.debug("Checking if client {} exists...", clientId);

        ParameterizedSparqlString pss = getPSSfromTemplate("client-exists.rq");
        pss.setIri("?clientId", getClientGraphId(clientId));
        pss.setIri("?clientGraph", getClientGraphId(clientId));

        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", pss.toString());
        }

        Query query = pss.asQuery();
        try (RDFConnection conn = connect()) {
            QueryExecution queryExecution = conn.query(query);
            return queryExecution.execAsk();
        }
    }

    public Client getClientById(String clientId) throws MiddlewareException {
        logger.debug("Retrieving client {}...", clientId);
        ParameterizedSparqlString pss = getPSSfromTemplate("client-getById-construct.rq");
        pss.setIri("?graph", getClientGraphId(clientId));

        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", pss.toString());
        }

        Client client = new Client();
        client.setClientId(clientId);
        Query query = pss.asQuery();
        try (RDFConnection conn = connect()) {
            Model clientModel = conn.queryConstruct(query);
            StmtIterator stmtIterator = clientModel.listStatements();

            while (stmtIterator.hasNext()) {
                Statement next = stmtIterator.next();
                String predicate = next.getPredicate().getLocalName();
                RDFNode object = next.getObject();

                if (predicate.equals("callbackUrl")) {
                    if (!object.asLiteral().getString().isEmpty()) {
                        client.setCallbackUrl(new URL(object.asLiteral().getString()));
                    }
                } else if (predicate.equals("receivingCapacity")) {
                    client.setReceivingCapacity(object.asLiteral().getInt());
                } else if (predicate.equals("responseDelivery")) {
                    client.setResponseDelivery(Client.ResponseDelivery.valueOf(object.asLiteral().getString()));
                } else if (predicate.equals("responseFormat")) {
                    client.setResponseFormat(Client.ResponseFormat.valueOf(object.asLiteral().getString()));
                }
            }
        } catch (MalformedURLException e) {
            throw new MiddlewareException("Failed to retrieve client.", e);
        }
        return client;
    }

    public List<Client> listClients() throws MiddlewareException {
        List<Client> clients = new ArrayList<>();
        ParameterizedSparqlString pss = getPSSfromTemplate("client-getAll.rq");
        Query query = pss.asQuery();

        String sparql = pss.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        try (RDFConnection conn = connect()) {
            QueryExecution queryExecution = conn.query(query);
            ResultSet resultSet = queryExecution.execSelect();

            while (resultSet.hasNext()) {
                // looping through data!
                // ?clientId ?callbackUrl ?receivingCapacity ?responseDelivery ?responseFormat
                QuerySolution next = resultSet.next();
                RDFNode clientId = next.get("client");
                RDFNode callbackUrl = next.get("callbackUrl");
                RDFNode receivingCapacity = next.get("receivingCapacity");
                RDFNode responseDelivery = next.get("responseDelivery");
                RDFNode responseFormat = next.get("responseFormat");

                Client client = new Client();
                client.setClientId(clientId.toString().split(CLIENTS_GRAPH + "#")[1]);
                if (!callbackUrl.asLiteral().getString().isEmpty()) {
                    client.setCallbackUrl(new URL(callbackUrl.asLiteral().getString()));
                }
                client.setReceivingCapacity(receivingCapacity.asLiteral().getInt());
                client.setResponseDelivery(Client.ResponseDelivery.valueOf(responseDelivery.asLiteral().getString()));
                client.setResponseFormat(Client.ResponseFormat.valueOf(responseFormat.asLiteral().getString()));

                clients.add(client);
            }
        } catch (MalformedURLException e) {
            logger.error("Could not list clients", e);
            throw new MiddlewareException(e);
        }
        return clients;
    }

    public void removeClient(String clientId) throws MiddlewareException {
        logger.debug("Removing client {}...", clientId);

        ParameterizedSparqlString pss = getPSSfromTemplate("client-remove.rq");
        pss.setIri("?clientId", getClientGraphId(clientId));
        pss.setIri("?clientIDGraph", getClientGraphId(clientId));
        UpdateRequest updateRequest = pss.asUpdate();

        String sparql = pss.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        try (RDFConnection conn = connect()) {
            conn.update(updateRequest);
            logger.debug("Client {} has been removed.", clientId);
        }
    }

    public void updateClient(Client client) throws MiddlewareException {
        logger.debug("Registering client {}...", client.getClientId());

        // Insert client
        ParameterizedSparqlString pss = getPSSfromTemplate("client-update.rq");
        pss.setIri("?graph", getClientGraphId(client.getClientId()));
        pss.setIri("?clientId", getClientGraphId(client.getClientId()));
        pss.setLiteral("?callbackUrl", client.getCallbackUrl() != null ? client.getCallbackUrl().toString() : "");
        pss.setLiteral("?receivingCapacity", client.getReceivingCapacity() != null ? client.getReceivingCapacity() : 0);
        pss.setLiteral("?responseDelivery", client.getResponseDelivery() != null ? client.getResponseDelivery().name() : "");
        pss.setLiteral("?responseFormat", client.getResponseFormat().name());

        update(pss);
    }

    public void registerPlatform(Platform platform) throws MiddlewareException {
        logger.debug("Registering platform {}...", platform.getPlatformId());

        ParameterizedSparqlString pss = getPSSfromTemplate("platform-register.rq");
        pss.setIri("?platformGraphId", platform.getPlatformId());
        pss.setIri("?platformId", platform.getPlatformId());
        pss.setLiteral("?name", platform.getName());
        pss.setLiteral("?type", platform.getType());
        pss.setLiteral("?baseEndpoint", platform.getBaseEndpoint().toString());
        pss.setIri("?clientId", CLIENT_PREFIX + platform.getClientId());
        pss.setLiteral("?timeCreated", platform.getTimeCreated());

        pss.setLiteral("?encryptedPassword", platform.getEncryptedPassword() != null ? platform.getEncryptedPassword() : "");
        pss.setLiteral("?encryptionAlgorithm", platform.getEncryptionAlgorithm() != null ? platform.getEncryptionAlgorithm() : "");
        pss.setLiteral("?location", platform.getLocationId() != null ? platform.getLocationId() : "");
        pss.setLiteral("?username", platform.getUsername() != null ? platform.getUsername() : "");

        // alignments
        pss.setLiteral("?downstreamInputAlignmentName", platform.getDownstreamInputAlignmentName());
        pss.setLiteral("?downstreamInputAlignmentVersion", platform.getDownstreamInputAlignmentVersion());
        pss.setLiteral("?downstreamOutputAlignmentName", platform.getDownstreamOutputAlignmentName());
        pss.setLiteral("?downstreamOutputAlignmentVersion", platform.getDownstreamOutputAlignmentVersion());
        pss.setLiteral("?upstreamInputAlignmentName", platform.getUpstreamInputAlignmentName());
        pss.setLiteral("?upstreamInputAlignmentVersion", platform.getUpstreamInputAlignmentVersion());
        pss.setLiteral("?upstreamOutputAlignmentName", platform.getUpstreamOutputAlignmentName());
        pss.setLiteral("?upstreamOutputAlignmentVersion", platform.getUpstreamOutputAlignmentVersion());

        update(pss);
    }

    public List<Platform> listPlatforms() throws MiddlewareException {
        URL url = Resources.getResource("platforms-getAll.rq");
        String selectAllPlatforms;
        try {
            selectAllPlatforms = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new MiddlewareException("Failed to load SPARQL query.", e);
        }

        try (RDFConnection conn = connect()) {
            Model allPlatformsModel = conn.queryConstruct(selectAllPlatforms);

            StmtIterator stmtIterator = allPlatformsModel.listStatements();

            List<String> platformIds = new ArrayList<>();
            while (stmtIterator.hasNext()) {
                Statement statement = stmtIterator.next();
                platformIds.add(statement.getObject().toString());
            }

            List<Platform> platformList = new ArrayList<>();
            if (!platformIds.isEmpty()) {
                String platformsQuery = getPlatformsQuery(platformIds);

                logger.debug("SPARQL construct:\n{}", platformsQuery);
                Model platforms = conn.queryConstruct(platformsQuery);
                try {
                    platformList = extractPlatformList(platforms);
                } catch (MalformedURLException e) {
                    throw new MiddlewareException("Failed to extract platform list from jena model.", e);
                }
            }

            return setPlatformStatistics(platformList);
        }
    }

    public Platform getPlatformById(String platformId) throws MiddlewareException {
        logger.debug("Retrieving platform {}...", platformId);
        ParameterizedSparqlString pss = getPSSfromTemplate("platform-getById.rq");
        pss.setIri("?platformGraphId", platformId);

        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", pss.toString());
        }

        Query query = pss.asQuery();
        try (RDFConnection conn = connect()) {
            Model platformModel = conn.queryConstruct(query);
            List<Platform> platformList = extractPlatformList(platformModel);
            if (platformList.isEmpty()) {
                return null;
            } else if (platformList.size() == 1) {
                platformList = setPlatformStatistics(platformList);
                return platformList.get(0);
            } else {
                throw new MiddlewareException("Multiple platforms found for a single platform ID.");
            }
        } catch (MalformedURLException e) {
            throw new MiddlewareException("Failed to extract platform list from jena model.", e);
        }
    }

    public void removePlatform(String platformId) throws MiddlewareException {
        logger.debug("Removing platform {}...", platformId);

        ParameterizedSparqlString pss = getPSSfromTemplate("platform-remove.rq");
        pss.setIri("?platformId", platformId);
        pss.setIri("?platformGraphId", platformId);

        UpdateRequest updateRequest = pss.asUpdate();

        String sparql = pss.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        try (RDFConnection conn = connect()) {
            conn.update(updateRequest);
            logger.debug("Platform {} has been removed.", platformId);
        }
    }


    public void updatePlatform(Platform platform) throws MiddlewareException {
        logger.debug("Registering platform {}...", platform.getPlatformId());

        ParameterizedSparqlString pss = getPSSfromTemplate("platform-update.rq");
        pss.setIri("?platformGraphId", platform.getPlatformId());
        pss.setIri("?platformId", platform.getPlatformId());
        pss.setLiteral("?name", platform.getName());
        pss.setLiteral("?type", platform.getType());
        pss.setLiteral("?baseEndpoint", platform.getBaseEndpoint().toString());
        pss.setIri("?clientId", CLIENT_PREFIX + platform.getClientId());
        pss.setLiteral("?timeCreated", platform.getTimeCreated());

        if (platform.getLocationId() != null) {
            pss.setLiteral("?location", platform.getLocationId());
        } else {
            pss.setParam("?location", (Node) null);
        }
        if (platform.getUsername() != null) {
            pss.setLiteral("?username", platform.getUsername());
        } else {
            pss.setParam("?username", (Node) null);
        }
        if (platform.getEncryptedPassword() != null) {
            pss.setLiteral("?encryptedPassword", platform.getEncryptedPassword());
        } else {
            pss.setParam("?encryptedPassword", (Node) null);
        }
        if (platform.getEncryptionAlgorithm() != null) {
            pss.setLiteral("?encryptionAlgorithm", platform.getEncryptionAlgorithm());
        } else {
            pss.setParam("?encryptionAlgorithm", (Node) null);
        }

        // Alignments
        pss.setLiteral("?downstreamInputAlignmentName", platform.getDownstreamInputAlignmentName());
        pss.setLiteral("?downstreamInputAlignmentVersion", platform.getDownstreamInputAlignmentVersion());
        pss.setLiteral("?downstreamOutputAlignmentName", platform.getDownstreamOutputAlignmentName());
        pss.setLiteral("?downstreamOutputAlignmentVersion", platform.getDownstreamOutputAlignmentVersion());
        pss.setLiteral("?upstreamInputAlignmentName", platform.getUpstreamInputAlignmentName());
        pss.setLiteral("?upstreamInputAlignmentVersion", platform.getUpstreamInputAlignmentVersion());
        pss.setLiteral("?upstreamOutputAlignmentName", platform.getUpstreamOutputAlignmentName());
        pss.setLiteral("?upstreamOutputAlignmentVersion", platform.getUpstreamOutputAlignmentVersion());

        pss.setLiteral("?downstreamInputAlignmentName", platform.getDownstreamInputAlignmentName());
        pss.setLiteral("?downstreamInputAlignmentVersion", platform.getDownstreamInputAlignmentVersion());
        pss.setLiteral("?downstreamOutputAlignmentName", platform.getDownstreamOutputAlignmentName());
        pss.setLiteral("?downstreamOutputAlignmentVersion", platform.getDownstreamOutputAlignmentVersion());
        pss.setLiteral("?upstreamInputAlignmentName", platform.getUpstreamInputAlignmentName());
        pss.setLiteral("?upstreamInputAlignmentVersion", platform.getUpstreamInputAlignmentVersion());
        pss.setLiteral("?upstreamOutputAlignmentName", platform.getUpstreamOutputAlignmentName());
        pss.setLiteral("?upstreamOutputAlignmentVersion", platform.getUpstreamOutputAlignmentVersion());

        update(pss);
    }

    public void registerDevices(List<IoTDevice> devices) throws MiddlewareException {
        logger.debug("Registering devices...");

        List<UpdateRequest> updateRequests = new ArrayList<>();
        for (IoTDevice ioTDevice : devices) {

            Map<String, String> replacements = new HashMap<>();
            replacements.put("{type}", getDeviceTypeQueryClause(ioTDevice));

            ParameterizedSparqlString pss = getPSSfromTemplate("device-create.rq", replacements);

            pss.setIri("?deviceId", ioTDevice.getDeviceId());
            pss.setLiteral("?name", ioTDevice.getName());
            pss.setIri("?hostedBy", ioTDevice.getHostedBy());
            pss.setIri("?location", ioTDevice.getLocation());

            UpdateRequest updateRequest = pss.asUpdate();
            updateRequests.add(updateRequest);
        }

        try (RDFConnection conn = connect()) {
            logger.trace("SPARQL queries:");
            for (UpdateRequest updateRequest1 : updateRequests) {
                if (logger.isTraceEnabled()) {
                    logger.trace(updateRequest1.getOperations().get(0).toString());
                }
                conn.update(updateRequest1);
            }
        }
    }

    public void updateDevices(List<IoTDevice> devices) throws MiddlewareException {
        logger.debug("Updating devices...");

        List<UpdateRequest> updateRequests = new ArrayList<>();
        for (IoTDevice ioTDevice : devices) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("{type}", getDeviceTypeQueryClause(ioTDevice));
            ParameterizedSparqlString pss = getPSSfromTemplate("device-update.rq", replacements);
            pss.setIri("?deviceId", ioTDevice.getDeviceId());
            pss.setLiteral("?name", ioTDevice.getName());
            pss.setIri("?hostedBy", ioTDevice.getHostedBy());
            pss.setIri("?location", ioTDevice.getLocation());

            UpdateRequest updateRequest = pss.asUpdate();
            updateRequests.add(updateRequest);
        }

        try (RDFConnection conn = connect()) {
            logger.trace("SPARQL queries :");
            for (UpdateRequest updateRequest : updateRequests) {
                if (logger.isTraceEnabled()) {
                    logger.trace(updateRequest.getOperations().get(0).toString());
                }
                conn.update(updateRequest);
            }
        }
    }

    public List<IoTDevice> deviceDiscoveryQuery(IoTDeviceFilter ioTDeviceFilter) throws MiddlewareException {
        Map<String, String> replacementMap = new HashMap<>();

        String conditions = ioTDeviceFilter.buildConditions();

        replacementMap.put("{conditions}", conditions);
        ParameterizedSparqlString pss = getPSSfromTemplate("device-discovery-query.rq", replacementMap);

        Query query = pss.asQuery();

        try (RDFConnection conn = connect()) {
            Model model = conn.queryConstruct(query);
            return IoTDeviceExtractor.fromIoTDevicePayload(new IoTDevicePayload(model));
        }
    }

    public void registerDevices(String platformId, Message message) throws MiddlewareException {
        // validate we have IoTDevices
        IoTDevicePayload ioTDevicePayload = message.getPayloadAsGOIoTPPayload().asIoTDevicePayload();
        Set<EntityID> ioTDevices = ioTDevicePayload.getIoTDevices();

        if (ioTDevices == null || ioTDevices.isEmpty()) {
            logger.warn("Trying to register devices with empty device list, no devices will be registered");
            return;
        }

        try (RDFConnection conn = connect()) {
            String updateString = buildDeviceDiscoverySparqlInsert(platformId, message);
            if (logger.isTraceEnabled()) {
                logger.trace("Register devices insert: " + updateString);
            }
            conn.update(updateString);
        }

    }

    public List<String> getDeviceIds(String platformId) throws MiddlewareException {
        ParameterizedSparqlString pss = getPSSfromTemplate("device-getAllPlatformDeviceIds.rq");

        if (StringUtils.isNotBlank(platformId)) {
            pss.setIri("?platformId", platformId);
        }

        Query query = pss.asQuery();

        String sparql = pss.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        List<String> results = new ArrayList<>();
        try (RDFConnection conn = connect()) {
            QueryExecution queryExecution = conn.query(query);
            ResultSet resultSet = queryExecution.execSelect();

            while (resultSet.hasNext()) {
                QuerySolution next = resultSet.next();

                Resource deviceIdRes = next.getResource("deviceId");
                results.add(deviceIdRes.toString());
            }
        }
        return results;
    }

    public List<IoTDevice> getDevices(List<String> deviceIds) throws MiddlewareException {
        ParameterizedSparqlString pss = getPSSDeviceByIds(deviceIds);

        try (RDFConnection conn = connect()) {
            Model model = conn.queryConstruct(pss.asQuery());


            Message message = new Message();
            message.setPayload(new IoTDevicePayload(model));

            DeviceRegistryInitializeRes res = new DeviceRegistryInitializeRes(message);

            IoTDeviceExtractor.fromIoTDevicePayload(new IoTDevicePayload(model));

            return res.getIoTDevices();
        }
    }

    public List<IoTDevice> getDevicesByType(IoTDeviceType type, String platformId) throws MiddlewareException {
        ParameterizedSparqlString pss = getPSSfromTemplate("deviceId-getByType.rq");
        pss.setIri("?sosaDeviceType", type.getDeviceTypeUri());
        pss.setIri("?platformId", platformId);
        Query sensorIdsQuery = pss.asQuery();


        String sparql = pss.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        List<String> sensorIds = new ArrayList<>();
        try (RDFConnection conn = connect()) {
            QueryExecution queryExecution = conn.query(sensorIdsQuery);
            ResultSet resultSet = queryExecution.execSelect();

            while (resultSet.hasNext()) {
                QuerySolution next = resultSet.next();

                Resource deviceIdRes = next.getResource("deviceId");
                sensorIds.add(deviceIdRes.toString());
            }

            ParameterizedSparqlString pssDeviceByIds = getPSSDeviceByIds(sensorIds);
            Model model = conn.queryConstruct(pssDeviceByIds.asQuery());
            return IoTDeviceExtractor.fromIoTDevicePayload(new IoTDevicePayload(model));
        }
    }

    public void removeDevices(List<String> deviceIds) throws MiddlewareException {
        for (String deviceId : deviceIds) {
            logger.debug("Removing device {}...", deviceId);

            ParameterizedSparqlString pss = getPSSfromTemplate("device-delete.rq");
            pss.setIri("?deviceId", deviceId);
            UpdateRequest updateRequest = pss.asUpdate();

            String sparql = pss.toString();
            if (logger.isTraceEnabled()) {
                logger.trace("SPARQL query:\n{}", sparql);
            }

            try (RDFConnection conn = connect()) {
                conn.update(updateRequest);
                logger.debug("Device {} has been removed successfully.", deviceId);
            }
        }
    }

    protected String getDeviceTypeQueryClause(IoTDevice ioTDevice) {
        StringBuilder sb = new StringBuilder();
        EnumSet<IoTDeviceType> deviceTypes = ioTDevice.getDeviceTypes();
        for (IoTDeviceType deviceType : deviceTypes) {
            sb.append(String.format("rdf:type <%s>;", deviceType.getDeviceTypeUri()));
        }
        return sb.toString();
    }

    public void subscribe(Subscription subscription) throws MiddlewareException {
        logger.debug("Storing subscription {}...", subscription.getConversationId());

        if (subscription.getDeviceIds().isEmpty()) {
            throw new MiddlewareException("A subscription must have at least one device.");
        }

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        URL url = Resources.getResource("subscription-add.rq");
        String template = null;
        try {
            template = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new MiddlewareException("Failed to load SPARQL query.", e);
        }
        pss.setCommandText(template);
        pss.setIri("?conversationId", SUBSCRIPTION_PREFIX + subscription.getConversationId());
        pss.setIri("?clientId", CLIENT_PREFIX + subscription.getClientId());
        pss.setLiteral("deviceId", "{DEVICE_IDS}");

        String sparql = pss.toString();
        List<String> deviceIris = new ArrayList<>();
        for (String deviceId : subscription.getDeviceIds()) {
            deviceIris.add("<" + deviceId + ">");
        }

        sparql = sparql.replace("\"{DEVICE_IDS}\"", String.join(",", deviceIris));
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.add(sparql);

        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        try (RDFConnection conn = connect()) {
            conn.update(updateRequest);
            logger.debug("Subscription {} has been stored successfully.", subscription.getConversationId());
        }
    }

    public Subscription getSubscriptionById(String conversationId) throws MiddlewareException {
        logger.debug("Retrieving subscription with ID {}...", conversationId);
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        URL url = Resources.getResource("subscription-getById.rq");
        String template = null;
        try {
            template = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new MiddlewareException("Failed to load SPARQL query.", e);
        }
        pss.setCommandText(template);
        pss.setIri("?conversationId", SUBSCRIPTION_PREFIX + conversationId);

        String sparql = pss.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        try (RDFConnection conn = connect()) {
            QueryExecution queryExecution = conn.query(sparql);
            ResultSet resultSet = queryExecution.execSelect();

            if (!resultSet.hasNext()) {
                return null;
            } else {
                Subscription subscription = new Subscription();
                subscription.setConversationId(conversationId);
                while (resultSet.hasNext()) {
                    QuerySolution qs = resultSet.next();
                    if (subscription.getClientId() == null) {
                        String clientId = qs.getResource("clientId").getURI().substring(CLIENT_PREFIX.length());
                        subscription.setClientId(clientId);
                    }
                    String deviceId = qs.getResource("deviceId").getURI();
                    subscription.addDeviceId(deviceId);
                }
                return subscription;
            }
        }
    }

    public Subscription findSubscription(String clientId, String conversationId) throws MiddlewareException {
        logger.debug("Retrieving subscription with clientId={}, conversationId={}", clientId, conversationId);
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        URL url = Resources.getResource("subscription-findByClientIdConvId.rq");
        String template = null;
        try {
            template = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new MiddlewareException("Failed to load SPARQL query.", e);
        }
        pss.setCommandText(template);
        pss.setIri("?clientId", CLIENT_PREFIX + clientId);
        pss.setIri("conversationId", SUBSCRIPTION_PREFIX + conversationId);

        String sparql = pss.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        try (RDFConnection conn = connect()) {
            QueryExecution queryExecution = conn.query(sparql);
            ResultSet resultSet = queryExecution.execSelect();

            if (!resultSet.hasNext()) {
                return null;
            } else {
                Subscription subscription = new Subscription();
                subscription.setConversationId(conversationId);
                subscription.setClientId(clientId);
                while (resultSet.hasNext()) {
                    QuerySolution qs = resultSet.next();
                    String deviceId = qs.getResource("deviceId").getURI();
                    subscription.addDeviceId(deviceId);
                }
                return subscription;
            }
        }
    }

    public List<Subscription> listSubcriptions() throws MiddlewareException {
        return listSubcriptions(null);
    }

    public List<Subscription> listSubcriptions(String clientId) throws MiddlewareException {
        logger.debug("Retrieving subscriptions...");
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        URL url = Resources.getResource("subscription-getById.rq");
        String template = null;
        try {
            template = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new MiddlewareException("Failed to load SPARQL query.", e);
        }
        pss.setCommandText(template);
        if (clientId != null) {
            pss.setIri("?clientId", CLIENT_PREFIX + clientId);
        }

        String sparql = pss.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        try (RDFConnection conn = connect()) {
            QueryExecution queryExecution = conn.query(sparql);
            ResultSet resultSet = queryExecution.execSelect();

            Map<String, Subscription> subscriptionsMap = new HashMap<>();
            while (resultSet.hasNext()) {
                QuerySolution qs = resultSet.next();
                String conversationId = qs.getResource("conversationId").getURI().substring(SUBSCRIPTION_PREFIX.length());
                if (clientId == null) {
                    clientId = qs.getResource("clientId").getURI().substring(CLIENT_PREFIX.length());
                }
                String deviceId = qs.getResource("deviceId").getURI();

                Subscription subscription;
                if (!subscriptionsMap.containsKey(conversationId)) {
                    subscription = new Subscription();
                    subscription.setConversationId(conversationId);
                    subscription.setClientId(clientId);
                    subscriptionsMap.put(conversationId, subscription);

                } else {
                    subscription = subscriptionsMap.get(conversationId);
                }

                subscription.addDeviceId(deviceId);
            }

            return new ArrayList<>(subscriptionsMap.values());
        }
    }

    public void addPlat2PlatSubscription(Plat2PlatSubscription subscription) throws MiddlewareException {
        logger.debug("Storing plat2plat subscription {}...", subscription.getConversationId());

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        URL url = Resources.getResource("plat2plat-subs-create.rq");
        String template;
        try {
            template = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new MiddlewareException("Failed to load SPARQL query.", e);
        }
        pss.setCommandText(template);
        pss.setIri("?conversationId", SUBSCRIPTION_PREFIX + subscription.getConversationId());
        pss.setIri("?clientId", CLIENT_PREFIX + subscription.getClientId());
        pss.setIri("?sourceDeviceId", subscription.getSourceDeviceId());
        pss.setIri("?sourcePlatformId", subscription.getSourcePlatformId());
        pss.setIri("?targetDeviceId", subscription.getTargetDeviceId());
        pss.setIri("?targetPlatformId", subscription.getTargetPlatformId());

        UpdateRequest updateRequest = pss.asUpdate();

        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", pss.toString());
        }

        try (RDFConnection conn = connect()) {
            conn.update(updateRequest);
            logger.debug("Subscription {} has been stored successfully.", subscription.getConversationId());
        }
    }

    public Plat2PlatSubscription getPlat2PlatSubscription(String conversationId) throws MiddlewareException {
        logger.debug("Retrieving plat2plat subscription with conversationId {}...", conversationId);
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        URL url = Resources.getResource("plat2plat-subs-getById.rq");
        String template;
        try {
            template = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new MiddlewareException("Failed to load SPARQL query.", e);
        }
        pss.setCommandText(template);
        pss.setIri("?conversationId", SUBSCRIPTION_PREFIX + conversationId);

        String sparql = pss.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        try (RDFConnection conn = connect()) {
            QueryExecution queryExecution = conn.query(sparql);
            ResultSet resultSet = queryExecution.execSelect();

            if (!resultSet.hasNext()) {
                return null;
            } else {
                Plat2PlatSubscription subscription = new Plat2PlatSubscription();
                subscription.setConversationId(conversationId);
                QuerySolution qs = resultSet.next();
                subscription.setClientId(qs.getResource("clientId").getURI().substring(CLIENT_PREFIX.length()));
                subscription.setTargetDeviceId(qs.getResource("targetDeviceId").getURI());
                subscription.setTargetPlatformId(qs.getResource("targetPlatformId").getURI());
                subscription.setSourceDeviceId(qs.getResource("sourceDeviceId").getURI());
                subscription.setSourcePlatformId(qs.getResource("sourcePlatformId").getURI());

                return subscription;
            }
        }
    }

    public Plat2PlatSubscription findPlat2PlatSubscription(String sourceDeviceId, String targetDeviceId) throws MiddlewareException {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        URL url = Resources.getResource("plat2plat-subs-getById.rq");
        String template;
        try {
            template = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new MiddlewareException("Failed to load SPARQL query.", e);
        }
        pss.setCommandText(template);
        pss.setIri("?sourceDeviceId", sourceDeviceId);
        pss.setIri("?targetDeviceId", targetDeviceId);

        String sparql = pss.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        try (RDFConnection conn = connect()) {
            QueryExecution queryExecution = conn.query(sparql);
            ResultSet resultSet = queryExecution.execSelect();

            if (!resultSet.hasNext()) {
                return null;
            } else {
                Plat2PlatSubscription subscription = new Plat2PlatSubscription();
                QuerySolution qs = resultSet.next();
                subscription.setConversationId(qs.getResource("conversationId").getURI().substring(SUBSCRIPTION_PREFIX.length()));
                subscription.setClientId(qs.getResource("clientId").getURI().substring(CLIENT_PREFIX.length()));
                subscription.setSourceDeviceId(sourceDeviceId);
                subscription.setSourcePlatformId(qs.getResource("sourcePlatformId").getURI());
                subscription.setTargetDeviceId(targetDeviceId);
                subscription.setTargetPlatformId(qs.getResource("targetPlatformId").getURI());

                return subscription;
            }
        }
    }

    public List<Plat2PlatSubscription> listPlat2PlatSubscriptions() throws MiddlewareException {
        return listPlat2PlatSubscriptions(null);
    }

    public List<Plat2PlatSubscription> listPlat2PlatSubscriptions(String clientId) throws MiddlewareException {
        logger.debug("Retrieving plat2plat subscriptions...");
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        URL url = Resources.getResource("plat2plat-subs-getById.rq");
        String template = null;
        try {
            template = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new MiddlewareException("Failed to load SPARQL query.", e);
        }
        pss.setCommandText(template);
        if (clientId != null) {
            pss.setIri("?clientId", CLIENT_PREFIX + clientId);
        }

        String sparql = pss.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        try (RDFConnection conn = connect()) {
            QueryExecution queryExecution = conn.query(sparql);
            ResultSet resultSet = queryExecution.execSelect();

            List<Plat2PlatSubscription> subscriptions = new ArrayList<>();
            while (resultSet.hasNext()) {
                QuerySolution qs = resultSet.next();
                String conversationId = qs.getResource("conversationId").getURI().substring(SUBSCRIPTION_PREFIX.length());
                if (clientId == null) {
                    clientId = qs.getResource("clientId").getURI().substring(CLIENT_PREFIX.length());
                }

                Plat2PlatSubscription vs = new Plat2PlatSubscription();
                vs.setConversationId(conversationId);
                vs.setClientId(clientId);
                vs.setTargetDeviceId(qs.getResource("targetDeviceId").getURI());
                vs.setTargetPlatformId(qs.getResource("targetPlatformId").getURI());
                vs.setSourceDeviceId(qs.getResource("sourceDeviceId").getURI());
                vs.setSourcePlatformId(qs.getResource("sourcePlatformId").getURI());
                subscriptions.add(vs);
            }

            return subscriptions;
        }
    }

    public void deletePlat2PlatSubscription(String conversationId) throws MiddlewareException {
        logger.debug("Deleting plat2plat subscription {}...", conversationId);

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        URL url = Resources.getResource("plat2plat-subs-remove.rq");
        String template = null;
        try {
            template = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new MiddlewareException("Failed to load SPARQL query.", e);
        }
        pss.setCommandText(template);
        pss.setIri("?conversationId", SUBSCRIPTION_PREFIX + conversationId);
        UpdateRequest updateRequest = pss.asUpdate();

        String sparql = pss.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", sparql);
        }

        try (RDFConnection conn = connect()) {
            conn.update(updateRequest);
            logger.debug("Plat2Plat subscription {} has been deleted.", conversationId);
        }
    }

    public void deleteSubscription(String conversationId) throws MiddlewareException {
        logger.debug("Deleting subscription {}...", conversationId);

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        URL url = Resources.getResource("subscription-remove.rq");
        String template = null;
        try {
            template = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new MiddlewareException("Failed to load SPARQL query.", e);
        }
        pss.setCommandText(template);
        pss.setIri("?conversationId", SUBSCRIPTION_PREFIX + conversationId);
        UpdateRequest updateRequest = pss.asUpdate();

        if (logger.isTraceEnabled()) {
            String sparql = pss.toString();
            logger.trace("SPARQL query:\n{}", sparql);
        }

        try (RDFConnection conn = connect()) {
            conn.update(updateRequest);
            logger.debug("Subscription {} has been deleted.", conversationId);
        }
    }


    public void registerLocationArea(LocationArea staticLocationArea) throws MiddlewareException {
        logger.debug("Registering a static location area");

        ParameterizedSparqlString pss = getPSSfromTemplate("location_area_register.rq");

        pss.setIri("?locationId", staticLocationArea.getLocationId());
        pss.setIri("?platformId", staticLocationArea.getPlatformId());
        pss.setLiteral("?description", staticLocationArea.getDescription());
        pss.setLiteral("?polygon_points", staticLocationArea.getWktGeometry());

        logger.debug(pss.toString());

        UpdateRequest updateRequest = pss.asUpdate();

        try (RDFConnection conn = connect()) {
            conn.update(updateRequest);
            logger.debug("Static location area {} has been registered.", staticLocationArea.getLocationId());
        }
    }

    public void registerLocationPoint(LocationPoint locationPoint) throws MiddlewareException {
        logger.debug("Registering a dynamic location");

        ParameterizedSparqlString pss = getPSSfromTemplate("location_point_register.rq");

        pss.setIri("?locationId", locationPoint.getLocationId());
        pss.setIri("?platformId", locationPoint.getPlatformId());
        pss.setLiteral("?description", locationPoint.getDescription());
        pss.setLiteral("?point", locationPoint.getWktPoint());

        logger.debug(pss.toString());

        UpdateRequest updateRequest = pss.asUpdate();

        try (RDFConnection conn = connect()) {
            conn.update(updateRequest);
            logger.debug("Dynamic location {} has been registered.", locationPoint.getLocationId());
        }
    }

    public List<LocationArea> getLocationAreas(String platformId) throws MiddlewareException {
        logger.debug("Retrieving all location areas");

        ParameterizedSparqlString pss = getPSSfromTemplate("location_area_getById.rq");
        pss.setIri("?platformIdInput", platformId);

        Query query = pss.asQuery();
        List<LocationArea> locationAreas = new ArrayList<>();

        try (RDFConnection connect = connect()) {
            QueryExecution queryExecution = connect.query(query);
            ResultSet resultSet = queryExecution.execSelect();

            while (resultSet.hasNext()) {
                QuerySolution next = resultSet.next();

                Resource locationIdRes = next.getResource("locationId");
                Resource platformIdRes = next.getResource("platformId");
                Literal descriptionLiteral = next.getLiteral("description");
                Literal geometryWKTLiteral = next.getLiteral("geometryWKT");

                locationAreas.add(new LocationArea(
                        locationIdRes.getURI(),
                        platformIdRes.getURI(),
                        descriptionLiteral.getString(),
                        geometryWKTLiteral.getString()
                ));
            }
        }
        return locationAreas;
    }

    public LocationArea getLocationArea(String platformId, String locationId) throws MiddlewareException {
        logger.debug("Retrieving location area {}", locationId);

        ParameterizedSparqlString pss = getPSSfromTemplate("location_area_getById.rq");
        pss.setIri("?locationIdInput", locationId);
        pss.setIri("?platformIdInput", platformId);
        Query query = pss.asQuery();

        LocationArea locationResult = null;

        try (RDFConnection conn = connect()) {
            QueryExecution qe = conn.query(query);

            ResultSet resultSet = qe.execSelect();

            int rowNumber = resultSet.getRowNumber();

            if (rowNumber > 1) {
                throw new MiddlewareException("Multiple locations found for id {}", locationId);
            }

            while (resultSet.hasNext()) {
                QuerySolution next = resultSet.next();
                Resource locationIdRes = next.getResource("locationId");
                Resource platformIdRes = next.getResource("platformId");
                Literal locationDescription = next.getLiteral("description");
                Literal geometryWKT = next.getLiteral("geometryWKT");

                locationResult = new LocationArea(
                        locationIdRes.getURI(),
                        platformIdRes.getURI(),
                        locationDescription.getString(),
                        geometryWKT.getString()
                );
            }
        }
        return locationResult;
    }

    public List<LocationPoint> getLocationPoints(String platformId) throws MiddlewareException {
        logger.debug("Retrieving all location points");

        ParameterizedSparqlString pss = getPSSfromTemplate("location_point_getById.rq");
        pss.setIri("?platformIdInput", platformId);

        Query query = pss.asQuery();
        List<LocationPoint> locationPoints = new ArrayList<>();

        try (RDFConnection connect = connect()) {
            QueryExecution queryExecution = connect.query(query);
            ResultSet resultSet = queryExecution.execSelect();

            while (resultSet.hasNext()) {
                QuerySolution next = resultSet.next();

                Resource locationIdRes = next.getResource("locationId");
                Resource platformIdRes = next.getResource("platformId");
                Literal descriptionLiteral = next.getLiteral("description");
                Literal geometryWKTLiteral = next.getLiteral("geometryWKT");

                locationPoints.add(new LocationPoint(
                        locationIdRes.getURI(),
                        platformIdRes.getURI(),
                        descriptionLiteral.getString(),
                        geometryWKTLiteral.getString()
                ));
            }
        }
        return locationPoints;
    }

    public LocationPoint getLocationPoint(String locationId) throws MiddlewareException {
        logger.debug("Retrieving dynamic location");

        ParameterizedSparqlString pss = getPSSfromTemplate("location_point_getById.rq");
        pss.setIri("?locationIdInput", locationId);

        Query query = pss.asQuery();
        LocationPoint locationPointResult = null;

        try (RDFConnection connect = connect()) {
            QueryExecution qe = connect.query(query);
            ResultSet resultSet = qe.execSelect();

            if (resultSet.getRowNumber() > 1) {
                throw new MiddlewareException("Multiple locations found for id {}", locationId);
            }

            while (resultSet.hasNext()) {
                QuerySolution next = resultSet.next();

                Resource id = next.getResource("locationId");
                Resource platformId = next.getResource("platformId");
                Literal description = next.getLiteral("description");
                Literal geometryWKT = next.getLiteral("geometryWKT");

                locationPointResult = new LocationPoint(
                        id.getURI(),
                        platformId.getURI(),
                        description.getString(),
                        geometryWKT.getString()
                );
            }

            return locationPointResult;
        }
    }

    public void updateLocationArea(String locationId, LocationArea updateInput) throws MiddlewareException {
        ParameterizedSparqlString pssRemoveById = getPSSfromTemplate("location_area_removeById.rq");
        ParameterizedSparqlString pssRegister = getPSSfromTemplate("location_area_register.rq");

        pssRemoveById.setIri("locationId", locationId);

        pssRegister.setIri("?locationId", updateInput.getLocationId());
        pssRegister.setIri("?platformId", updateInput.getPlatformId());
        pssRegister.setLiteral("?description", updateInput.getDescription());
        pssRegister.setLiteral("?polygon_points", updateInput.getWktGeometry());

        UpdateRequest removeByIdUpdateReq = pssRemoveById.asUpdate();
        UpdateRequest registerUpdateReq = pssRegister.asUpdate();

        try (RDFConnection connect = connect()) {
            connect.update(removeByIdUpdateReq);
            connect.update(registerUpdateReq);
            logger.debug("Static location area {} has been updated.", locationId);
        }
    }

    public void updateLocationPoint(String locationId, LocationPoint updateInput) throws MiddlewareException {
        // Delete
        ParameterizedSparqlString pssRemoveById = getPSSfromTemplate("location_point_removeById.rq");
        ParameterizedSparqlString pssRegister = getPSSfromTemplate("location_point_register.rq");

        pssRemoveById.setIri("locationId", locationId);

        pssRegister.setIri("?locationId", updateInput.getLocationId());
        pssRegister.setIri("?platformId", updateInput.getPlatformId());
        pssRegister.setLiteral("?description", updateInput.getDescription());
        pssRegister.setLiteral("?point", updateInput.getWktPoint());

        logger.debug("Delete query = \n " + pssRemoveById.toString());
        logger.debug("Update/insert query = \n " + pssRegister.toString());

        UpdateRequest removeByIdUpdateReq = pssRemoveById.asUpdate();
        UpdateRequest registerUpdateReq = pssRegister.asUpdate();

        try (RDFConnection connect = connect()) {
            connect.update(removeByIdUpdateReq);
            connect.update(registerUpdateReq);
            logger.debug("Dynamic location {} has been updated.", locationId);
        }
    }


    public void deleteLocationPoint(String locationId) throws MiddlewareException {
        ParameterizedSparqlString pssDelete = getPSSfromTemplate("location_point_removeById.rq");

        pssDelete.setIri("locationId", locationId);

        logger.debug("Delete query = \n " + pssDelete.toString());

        UpdateRequest deleteUpdateReq = pssDelete.asUpdate();
        try (RDFConnection connect = connect()) {
            connect.update(deleteUpdateReq);
            logger.debug("Dynamic location {} has been deleted.", locationId);
        }
    }

    public void deleteLocationArea(String locationId) throws MiddlewareException {
        ParameterizedSparqlString pssDelete = getPSSfromTemplate("location_area_removeById.rq");

        pssDelete.setIri("locationId", locationId);

        logger.debug("Delete query = \n " + pssDelete.toString());

        UpdateRequest deleteUpdateReq = pssDelete.asUpdate();
        try (RDFConnection connect = connect()) {
            connect.update(deleteUpdateReq);
            logger.debug("Dynamic location {} has been deleted.", locationId);
        }
    }

    public List<String> getLocationPointsInsideArea(String areaLocationId) throws MiddlewareException {
        ParameterizedSparqlString pss = getPSSfromTemplate("location_in_area_getById.rq");

        pss.setIri("locationId", areaLocationId);

        logger.debug("Get points in area query = \n", pss.toString());

        Query query = pss.asQuery();

        List<String> dynamicLocationsInsideArea = new ArrayList<>();

        try (RDFConnection connect = connect()) {
            QueryExecution queryExecution = connect.query(query);

            ResultSet resultSet = queryExecution.execSelect();

            while (resultSet.hasNext()) {
                QuerySolution next = resultSet.next();

                Resource dynamicLocationId = next.getResource("dynamicLocationId");
                dynamicLocationsInsideArea.add(dynamicLocationId.getURI());
            }
        }
        return dynamicLocationsInsideArea;
    }

    public QueryReturn executeQuery(String query, QueryType queryType) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(query);

        Query q = pss.asQuery();

        QueryReturn qr = new QueryReturn();

        try (RDFConnection conn = connect()) {
            switch (queryType) {
                case ASK:
                    boolean askResult = conn.queryAsk(q);
                    qr.setAskResult(askResult);
                    break;
                case CONSTRUCT:
                    Model constructResult = conn.queryConstruct(q);
                    qr.setConstructResult(constructResult);
                    break;
                case SELECT:
                    QueryExecution queryExecution = conn.query(q);
                    ResultSet resultSet = queryExecution.execSelect();
                    qr.setSelectResult(resultSet);
                    break;
            }
            return qr;
        }
    }

    public Configuration getConf() {
        return conf;
    }

    private String getRefInsert(String graph, String refId) {
        StringWriter sw = new StringWriter();
        sw.append(PREFIXES).append("\n\n");
        sw.append("INSERT DATA\n{")
                .append("GRAPH <").append(graph).append(">").append("{\n")
                .append("<").append(refId).append(">").append(" rdf:type <").append(refId).append(">\n")
                .append("}")
                .append("}");

        return sw.toString();
    }

    private String getClientGraphId(String clientId) {
        return CLIENTS_GRAPH + "#" + clientId;
    }

    private ParameterizedSparqlString getPSSfromTemplate(String templateResourceName) throws MiddlewareException {
        return getPSSfromTemplate(templateResourceName, null);
    }

    private ParameterizedSparqlString getPSSfromTemplate(String templateResourceName, @Nullable Map<String, String> replacements) throws MiddlewareException {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        URL url = Resources.getResource(templateResourceName);
        String template = null;
        try {
            template = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new MiddlewareException("Failed to load SPARQL query.", e);
        }

        if (replacements != null) {
            for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                template = template.replace(replacement.getKey(), replacement.getValue());
            }
        }

        pss.setCommandText(template);
        return pss;
    }

    private ParameterizedSparqlString getPSSDeviceByIds(List<String> deviceIds) throws MiddlewareException {
        ParameterizedSparqlString pss = getPSSfromTemplate("device-getByIds.rq");

        StringBuilder sb = new StringBuilder();

        Iterator<String> platformDeviceGraphIteratior = deviceIds.iterator();

        while (platformDeviceGraphIteratior.hasNext()) {
            String deviceGraphId = platformDeviceGraphIteratior.next();
            sb.append("{ GRAPH <").append(deviceGraphId).append("> { ?a ?b ?c } }");
            if (platformDeviceGraphIteratior.hasNext()) {
                sb.append(" UNION ");
            }
        }

        String commandText = pss.getCommandText();
        String deviceGraphUnion = sb.toString();
        pss.setCommandText(commandText.replace("{device_graph_union}", deviceGraphUnion));
        return pss;
    }

    private List<Platform> setPlatformStatistics(List<Platform> platforms) throws MiddlewareException {
        ParameterizedSparqlString pssCountDevices = getPSSfromTemplate("platforms-countDevices.rq");
        ParameterizedSparqlString pssCountSubscribedDevices = getPSSfromTemplate("platforms-countSubscribedDevices.rq");
        ParameterizedSparqlString pssCountSubscriptions = getPSSfromTemplate("platforms-countSubscriptions.rq");
        HashMap<String, Integer> devicesPerPlatform = new HashMap<>();
        HashMap<String, Integer> subscriptionsPerPlatform = new HashMap<>();
        HashMap<String, Integer> subscribedDevicesPerPlatform = new HashMap<>();
        List<Platform> setPlatforms = new ArrayList<>();

        try (RDFConnection conn = connect()) {
            Consumer<QuerySolution> consumerDevicesPerPlatform = querySolution -> {
                if (querySolution.contains("platformId") && querySolution.contains("devices")) {
                    devicesPerPlatform.put(querySolution.get("platformId").toString(), querySolution.get("devices").asLiteral().getInt());
                }
            };
            conn.querySelect(pssCountDevices.asQuery(), consumerDevicesPerPlatform);
            Consumer<QuerySolution> consumerSubscribedDevicesPerPlatform = querySolution -> {
                if (querySolution.contains("platformId") && querySolution.contains("subscribedDevices")) {
                    subscribedDevicesPerPlatform.put(querySolution.get("platformId").toString(), querySolution.get("subscribedDevices").asLiteral().getInt());
                }
            };
            conn.querySelect(pssCountSubscribedDevices.asQuery(), consumerSubscribedDevicesPerPlatform);
            Consumer<QuerySolution> consumerSubscriptionsPerPlatform = querySolution -> {
                if (querySolution.contains("platformId") && querySolution.contains("subscriptions")) {
                    subscriptionsPerPlatform.put(querySolution.get("platformId").toString(), querySolution.get("subscriptions").asLiteral().getInt());
                }
            };
            conn.querySelect(pssCountSubscriptions.asQuery(), consumerSubscriptionsPerPlatform);

        } catch (Exception e) {
            logger.error("Error occurred while listing devices: {} ", e);
        }

        for (Platform platform : platforms) {
            String platformId = platform.getPlatformId();
            PlatformStatistics platformStatistics = new PlatformStatistics();
            platformStatistics.setDeviceCount(devicesPerPlatform.getOrDefault(platformId, 0));
            platformStatistics.setSubscribedDeviceCount(subscribedDevicesPerPlatform.getOrDefault(platformId, 0));
            platformStatistics.setSubscriptionCount(subscriptionsPerPlatform.getOrDefault(platformId, 0));
            platform.setPlatformStatistics(platformStatistics);
            setPlatforms.add(platform);
        }
        return setPlatforms;
    }

    private RDFConnection connect() {
        return RDFConnectionFactory.connect(getConf().getParliamentUrl(), "sparql", "sparql", "sparql");
    }

    private void update(ParameterizedSparqlString pss) {
        if (logger.isTraceEnabled()) {
            logger.trace("SPARQL query:\n{}", pss.toString());
        }
        UpdateRequest updateRequest = pss.asUpdate();
        try (RDFConnection conn = connect()) {
            conn.update(updateRequest);
        }
    }

    private List<Platform> extractPlatformList(Model platforms) throws MalformedURLException {
        Map<String, Platform> platformMap = new HashMap<>();
        StmtIterator stmtIterator = platforms.listStatements();

        while (stmtIterator.hasNext()) {
            Statement next = stmtIterator.next();

            String platformId = next.getSubject().toString();
            Property predicate = next.getPredicate();

            RDFNode object = next.getObject();

            if (!platformMap.containsKey(platformId)) {
                platformMap.put(platformId, new Platform());
            }

            Platform platform = platformMap.get(platformId);
            platform.setPlatformId(platformId);

            if (predicate.getNameSpace().equals(MIDDLEWARE_NAMESPACE)) {
                String localName = predicate.getLocalName();
                switch (localName) {
                    case "name":
                        platform.setName(object.asLiteral().getString());
                        break;
                    case "type":
                        platform.setType(object.asLiteral().getString());
                        break;
                    case "baseEndpoint":
                        if (!object.asLiteral().getString().isEmpty()) {
                            platform.setBaseEndpoint(new URL(object.asLiteral().getString()));
                        }
                        break;
                    case "location":
                        platform.setLocationId(object.asLiteral().getString());
                        break;
                    case "clientId":
                        platform.setClientId(object.asResource().toString());
                        break;
                    case "username":
                        platform.setUsername(object.asLiteral().getString());
                        break;
                    case "timeCreated":
                        platform.setTimeCreated(object.asLiteral().getLong());
                        break;
                    case "encryptedPassword":
                        platform.setEncryptedPassword(object.asLiteral().getString());
                        break;
                    case "encryptionAlgorithm":
                        platform.setEncryptionAlgorithm(object.asLiteral().getString());
                        break;
                    case "downstreamInputAlignmentName":
                        platform.setDownstreamInputAlignmentName(object.asLiteral().getString());
                        break;
                    case "downstreamInputAlignmentVersion":
                        platform.setDownstreamInputAlignmentVersion(object.asLiteral().getString());
                        break;
                    case "downstreamOutputAlignmentName":
                        platform.setDownstreamOutputAlignmentName(object.asLiteral().getString());
                        break;
                    case "downstreamOutputAlignmentVersion":
                        platform.setDownstreamOutputAlignmentVersion(object.asLiteral().getString());
                        break;
                    case "upstreamInputAlignmentName":
                        platform.setUpstreamInputAlignmentName(object.asLiteral().getString());
                        break;
                    case "upstreamInputAlignmentVersion":
                        platform.setUpstreamInputAlignmentVersion(object.asLiteral().getString());
                        break;
                    case "upstreamOutputAlignmentName":
                        platform.setUpstreamOutputAlignmentName(object.asLiteral().getString());
                        break;
                    case "upstreamOutputAlignmentVersion":
                        platform.setUpstreamOutputAlignmentVersion(object.asLiteral().getString());
                        break;
                }
            }
        }
        return new ArrayList<>(platformMap.values());
    }

    private String getPlatformsQuery(List<String> platformIds) {
        // TODO template in rq file

        StringBuilder platformGraphs = new StringBuilder();

        Iterator<String> iterator = platformIds.iterator();
        while (iterator.hasNext()) {
            String platformId = iterator.next();
            platformGraphs.append("{ GRAPH <").append(platformId).append("> { ?s ?p ?o } }");
            if (iterator.hasNext()) {
                platformGraphs.append(" UNION ");
            }
        }

        return "PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>\n" +
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
                "PREFIX sosa: <http://www.w3.org/ns/sosa/>\n" +
                "\n" +
                "\n" +
                "CONSTRUCT {?s ?p ?o}\n{" +
                platformGraphs.toString() +
                "}";

    }

    private String buildDeviceDiscoverySparqlInsert(String platformId, Message message) {
        StringWriter sw = new StringWriter();

        Map<String, String> nsPrefixMap = message.getPrefixes();

        for (Map.Entry<String, String> entry : nsPrefixMap.entrySet()) {
            if (!entry.getKey().isEmpty()) {
                sw.append("PREFIX ").append(entry.getKey()).append(": <").append(entry.getValue()).append(">").append("\n");
            }
        }

        GraphExtract graphExtract = new GraphExtract(TripleBoundary.stopNowhere);
        ResIterator subjectIterator = message.getPayload().getJenaModel().listSubjects();

        while (subjectIterator.hasNext()) {
            // This iterates through all subjects in the device discovery graph response
            Resource next = subjectIterator.next();
            if (!next.isAnon()) {
                Graph extract = graphExtract.extract(next.asNode(), message.getPayload().getJenaModel().getGraph());

                Model extractModel = ModelFactory.createModelForGraph(extract);
                sw.append("INSERT DATA\n").append("{\n").append("\tGRAPH <").append(next.getURI()).append("> {\n");
                RDFDataMgr.write(sw, extractModel, Lang.TTL);
                sw.append("\t}\n};\n\n");

                sw.append("INSERT DATA \n").append("{\n")
                        .append("\tGRAPH <").append("http://inter-iot.eu/devices").append("> {\n")
                        .append("<").append(next.getURI()).append(">").append(" sosa:isHostedBy ").append(" <").append(platformId).append(">;\n")
                        .append("}")
                        .append("};\n\n");
            }
        }
        return sw.toString();
    }
}
