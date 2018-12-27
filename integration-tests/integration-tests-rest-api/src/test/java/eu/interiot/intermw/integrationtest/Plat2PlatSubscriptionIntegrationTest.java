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

import eu.interiot.intermw.api.InterMWInitializer;
import eu.interiot.intermw.api.model.*;
import eu.interiot.intermw.api.rest.model.MwAsyncResponse;
import eu.interiot.intermw.api.rest.resource.InterMwApiREST;
import eu.interiot.intermw.api.rest.resource.InterMwExceptionMapper;
import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.model.Client;
import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.commons.model.Plat2PlatSubscription;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.commons.model.enums.IoTDeviceType;
import eu.interiot.intermw.integrationtests.utils.TestUtils;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import eu.interiot.message.payload.types.ObservationPayload;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Service;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Plat2PlatSubscriptionIntegrationTest extends JerseyTest {
    private static final Logger logger = LoggerFactory.getLogger(Plat2PlatSubscriptionIntegrationTest.class);
    private static final String CONFIG_FILE = "intermw.properties";
    private static final int CALLBACK_PORT = 7070;
    private static final String CALLBACK_URL = "http://localhost:" + CALLBACK_PORT + "/endpoint";
    private static final String CLIENT_ID = "myclient";
    private static final String SOURCE_PLATFORM_ID = "http://test.inter-iot.eu/test-platform_discovery";
    private static final String SOURCE_DEVICE_ID_PREFIX = "http://inter-iot.eu/test-platform_discovery/gate/";
    private static final String TARGET_PLATFORM_ID = "http://virtual.inter-iot.eu/platform";
    private static final String TARGET_DEVICE_ID_PREFIX = "http://virtual.inter-iot.eu/device/";
    private static final String LOCATION_S = "http://inter-iot.eu/TestLocation1";
    private static final String LOCATION_T = "http://inter-iot.eu/TestLocation2";
    private static final int TIMEOUT = 3000;
    private static DefaultConfiguration conf;
    private Map<String, BlockingQueue<Message>> conversations = new HashMap<>();
    private Service listenerService;

    @BeforeClass
    public static void beforeClass() throws Exception {
        conf = new DefaultConfiguration(CONFIG_FILE);
        TestUtils.clearRabbitMQ(conf);
        TestUtils.clearParliament(conf);
        InterMWInitializer.initialize();
    }

    @After
    public void after() throws Exception {
        if (listenerService != null) {
            listenerService.stop();
            logger.debug("Client listener has stopped.");
        }
        for (String conversation : conversations.keySet()) {
            if (!conversations.get(conversation).isEmpty()) {
                fail("Conversations map is not empty.");
            }
        }
        TestUtils.checkIfRabbitQueuesEmpty(conf);
    }

    @Override
    protected Application configure() {
        final SecurityContext securityContextMock = mock(SecurityContext.class);
        when(securityContextMock.getUserPrincipal()).thenReturn(() -> CLIENT_ID);

        ResourceConfig resourceConfig = new ResourceConfig();
        return resourceConfig
                .register((ContainerRequestFilter) requestContext -> requestContext.setSecurityContext(securityContextMock))
                .register(InterMwApiREST.class)
                .register(InterMwExceptionMapper.class);
    }

    @Test
    public void integrationTest() throws Exception {
        logger.info("Platform-to-Platform subscription integration test has started.");

        registerClient();
        listenerService = startClientListener();
        registerPlatforms();
        createVirtualDevices();
        List<String> plat2PlatSubscriptionIds = subscribePlat2Plat();
        String conversationId = subscribe();
        checkObservations(conversationId);
        unsubscribePlat2Plat(plat2PlatSubscriptionIds);

        // observations shouldn't be arriving any more
        Message shouldBeNull = conversations.get(conversationId).poll(5, TimeUnit.SECONDS);
        assertNull("Unexpected message arrived.", shouldBeNull);

        unsubscribe(conversationId);

        logger.info("Platform-to-Platform subscription integration test finished successfully.");
    }

    private void registerClient() throws Exception {
        RegisterClientInput input = new RegisterClientInput();
        input.setClientId(CLIENT_ID);
        input.setResponseDelivery(Client.ResponseDelivery.SERVER_PUSH);
        input.setCallbackUrl(CALLBACK_URL);
        input.setResponseFormat(Client.ResponseFormat.JSON_LD);
        input.setReceivingCapacity(5);

        Entity<RegisterClientInput> entity = Entity.json(input);
        Response response = request("mw2mw/clients").post(entity);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
    }

    private void registerPlatforms() throws Exception {
        RegisterPlatformInput platform1 = new RegisterPlatformInput();
        platform1.setPlatformId(SOURCE_PLATFORM_ID);
        platform1.setName("Source Platform");
        platform1.setType("http://inter-iot.eu/MWTestPlatform");
        platform1.setBaseEndpoint("http://localhost:4568");
        platform1.setLocationId(LOCATION_S);
        registerPlatform(platform1);

        RegisterPlatformInput platform2 = new RegisterPlatformInput();
        platform2.setPlatformId(TARGET_PLATFORM_ID);
        platform2.setName("Target Platform");
        platform2.setType("http://inter-iot.eu/MWTestPlatform");
        platform2.setBaseEndpoint("http://localhost:4569");
        platform2.setLocationId(LOCATION_T);
        registerPlatform(platform2);

        Response response = request("mw2mw/platforms").get();
        List<Platform> platforms = response.readEntity(new GenericType<List<Platform>>() {
        });
        Set<String> platformIds = new HashSet<>();
        for (Platform platform : platforms) {
            platformIds.add(platform.getPlatformId());
        }
        assertTrue(platformIds.contains(SOURCE_PLATFORM_ID));
        assertTrue(platformIds.contains(TARGET_PLATFORM_ID));
    }

    private void registerPlatform(RegisterPlatformInput platform) throws Exception {
        Entity<RegisterPlatformInput> entity = Entity.json(platform);
        Response response = request("mw2mw/platforms").post(entity);
        assertEquals(202, response.getStatus());
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();
        Message responseMessage = waitForResponseMessage(conversationId);
        assertTrue(responseMessage.getMetadata().getMessageTypes().containsAll(
                EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.PLATFORM_REGISTER)));
    }

    private void createVirtualDevices() throws Exception {
        List<IoTDevice> devices = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            IoTDevice ioTDevice = new IoTDevice(TARGET_DEVICE_ID_PREFIX + i);
            ioTDevice.setName("Virtual device " + i);
            ioTDevice.setHostedBy(TARGET_PLATFORM_ID);
            ioTDevice.setLocation(LOCATION_T);
            ioTDevice.setDeviceTypes(EnumSet.of(IoTDeviceType.SENSOR, IoTDeviceType.VIRTUAL));
            devices.add(ioTDevice);
        }
        PlatformCreateDeviceInput input = new PlatformCreateDeviceInput(devices);

        Entity<PlatformCreateDeviceInput> entity = Entity.json(input);
        Response response = request("mw2mw/devices").post(entity);
        assertEquals(202, response.getStatus());
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();

        Message responseMessage = waitForResponseMessage(conversationId);
        assertTrue(responseMessage.getMetadata().getMessageTypes().containsAll(
                EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.PLATFORM_CREATE_DEVICE)));

        // retrieve virtual devices
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("platformId", TARGET_PLATFORM_ID);
        Response responseGet = request("mw2mw/devices", queryParams).get();
        List<IoTDevice> devicesGet = responseGet.readEntity(new GenericType<List<IoTDevice>>() {
        });
        assertEquals(devices.size(), devicesGet.size());
        Set<String> deviceIdsGet = new HashSet<>();
        for (IoTDevice device : devicesGet) {
            deviceIdsGet.add(device.getDeviceId());
        }
        for (IoTDevice device : devices) {
            assertTrue(deviceIdsGet.contains(device.getDeviceId()));
        }
    }

    private List<String> subscribePlat2Plat() throws Exception {
        List<String> plat2PlatSubIds = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            Plat2PlatSubscribeInput input = new Plat2PlatSubscribeInput();
            input.setTargetDeviceId(TARGET_DEVICE_ID_PREFIX + i);
            input.setSourceDeviceId(SOURCE_DEVICE_ID_PREFIX + i);

            Entity<Plat2PlatSubscribeInput> entity = Entity.json(input);
            Response response = request("mw2mw/subscriptions-platform-to-platform").post(entity);
            assertEquals(202, response.getStatus());
            MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
            String conversationId = mwAsyncResponse.getConversationId();
            Message responseMessage = waitForResponseMessage(conversationId);
            assertTrue(responseMessage.getMetadata().getMessageTypes().containsAll(
                    EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.VIRTUAL_SUBSCRIBE)));
            plat2PlatSubIds.add(conversationId);
        }

        Response response = request("mw2mw/subscriptions-platform-to-platform").get();
        assertEquals(200, response.getStatus());
        List<Plat2PlatSubscription> plat2PlatSubs = response.readEntity(new GenericType<List<Plat2PlatSubscription>>() {
        });
        assertEquals(2, plat2PlatSubs.size());
        Map<String, Plat2PlatSubscription> plat2PlatSubsMap =
                plat2PlatSubs.stream().collect(Collectors.toMap(Plat2PlatSubscription::getConversationId, x -> x));
        for (int i = 1; i <= 2; i++) {
            Plat2PlatSubscription vs = plat2PlatSubsMap.get(plat2PlatSubIds.get(i - 1));
            assertEquals(TARGET_DEVICE_ID_PREFIX + i, vs.getTargetDeviceId());
            assertEquals(TARGET_PLATFORM_ID, vs.getTargetPlatformId());
            assertEquals(SOURCE_DEVICE_ID_PREFIX + i, vs.getSourceDeviceId());
            assertEquals(SOURCE_PLATFORM_ID, vs.getSourcePlatformId());
        }

        return plat2PlatSubIds;
    }

    private void unsubscribePlat2Plat(List<String> plat2PlatSubscriptionIds) throws InterruptedException {
        for (String subscriptionId : plat2PlatSubscriptionIds) {
            Response response = request("mw2mw/subscriptions-platform-to-platform/{conversationId}", "conversationId", subscriptionId)
                    .delete();
            assertEquals(202, response.getStatus());
            MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
            String conversationId = mwAsyncResponse.getConversationId();
            Message responseMessage = waitForResponseMessage(conversationId);
            assertTrue(responseMessage.getMetadata().getMessageTypes().containsAll(
                    EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.VIRTUAL_UNSUBSCRIBE)));
            assertEquals(subscriptionId, responseMessage.getMetadata().asPlatformMessageMetadata().getSubscriptionId().get());
        }
    }

    private void unsubscribe(String conversationId) throws InterruptedException {
        Response response = request("mw2mw/subscriptions/{conversationId}", "conversationId", conversationId)
                .delete();
        assertEquals(202, response.getStatus());
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String unsubscribeConversationId = mwAsyncResponse.getConversationId();
        Message responseMessage = waitForResponseMessage(unsubscribeConversationId);
        assertTrue(responseMessage.getMetadata().getMessageTypes().containsAll(
                EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.UNSUBSCRIBE)));
    }

    private String subscribe() throws InterruptedException {
        SubscribeInput input = new SubscribeInput();
        input.addIoTDevice(TARGET_DEVICE_ID_PREFIX + "1");
        input.addIoTDevice(TARGET_DEVICE_ID_PREFIX + "2");
        Entity<SubscribeInput> entity = Entity.json(input);
        Response response = request("mw2mw/subscriptions").post(entity);
        assertEquals(202, response.getStatus());
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();
        Message responseMessage = waitForResponseMessage(conversationId);
        assertTrue(responseMessage.getMetadata().getMessageTypes().containsAll(
                EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.SUBSCRIBE)));
        return conversationId;
    }

    private void checkObservations(String conversationId) throws Exception {
        Map<String, Integer> madeBySensorMap = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            Message message = waitForResponseMessage(conversationId);
            MessageMetadata metadata = message.getMetadata();
            assertTrue(metadata.getMessageTypes().contains(MessageTypesEnum.OBSERVATION));
            assertEquals(conversationId, metadata.getConversationId().get());
            assertEquals(TARGET_PLATFORM_ID, metadata.asPlatformMessageMetadata().getSenderPlatformId().get().toString());

            ObservationPayload observationPayload = message.getPayloadAsGOIoTPPayload().asObservationPayload();
            for (EntityID entityID : observationPayload.getObservations()) {
                String madeBySensor = observationPayload.getMadeBySensor(entityID).get().toString();
                if (madeBySensorMap.containsKey(madeBySensor)) {
                    int count = madeBySensorMap.get(madeBySensor);
                    madeBySensorMap.replace(madeBySensor, count + 1);
                } else {
                    madeBySensorMap.put(madeBySensor, 1);
                }
            }
        }
        assertEquals(2, madeBySensorMap.size());
        assertEquals(3, (int) madeBySensorMap.get("http://virtual.inter-iot.eu/device/1"));
        assertEquals(3, (int) madeBySensorMap.get("http://virtual.inter-iot.eu/device/2"));
    }

    private Service startClientListener() {
        Service spark = Service.ignite().port(CALLBACK_PORT);
        logger.debug("Client {} is listening on port {}.", CLIENT_ID, CALLBACK_PORT);

        spark.post("/endpoint", (request, response) -> {
            try {
                String content = request.body();

                assertTrue(content.startsWith("[") && content.endsWith("]"));
                assertEquals(request.contentType(), "application/ld+json; charset=UTF-8");
                content = content.substring(1, content.length() - 2);

                String[] rawMessagesArray = content.split("\\r?\\n,\\r?\\n");
                logger.debug("New message array has been received with content type {} containing {} messages.",
                        request.contentType(), rawMessagesArray.length);

                for (String rawMessage : rawMessagesArray) {
                    logger.debug("Message content:\n{}", rawMessage);
                    Message message = new Message(rawMessage);
                    String conversationId = message.getMetadata().getConversationId().get();
                    if (!conversations.containsKey(conversationId)) {
                        fail("Unexpected conversationId.");
                    }
                    conversations.get(conversationId).add(message);
                }

                logger.debug("Message(s) have been processed successfully.");

            } catch (Error e) {
                logger.error("Failed to handle incoming message(s): " + e.getMessage(), e);
                throw e;
            }

            response.status(204);
            return "";
        });

        return spark;
    }

    private Message waitForResponseMessage(String conversationId) throws InterruptedException {
        if (!conversations.containsKey(conversationId)) {
            conversations.put(conversationId, new LinkedBlockingQueue<>());
        }

        Message message = conversations.get(conversationId).poll(TIMEOUT, TimeUnit.SECONDS);
        assertNotNull("Timeout while waiting for response message.", message);
        assertEquals(message.getMetadata().getConversationId().get(), conversationId);
        return message;
    }

    private Invocation.Builder request(String path) {
        return target(path)
                .request();
    }

    private Invocation.Builder request(String path, String param, String value) {
        return target(path)
                .resolveTemplate(param, value)
                .request();
    }

    private Invocation.Builder request(String path, Map<String, String> queryParams) {
        WebTarget target = target(path);
        for (String name : queryParams.keySet()) {
            target = target.queryParam(name, queryParams.get(name));
        }
        return target.request();
    }
}