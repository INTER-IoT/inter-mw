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
package eu.interiot.intermw.integrationtest;

import com.google.common.hash.Hashing;
import eu.interiot.intermw.api.InterMWInitializer;
import eu.interiot.intermw.api.model.RegisterClientInput;
import eu.interiot.intermw.api.model.RegisterPlatformInput;
import eu.interiot.intermw.api.rest.model.MwAsyncResponse;
import eu.interiot.intermw.api.rest.resource.InterMwApiREST;
import eu.interiot.intermw.api.rest.resource.InterMwExceptionMapper;
import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.dto.ResponseMessage;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.Client;
import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.commons.model.IoTDeviceFilter;
import eu.interiot.intermw.commons.model.enums.IoTDeviceType;
import eu.interiot.intermw.integrationtests.utils.TestUtils;
import eu.interiot.message.Message;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.metadata.QueryMessageMetadata;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeviceDiscoveryIntegrationTest extends JerseyTest {
    private static final String CONFIG_FILE = "intermw.properties";
    private static final String PLATFORM_ID = "http://test.inter-iot.eu/test-platform_discovery";
    private static final String CLIENT_ID = "myclient";
    private static final int TIMEOUT = 30;
    private static final Logger logger = LoggerFactory.getLogger(DeviceDiscoveryIntegrationTest.class);
    private Map<String, BlockingQueue<Message>> conversations = new HashMap<>();
    private Client client;
    private static Configuration conf;

    @BeforeClass
    public static void beforeClass() throws MiddlewareException {
        conf = new DefaultConfiguration(CONFIG_FILE);
        TestUtils.clearRabbitMQ(conf);
        TestUtils.clearParliament(conf);

        InterMWInitializer.initialize();
    }

    @After
    @Before
    public void clearParliament() {
        TestUtils.clearParliament(conf);
    }

    @Override
    protected Application configure() {
        final SecurityContext securityContextMock = mock(SecurityContext.class);
        when(securityContextMock.getUserPrincipal()).thenReturn(() -> CLIENT_ID);

        return new ResourceConfig()
                .register((ContainerRequestFilter) requestContext -> requestContext.setSecurityContext(securityContextMock))
                .register(InterMwApiREST.class)
                .register(InterMwExceptionMapper.class);
    }

    @Test
    public void test() throws Exception {
        client = registerAndAssertClient();

        startPullingResponseMessages();
        Client.ResponseFormat responseFormat = client.getResponseFormat();

        registerPlatform(responseFormat);

        // sleep for a moment to let device discovery message to arrive
        Thread.sleep(5000);

        retrieveDevices();

        discoveryActuators();
        discoverySensors();
        discoveryAllDevices();

        discoveryHostedBy();
        discoveryLocation();
        discoveryName();

        discoveryByMessage(responseFormat);

        logger.info("DeviceDiscoveryIntegrationTest finished successfully.");
    }

    private void discoveryByMessage(Client.ResponseFormat responseFormat) throws IOException, InterruptedException {
        // We'll try to get all devices with 'Noatum' in the name
        String queryString = "PREFIX iiot: <http://inter-iot.eu/GOIoTP#>\n" +
                "PREFIX mdw: <http://interiot.eu/ont/middleware.owl#>\n" +
                "PREFIX sosa: <http://www.w3.org/ns/sosa/>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "\n" +
                "SELECT ?s ?p ?o\n" +
                "WHERE {\n" +
                "    GRAPH <http://inter-iot.eu/devices> {\n" +
                "        ?deviceId sosa:isHostedBy ?platformId;\n" +
                "        GRAPH ?deviceId {\n" +
                "            ?s ?p ?o;\n" +
                "            \n" +
                "        }\n" +
                "    }\n" +
                "    FILTER regex(?o, \"Noatum\")\n" +
                "}\n";

        Message message = new Message();
        QueryMessageMetadata queryMessageMetadata = message.getMetadata().asQueryMetadata();
        queryMessageMetadata.setQueryString(queryString);
        queryMessageMetadata.setClientId(CLIENT_ID);
        queryMessageMetadata.setMessageType(URIManagerMessageMetadata.MessageTypesEnum.DEVICE_DISCOVERY_QUERY);

        Response response = request("mw2mw/requests").post(Entity.entity(message.serializeToJSONLD(), "application/ld+json"));

        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();

        ResponseMessage responseMessage = waitForResponseMessage(conversationId, responseFormat);
        Message responseMessageJsonLD = responseMessage.getMessageJSON_LD();
        NodeIterator nodeIterator = responseMessageJsonLD.getPayload().getJenaModel().listObjects();

        Set<String> literals = new HashSet<>();
        Set<String> resources = new HashSet<>();
        while (nodeIterator.hasNext()) {
            RDFNode next = nodeIterator.next();
            if (!next.isAnon()) {
                // we ignore anonymous entities
                if (next.isLiteral()) {
                    // these are literal names
                    literals.add(next.toString());
                } else if (next.isURIResource()) {
                    resources.add(next.toString());
                }
            }
        }

        assertEquals("Literals should contain all distinct names, subject, " +
                "predicate and object names, and some parliament additional data", 9, literals.size());
        assertEquals("Resources should contain all deviceIds, predicate URI and some" +
                "additional parliament data", 33, resources.size());

        assertTrue("Literals should contain device name \"Railway switchgears Noatum access - APV managed\"",
                literals.contains("Railway switchgears Noatum access - APV managed"));
        assertTrue("Literals should contain device name \"Noatum access road (B14) - APV managed\"",
                literals.contains("Noatum access road (B14) - APV managed"));
        assertTrue("Literals should contain device name \"Noatum access road - APV managed\"",
                literals.contains("Noatum access road - APV managed"));
        assertTrue("Literals should contain device name \"Noatum access road (B01) - E3TCity managed\"",
                literals.contains("Noatum access road (B01) - E3TCity managed"));
        assertTrue("Literals should contain device name \"Noatum access road - E3TCity managed\"",
                literals.contains("Noatum access road - E3TCity managed"));


        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/presenceSensors/P01\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/presenceSensors/P01"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/presenceSensors/P02\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/presenceSensors/P02"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/presenceSensors/P03\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/presenceSensors/P03"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L01\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L01"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L02\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L02"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L03\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L03"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L04\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L04"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L05\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L05"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L06\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L06"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L07\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L07"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L08\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L08"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L09\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L09"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L10\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L10"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L11\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L11"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L12\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L12"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L13\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L13"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L14\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L14"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L15\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L15"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L16\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L16"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L17\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L17"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L18\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L18"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L19\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L19"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L20\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L20"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L21\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L21"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L22\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L22"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L23\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L23"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L24\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L24"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L25\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L25"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L26\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L26"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L27\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L27"));
        assertTrue("Resources should contain device id \"http://inter-iot.eu/test-platform_discovery/light/L28\"",
                resources.contains("http://inter-iot.eu/test-platform_discovery/light/L28"));
    }

    private Client registerAndAssertClient() throws Exception {
        RegisterClientInput input = new RegisterClientInput();
        input.setClientId(CLIENT_ID);
        input.setResponseDelivery(Client.ResponseDelivery.CLIENT_PULL);
        input.setResponseFormat(Client.ResponseFormat.JSON_LD);
        input.setReceivingCapacity(10);
        Entity<RegisterClientInput> entity = Entity.json(input);

        // register
        Response response = request("mw2mw/clients").post(entity);

        return response.readEntity(Client.class);
    }

    private void registerPlatform(Client.ResponseFormat responseFormat) throws Exception {
        RegisterPlatformInput input = new RegisterPlatformInput();
        input.setPlatformId(PLATFORM_ID);
        input.setName("InterMW Test Platform");
        input.setType("http://inter-iot.eu/MWTestPlatform");
        input.setBaseEndpoint("http://localhost:4568");
        input.setLocationId("http://test.inter-iot.eu/TestLocation");

        String encryptedPassword = Hashing.sha256()
                .hashString("somepassword", StandardCharsets.UTF_8)
                .toString();
        input.setUsername("interiotuser");
        input.setEncryptedPassword(encryptedPassword);
        input.setEncryptionAlgorithm("SHA-256");
        Entity<RegisterPlatformInput> entity = Entity.json(input);

        Response response = request("mw2mw/platforms").post(entity);
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();

        ResponseMessage responseMessageWrapper = waitForResponseMessage(conversationId, responseFormat);
        logger.info("---- Got response from registerPlatform = " + responseMessageWrapper.getMessageJSON_LD().serializeToJSONLD());
    }

    private void retrieveDevices() {
        // retrieve devices
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("platformId", PLATFORM_ID);

        Response response = request("mw2mw/devices", queryParams).get();
        List<IoTDevice> devices = response.readEntity(new GenericType<List<IoTDevice>>() {
        });
        Map<String, IoTDevice> deviceMap = new HashMap<>();
        for (IoTDevice device : devices) {
            deviceMap.put(device.getDeviceId(), device);
        }
        assertEquals(56, devices.size());
    }

    private void discoveryByType(IoTDeviceType type, int expectedResultCount) {
        IoTDeviceFilter input = new IoTDeviceFilter();
        input.addDeviceType(type);

        List<IoTDevice> sensorDevices = getIoTDevices(input);

        assertEquals("Device query should return all sensors", expectedResultCount, sensorDevices.size());

        for (IoTDevice device : sensorDevices) {
            assertTrue(device.getDeviceTypes().contains(type));
        }
    }

    private void discoverySensors() {
        discoveryByType(IoTDeviceType.SENSOR, 28);
    }

    private void discoveryActuators() {
        discoveryByType(IoTDeviceType.ACTUATOR, 28);
    }

    private void discoveryAllDevices() {
        discoveryByType(IoTDeviceType.DEVICE, 56);
    }

    private Invocation.Builder request(String path, String clientId) {
        return target(path)
                .request();
    }

    private void discoveryHostedBy() {
        IoTDeviceFilter input = new IoTDeviceFilter();
        String hostedBy = PLATFORM_ID;
        input.setHostedBy(hostedBy);

        List<IoTDevice> hostedByDevices = getIoTDevices(input);

        assertEquals("Query should return all devices hosted by a platform", 56, hostedByDevices.size());

        for (IoTDevice device : hostedByDevices) {
            assertEquals("Device should have correct hostedBy value", hostedBy, device.getHostedBy());
        }
    }

    private void discoveryLocation() {
        IoTDeviceFilter input = new IoTDeviceFilter();
        input.setLocation("http://test.inter-iot.eu/test-location");
        List<IoTDevice> ioTDevices = getIoTDevices(input);
        assertEquals("Query should return 0 devices with location field", 0, ioTDevices.size());
    }

    private void discoveryName() {
        IoTDeviceFilter input = new IoTDeviceFilter();
        String name = "Noatum access road - E3TCity managed";
        input.setName(name);

        List<IoTDevice> ioTDevices = getIoTDevices(input);

        assertEquals("Query by name should return all IoTDevices with the same name", 14, ioTDevices.size());

        IoTDevice ioTDevice = ioTDevices.get(0);

        assertEquals("Retrieved device should contain correct value in name field", name, ioTDevice.getName());
    }

    private List<IoTDevice> getIoTDevices(IoTDeviceFilter input) {
        Entity<IoTDeviceFilter> entity = Entity.json(input);
        Response response = request("mw2mw/devices/query", CLIENT_ID).post(entity);
        GenericType<List<IoTDevice>> iotDeviceListGenericType = new GenericType<List<IoTDevice>>() {
        };
        return response.readEntity(iotDeviceListGenericType);
    }

    private void startPullingResponseMessages() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        Entity<Object> emptyEntity = Entity.json(null);
                        Response response = request("mw2mw/responses").post(emptyEntity);
                        String messageString = response.readEntity(String.class);
                        assertEquals(response.getStatus(), 200);
                        assertTrue(messageString.startsWith("[") && messageString.endsWith("]"));

                        if (!messageString.equals("[]")) {
                            logger.debug("Message string: " + messageString);
                            messageString = messageString.substring(1, messageString.length() - 2);
                            // JSON_LD messages
                            String[] rawMessagesArray = messageString.split("\\r?\\n,\\r?\\n");
                            logger.debug("Number of response messages received: {}", rawMessagesArray.length);
                            for (String rawMessage : rawMessagesArray) {
                                Message message = new Message(rawMessage);
                                String conversationId = message.getMetadata().getConversationId().get();
                                if (!conversations.containsKey(conversationId)) {
                                    fail("Unexpected conversationId.");
                                }
                                logger.debug("New message has been retrieved of type {} with conversationId {}.",
                                        message.getMetadata().getMessageTypes(), conversationId);
                                conversations.get(conversationId).add(message);
                            }
                        }
                        Thread.sleep(500);

                    } catch (Exception e) {
                        logger.error("Failed to retrieve response messages.", e);
                        fail();
                    }
                } while (true);
            }
        };
        new Thread(runnable).start();
    }

    private ResponseMessage waitForResponseMessage(String conversationId, Client.ResponseFormat responseFormat) throws InterruptedException {
        return new ResponseMessage(waitForJSONLDResponseMessage(conversationId), responseFormat);
    }

    private Message waitForJSONLDResponseMessage(String conversationId) throws InterruptedException {
        if (!conversations.containsKey(conversationId)) {
            conversations.put(conversationId, new LinkedBlockingQueue<>());
        }

        Message message = conversations.get(conversationId).poll(TIMEOUT, TimeUnit.SECONDS);
        assertNotNull(message);
        assertEquals(message.getMetadata().getConversationId().get(), conversationId);
        return message;
    }

    private Invocation.Builder request(String path) {
        return target(path)
                .request();
    }

    private Invocation.Builder request(String path, Map<String, Object> queryParams) {
        WebTarget target = target(path);

        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            target = target.queryParam(entry.getKey(), entry.getValue());
        }

        return target
                .request();
    }
}
