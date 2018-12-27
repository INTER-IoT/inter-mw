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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import eu.interiot.intermw.api.InterMWInitializer;
import eu.interiot.intermw.api.model.*;
import eu.interiot.intermw.api.rest.model.MwAsyncResponse;
import eu.interiot.intermw.api.rest.resource.InterMwApiREST;
import eu.interiot.intermw.api.rest.resource.InterMwExceptionMapper;
import eu.interiot.intermw.comm.arm.ResponseMessageParser;
import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.dto.ResponseMessage;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.ActuationResult;
import eu.interiot.intermw.commons.model.Client;
import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.commons.responses.*;
import eu.interiot.intermw.integrationtests.utils.TestErrorTopicHandler;
import eu.interiot.intermw.integrationtests.utils.TestUtils;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.managers.URI.URIManagerINTERMW;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MwRestApiIntegrationTest extends JerseyTest {
    private static final Logger logger = LoggerFactory.getLogger(MwRestApiIntegrationTest.class);
    private static final String CONFIG_FILE = "intermw.properties";
    private static final String PLATFORM_TYPE = "http://inter-iot.eu/MWTestPlatform";
    private static final String PLATFORM_ID = "http://test.inter-iot.eu/test-platform1";
    private static final String CLIENT_ID = "myclient";
    private static final String DEVICE_ID_PREFIX = "http://test.inter-iot.eu/device";
    private static final String ACTUATOR_ID = "http://test.inter-iot.eu/actuator";
    private static final String ACTUATOR_LOCAL_ID = "actuator";
    private static final String PROPERTY_ID = "http://test.inter-iot.eu/property";
    private static final String RESULT_VALUE = "result_value";
    private static final String RESULT_UNIT = "http://test.inter-iot.eu/unit";
    private static final int TIMEOUT = 30;
    private static final int CALLBACK_PORT = 7070;
    private static final String CALLBACK_URL = "http://localhost:" + CALLBACK_PORT + "/endpoint";
    private static final int RESPONSE_CODE_202 = 202;
    private static final int RESPONSE_CODE_404 = 404;
    private static final int RESPONSE_CODE_204 = 204;
    private Map<String, BlockingQueue<Message>> conversationsJsonld = new HashMap<>();
    private Map<String, BlockingQueue<BaseRes>> conversationsJson = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();
    private static Configuration conf;
    private static TestErrorTopicHandler errorTopicHandler;
    private Service listenerService;

    @BeforeClass
    public static void beforeClass() throws Exception {
        conf = new DefaultConfiguration(CONFIG_FILE);
        TestUtils.clearRabbitMQ(conf);
        TestUtils.clearParliament(conf);

        TestUtils.uploadAlignments(conf);

        InterMWInitializer.initialize();

        errorTopicHandler = new TestErrorTopicHandler();
    }

    @Before
    public void before() {
        TestUtils.clearParliament(conf);
        conversationsJson.clear();
        conversationsJsonld.clear();
    }

    @After
    public void after() {
        if (listenerService != null) {
            listenerService.stop();
            logger.debug("Client listener has stopped.");
        }
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
    public void testJsonLD() throws Exception {
        executeTest(Client.ResponseFormat.JSON_LD);
    }

    @Test
    public void testSimpleJson() throws Exception {
        executeTest(Client.ResponseFormat.JSON);
    }

    private void executeTest(Client.ResponseFormat responseFormat) throws Exception {
        logger.info("MW REST API integration test using {} response format started.", responseFormat);

        registerClient(responseFormat);
        updateClient();
        listenerService = startClientListener(responseFormat);

        listClients();

        listPlatformTypes(responseFormat);

        registerPlatform(responseFormat);
        updatePlatform(responseFormat);

        registerDevices(responseFormat);
        updateDevices(responseFormat);
        deleteDevices(responseFormat);
        retrieveDevices();

        String conversationId = subscribe(responseFormat);
        listPlatforms();
        checkObservations(conversationId, responseFormat);
        unsubscribe(conversationId, responseFormat);

        actuate(responseFormat);
        unregisterPlatform(responseFormat);
        unregisterClient();

        for (String conversation : conversationsJsonld.keySet()) {
            if (!conversationsJsonld.get(conversation).isEmpty()) {
                fail("Conversations map is not empty.");
            }
        }
        assertEquals("Error message(s) received from ERROR topic.", 0, errorTopicHandler.getNumberOfErrorMessages());
        TestUtils.checkIfRabbitQueuesEmpty(conf);

        logger.info("MW REST API integration test using {} response format finished successfully.", responseFormat);
    }

    private void listClients() {
        Response response = request("mw2mw/clients").get();
        List<Client> clients = response.readEntity(new GenericType<List<Client>>() {
        });

        assertEquals(1, clients.size());
        assertEquals(CLIENT_ID, clients.get(0).getClientId());
    }

    private Client registerClient(Client.ResponseFormat responseFormat) throws Exception {
        RegisterClientInput input = new RegisterClientInput();
        input.setClientId(CLIENT_ID);
        input.setResponseDelivery(Client.ResponseDelivery.SERVER_PUSH);
        input.setCallbackUrl(CALLBACK_URL);
        input.setResponseFormat(responseFormat);
        input.setReceivingCapacity(5);
        Entity<RegisterClientInput> entity = Entity.json(input);

        // register client
        Response response = request("mw2mw/clients").post(entity);
        URI locationUri = new URI(response.getHeaderString("Location"));
        Client client = response.readEntity(Client.class);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("/mw2mw/clients/myclient", locationUri.getPath());
        assertEquals(CLIENT_ID, client.getClientId());
        assertEquals(responseFormat, client.getResponseFormat());
        assertEquals(Client.ResponseDelivery.SERVER_PUSH, client.getResponseDelivery());
        assertEquals(CALLBACK_URL, client.getCallbackUrl().toString());
        assertEquals(5, (int) client.getReceivingCapacity());

        // try again
        Response responseConflict = request("mw2mw/clients").post(entity);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), responseConflict.getStatus());

        // get client
        Response responseGet = request(locationUri.getPath()).get();
        client = responseGet.readEntity(Client.class);
        assertEquals(Response.Status.OK.getStatusCode(), responseGet.getStatus());
        assertEquals(CLIENT_ID, client.getClientId());
        assertEquals(responseFormat, client.getResponseFormat());
        assertEquals(Client.ResponseDelivery.SERVER_PUSH, client.getResponseDelivery());
        assertEquals(CALLBACK_URL, client.getCallbackUrl().toString());
        assertEquals(5, (int) client.getReceivingCapacity());

        return client;
    }

    private Client updateClient() {
        // update client
        UpdateClientInput input = new UpdateClientInput();
        input.setReceivingCapacity(10);
        Entity<UpdateClientInput> entity = Entity.json(input);

        Response response = request("mw2mw/clients/" + CLIENT_ID).put(entity);
        Client client = response.readEntity(Client.class);

        assertEquals(200, response.getStatus());
        assertEquals(CLIENT_ID, client.getClientId());
        assertEquals(10, (int) client.getReceivingCapacity());

        return client;
    }

    private void unregisterClient() {
        Response response = request("mw2mw/clients/" + CLIENT_ID).delete();
        Response responseRetry = request("mw2mw/clients/" + CLIENT_ID).delete();

        assertEquals("Delete client response code should be 204", RESPONSE_CODE_204, response.getStatus());
        assertEquals("Second delete client response should be 404", RESPONSE_CODE_404, responseRetry.getStatus());
    }

    private void listPlatformTypes(Client.ResponseFormat responseFormat) throws Exception {
        Response response = request("mw2mw/platform-types").get();
        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();

        ResponseMessage responseMessageWrapper = waitForResponseMessage(conversationId, responseFormat);
        Set<URIManagerMessageMetadata.MessageTypesEnum> messageTypes = null;
        switch (responseFormat) {
            case JSON_LD:
                Message responseMessage = responseMessageWrapper.getMessageJSON_LD();
                messageTypes = responseMessage.getMetadata().getMessageTypes();
                assertFalse(responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().isPresent());

                EntityID entityID = new EntityID(URIManagerINTERMW.PREFIX.intermw + "platform-types");
                Set<EntityID> platformTypeIDs = responseMessage.getPayload().getEntityTypes(entityID);
                assertThat(platformTypeIDs.size(), is(1));
                assertThat(platformTypeIDs.iterator().next().toString(), is("http://inter-iot.eu/MWTestPlatform"));
                break;

            case JSON:
                ListPlatformTypesRes listPlatformTypesRes = (ListPlatformTypesRes) responseMessageWrapper.getMessageJSON();
                messageTypes = listPlatformTypesRes.getMessageTypes();
                assertNull(listPlatformTypesRes.getSenderPlatform());
                List<String> platformTypes = listPlatformTypesRes.getPlatformTypes();
                assertThat(platformTypes.size(), is(1));
                assertThat(platformTypes.get(0), is("http://inter-iot.eu/MWTestPlatform"));
                break;
        }

        assertTrue(messageTypes.containsAll(EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.LIST_SUPPORTED_PLATFORM_TYPES)));
    }

    private void registerPlatform(Client.ResponseFormat responseFormat) throws Exception {
        RegisterPlatformInput input = new RegisterPlatformInput();
        input.setPlatformId(PLATFORM_ID);
        input.setName("InterMW Test Platform");
        input.setType(PLATFORM_TYPE);
        input.setBaseEndpoint("http://localhost:4568");
        input.setLocationId("http://test.inter-iot.eu/TestLocation");

        String encryptedPassword = Hashing.sha256()
                .hashString("somepassword", StandardCharsets.UTF_8)
                .toString();
        input.setUsername("interiotuser");
        input.setEncryptedPassword(encryptedPassword);
        input.setEncryptionAlgorithm("SHA-256");

        // downstream alignment
        input.setDownstreamInputAlignmentName("");
        input.setDownstreamInputAlignmentVersion("");
        input.setDownstreamOutputAlignmentName("Test_Downstream_Alignment");
        input.setDownstreamOutputAlignmentVersion("1.0");

        // upstream alignment
        input.setUpstreamInputAlignmentName("Test_Upstream_Alignment");
        input.setUpstreamInputAlignmentVersion("1.0");
        input.setUpstreamOutputAlignmentName("");
        input.setUpstreamOutputAlignmentVersion("");

        Entity<RegisterPlatformInput> entity = Entity.json(input);

        Response response = request("mw2mw/platforms").post(entity);
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();

        ResponseMessage responseMessageWrapper = waitForResponseMessage(conversationId, responseFormat);
        Set<MessageTypesEnum> messageTypes = null;
        String senderPlatformId = null;
        switch (responseFormat) {
            case JSON_LD:
                Message responseMessage = responseMessageWrapper.getMessageJSON_LD();
                messageTypes = responseMessage.getMetadata().getMessageTypes();
                Optional<EntityID> senderPlatformEntityId = responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId();
                if (senderPlatformEntityId.isPresent()) {
                    senderPlatformId = senderPlatformEntityId.get().toString();
                }
                break;

            case JSON:
                PlatformRegisterRes platformRegisterRes = (PlatformRegisterRes) responseMessageWrapper.getMessageJSON();
                messageTypes = platformRegisterRes.getMessageTypes();
                senderPlatformId = platformRegisterRes.getSenderPlatform();
                break;
        }

        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
        assertTrue(messageTypes.containsAll(EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.PLATFORM_REGISTER)));
        assertThat(senderPlatformId, is(PLATFORM_ID));

        String platformIdEncoded = URLEncoder.encode(PLATFORM_ID, "UTF-8");
        Response response1 = request("mw2mw/platforms/" + platformIdEncoded).get();
        assertThat(response1.getStatus(), is(Response.Status.OK.getStatusCode()));
        Platform platform1 = response1.readEntity(Platform.class);
        assertThat(platform1.getPlatformId(), is(input.getPlatformId()));
        assertThat(platform1.getName(), is(input.getName()));
        assertThat(platform1.getType(), is(input.getType()));
        assertThat(platform1.getBaseEndpoint().toString(), is(input.getBaseEndpoint()));
        assertThat(platform1.getUsername(), is(input.getUsername()));
    }

    private void updatePlatform(Client.ResponseFormat responseFormat) throws Exception {
        UpdatePlatformInput input = new UpdatePlatformInput();
        input.setName("InterMW Test Platform 1");
        input.setBaseEndpoint("https://localhost:4568");
        input.setDownstreamOutputAlignmentVersion("1.0.1");
        input.setUpstreamInputAlignmentVersion("1.0.1");

        Entity<UpdatePlatformInput> entity = Entity.json(input);

        String platformIdEncoded = URLEncoder.encode(PLATFORM_ID, "UTF-8");
        Response response = request("mw2mw/platforms/" + platformIdEncoded).put(entity);
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();

        ResponseMessage responseMessageWrapper = waitForResponseMessage(conversationId, responseFormat);
        Set<MessageTypesEnum> messageTypes = null;
        String senderPlatformId = null;
        switch (responseFormat) {
            case JSON_LD:
                Message responseMessage = responseMessageWrapper.getMessageJSON_LD();
                messageTypes = responseMessage.getMetadata().getMessageTypes();
                senderPlatformId = responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString();
                break;

            case JSON:
                PlatformUpdateRes platformUpdateRes = (PlatformUpdateRes) responseMessageWrapper.getMessageJSON();
                messageTypes = platformUpdateRes.getMessageTypes();
                senderPlatformId = platformUpdateRes.getSenderPlatform();
                break;
        }

        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
        assertTrue(messageTypes.containsAll(EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.PLATFORM_UPDATE)));
        assertThat(senderPlatformId, is(PLATFORM_ID));

        Response response1 = request("mw2mw/platforms/" + platformIdEncoded).get();
        assertThat(response1.getStatus(), is(Response.Status.OK.getStatusCode()));
        Platform platform1 = response1.readEntity(Platform.class);
        assertThat(platform1.getPlatformId(), is(PLATFORM_ID));
        assertThat(platform1.getName(), is(input.getName()));
        assertThat(platform1.getBaseEndpoint().toString(), is(input.getBaseEndpoint()));
    }

    private void listPlatforms() {
        Response response = request("mw2mw/platforms").get();
        List<Platform> platforms = response.readEntity(new GenericType<List<Platform>>() {
        });

        assertEquals(1, platforms.size());
        Platform platform = platforms.get(0);
        assertEquals(11, platform.getPlatformStatistics().getDeviceCount());
        assertEquals(3, platform.getPlatformStatistics().getSubscribedDeviceCount());
        assertEquals(1, platform.getPlatformStatistics().getSubscriptionCount());
    }

    private void unregisterPlatform(Client.ResponseFormat responseFormat) throws Exception {
        String platformIdEncoded = URLEncoder.encode(PLATFORM_ID, "UTF-8");
        Response response = request("mw2mw/platforms/" + platformIdEncoded).delete();
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();

        ResponseMessage responseMessageWrapper = waitForResponseMessage(conversationId, responseFormat);
        Set<MessageTypesEnum> messageTypes = null;
        String senderPlatform = null;
        switch (responseFormat) {
            case JSON_LD:
                Message responseMessage = responseMessageWrapper.getMessageJSON_LD();
                messageTypes = responseMessage.getMetadata().getMessageTypes();
                senderPlatform = responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString();
                break;

            case JSON:
                PlatformUnregisterRes platformUnregisterRes = (PlatformUnregisterRes) responseMessageWrapper.getMessageJSON();
                messageTypes = platformUnregisterRes.getMessageTypes();
                senderPlatform = platformUnregisterRes.getSenderPlatform();
                break;
        }


        assertEquals("Unregister platform response code should be 202", RESPONSE_CODE_202, response.getStatus());
        assertTrue("UnregisterPlatform response messageTypes should contain RESPONSE type", messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue("UnregisterPlatform response messageTypes should contain PLATFORM_UNREGISTER type", messageTypes.contains(MessageTypesEnum.PLATFORM_UNREGISTER));
        assertEquals("UnregisterPlatform response should contain correct PLATFORM_ID", PLATFORM_ID, senderPlatform);

        // TODO make prettier
        // try to unregister already unregistered platform
        Response responseDuplicate = request("mw2mw/platforms/" + platformIdEncoded).delete();
        assertEquals(RESPONSE_CODE_404, responseDuplicate.getStatus());
    }

    private void registerDevices(Client.ResponseFormat responseFormat) throws Exception {
        List<IoTDevice> devices = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            IoTDevice ioTDevice = getIoTDevice(i);
            devices.add(ioTDevice);
        }
        PlatformCreateDeviceInput input = new PlatformCreateDeviceInput(devices);

        Entity<PlatformCreateDeviceInput> entity = Entity.json(input);
        Response response = request("mw2mw/devices").post(entity);
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();

        ResponseMessage responseMessageWrapper = waitForResponseMessage(conversationId, responseFormat);
        Set<MessageTypesEnum> messageTypes = null;
        String senderPlatform = null;
        switch (responseFormat) {
            case JSON_LD:
                Message responseMessage = responseMessageWrapper.getMessageJSON_LD();
                messageTypes = responseMessage.getMetadata().getMessageTypes();
                senderPlatform = responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString();
                break;
            case JSON:
                PlatformCreateDeviceRes platformCreateDeviceRes = (PlatformCreateDeviceRes) responseMessageWrapper.getMessageJSON();
                messageTypes = platformCreateDeviceRes.getMessageTypes();
                senderPlatform = platformCreateDeviceRes.getSenderPlatform();
                break;
        }

        assertEquals("Register device response code should be 202", RESPONSE_CODE_202, response.getStatus());
        assertTrue("PlatformCreateDevice response messageTypes should contain RESPONSE type",
                messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue("PlatformCreateDevice response messageTypes should contain PLATFORM_CREATE_DEVICE type",
                messageTypes.contains(MessageTypesEnum.PLATFORM_CREATE_DEVICE));
        assertEquals("PlatformCreateDevice response should contain correct PLATFORM_ID", PLATFORM_ID, senderPlatform);

        // try to register some of already registered devices
        List<IoTDevice> devices1 = new ArrayList<>();
        for (int i = 9; i <= 12; i++) {
            IoTDevice ioTDevice = getIoTDevice(i);
            devices1.add(ioTDevice);
        }
        PlatformCreateDeviceInput input1 = new PlatformCreateDeviceInput(devices1);
        Entity<PlatformCreateDeviceInput> entity1 = Entity.json(input1);
        Response response1 = request("mw2mw/devices").post(entity1);
        assertEquals(response1.getStatus(), 409);
    }

    private void updateDevices(Client.ResponseFormat responseFormat) throws Exception {
        // update device 11
        IoTDevice deviceToUpdate = new IoTDevice(DEVICE_ID_PREFIX + 11);
        deviceToUpdate.setName("Device " + 11 + " updated");
        deviceToUpdate.setHostedBy(PLATFORM_ID);
        deviceToUpdate.setLocation("http://test.inter-iot.eu/TestLocation1");

        Entity<IoTDevice> entity = Entity.json(deviceToUpdate);
        Response response = request("mw2mw/devices/{deviceId}", "deviceId", DEVICE_ID_PREFIX + "11")
                .put(entity);

        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();

        ResponseMessage responseMessageWrapper = waitForResponseMessage(conversationId, responseFormat);

        Set<MessageTypesEnum> messageTypes = null;
        String senderPlatformId = null;
        switch (responseFormat) {
            case JSON_LD:
                Message message = responseMessageWrapper.getMessageJSON_LD();
                messageTypes = message.getMetadata().getMessageTypes();
                senderPlatformId = message.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString();
                break;

            case JSON:
                PlatformUpdateDeviceRes platformUpdateDeviceRes = (PlatformUpdateDeviceRes) responseMessageWrapper.getMessageJSON();
                messageTypes = platformUpdateDeviceRes.getMessageTypes();
                senderPlatformId = platformUpdateDeviceRes.getSenderPlatform();
                break;
        }

        assertEquals("Update device response code should be 202", RESPONSE_CODE_202, response.getStatus());
        assertTrue("PlatformUpdateDevice response messageTypes should contain RESPONSE type",
                messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue("PlatformUpdateDevice response messageTypes should contain PLATFORM_UPDATE_DEVICE type",
                messageTypes.contains(MessageTypesEnum.PLATFORM_UPDATE_DEVICE));
        assertEquals("PlatformUpdateDevice response should contain correct PLATFORM_ID", PLATFORM_ID, senderPlatformId);

        // TODO check if devices really updated with new values!


    }

    private void deleteDevices(Client.ResponseFormat responseFormat) throws InterruptedException {
        // delete device 12
        Response response = request("mw2mw/devices/{deviceId}", "deviceId", DEVICE_ID_PREFIX + "12").delete();
        MwAsyncResponse mwAsyncResponse1 = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse1.getConversationId();

        ResponseMessage responseMessageWrapper = waitForResponseMessage(conversationId, responseFormat);

        Set<MessageTypesEnum> messageTypes = null;
        String senderPlatform = null;
        switch (responseFormat) {
            case JSON_LD:
                Message message = responseMessageWrapper.getMessageJSON_LD();
                messageTypes = message.getMetadata().getMessageTypes();
                senderPlatform = message.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString();
                break;

            case JSON:
                PlatformDeleteDeviceRes platformDeleteDeviceRes = (PlatformDeleteDeviceRes) responseMessageWrapper.getMessageJSON();
                messageTypes = platformDeleteDeviceRes.getMessageTypes();
                senderPlatform = platformDeleteDeviceRes.getSenderPlatform();
                break;
        }

        assertEquals("Delete device response code should be 202", RESPONSE_CODE_202, response.getStatus());
        assertTrue("PlatformDeleteDevice response messageTypes should contain RESPONSE type", messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue("PlatformDeleteDevice response messageTypes should contain PLATFORM_DELETE_DEVICE type", messageTypes.contains(MessageTypesEnum.PLATFORM_DELETE_DEVICE));
        assertEquals("PlatformDeleteDevice response should contain correct PLATFORM_ID", PLATFORM_ID, senderPlatform);
    }

    private void retrieveDevices() {
        // TODO pretty
        // retrieve devices
        Map<String, Object[]> queryParamMap = new HashMap<>();
        queryParamMap.put("platformId", new String[]{PLATFORM_ID});

        Response response = request("mw2mw/devices", queryParamMap).get();
        List<IoTDevice> devices = response.readEntity(new GenericType<List<IoTDevice>>() {
        });
        Map<String, IoTDevice> deviceMap = new HashMap<>();
        for (IoTDevice device : devices) {
            deviceMap.put(device.getDeviceId(), device);
        }
        assertEquals(devices.size(), 11);
        assertEquals(deviceMap.get(DEVICE_ID_PREFIX + "11").getName(), "Device 11 updated");
        assertFalse(deviceMap.containsKey(DEVICE_ID_PREFIX + "12"));
    }

    private String subscribe(Client.ResponseFormat responseFormat) throws Exception {
        SubscribeInput input = new SubscribeInput();
        for (int i = 1; i <= 3; i++) {
            input.addIoTDevice(DEVICE_ID_PREFIX + i);
        }
        Entity<SubscribeInput> entity = Entity.json(input);
        Response response = request("mw2mw/subscriptions").post(entity);
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();


        ResponseMessage responseMessageWrapper = waitForResponseMessage(conversationId, responseFormat);
        Set<MessageTypesEnum> messageTypes = null;
        String senderPlatformId = null;
        switch (responseFormat) {
            case JSON_LD:
                Message message = responseMessageWrapper.getMessageJSON_LD();
                messageTypes = message.getMetadata().getMessageTypes();
                senderPlatformId = message.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().orElse(null).toString();
                break;

            case JSON:
                SubscribeRes subscribeRes = (SubscribeRes) responseMessageWrapper.getMessageJSON();
                messageTypes = subscribeRes.getMessageTypes();
                senderPlatformId = subscribeRes.getSenderPlatform();
                break;
        }

        assertEquals("Subscribe response code should be 202", RESPONSE_CODE_202, response.getStatus());
        assertTrue("Subscribe response messageTypes should contain RESPONSE type", messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue("Subscribe response messageTypes should contain SUBSCRIBE type", messageTypes.contains(MessageTypesEnum.SUBSCRIBE));
        assertEquals("Subscribe response should contain correct PLATFORM_ID", PLATFORM_ID, senderPlatformId);

        return conversationId;
    }

    private void actuate(Client.ResponseFormat responseFormat) throws InterruptedException, UnsupportedEncodingException {
        ActuationInput actuationInput = new ActuationInput();
        actuationInput.setActuatorId(ACTUATOR_ID + 1);
        actuationInput.setActuatorLocalId(ACTUATOR_LOCAL_ID + 1);
        actuationInput.setActuationResultSet(Collections.singleton(new ActuationResult(PROPERTY_ID + 1, RESULT_VALUE, RESULT_UNIT)));

        Entity<ActuationInput> entity = Entity.json(actuationInput);
        String deviceId = URLEncoder.encode(DEVICE_ID_PREFIX + 1, "UTF-8");
        Response response = request("mw2mw/devices/" + deviceId + "/actuation").post(entity);

        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();


        ResponseMessage responseMessageWrapper = waitForResponseMessage(conversationId, responseFormat);
        Set<MessageTypesEnum> messageTypes = null;
        String senderPlatformId = null;

        switch (responseFormat) {
            case JSON_LD:
                Message message = responseMessageWrapper.getMessageJSON_LD();
                messageTypes = message.getMetadata().getMessageTypes();
                senderPlatformId = message.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().orElse(null).toString();
                break;

            case JSON:
                ActuationRes actuationRes = (ActuationRes) responseMessageWrapper.getMessageJSON();
                messageTypes = actuationRes.getMessageTypes();
                senderPlatformId = actuationRes.getSenderPlatform();
                break;
        }

        assertEquals("Actuation response code should be 202", RESPONSE_CODE_202, response.getStatus());
        assertTrue("Actuation response messageTypes should contain RESPONSE type", messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue("Actuation response messageTypes should contain ACTUATION type", messageTypes.contains(MessageTypesEnum.ACTUATION));
        assertEquals("Subscribe response should contain correct PLATFORM_ID", PLATFORM_ID, senderPlatformId);
    }


    private void checkObservations(String conversationId, Client.ResponseFormat responseFormat) throws Exception {

        for (int i = 0; i < 6; i++) {

            logger.debug("Checking observations on conversationId = " + conversationId + " in responseFormat = " + responseFormat.name());
            ResponseMessage responseMessage = waitForResponseMessage(conversationId, responseFormat);
            Set<MessageTypesEnum> messageTypes = null;
            String responseConversationId = null;
            String senderPlatformId = null;

            switch (responseFormat) {
                case JSON_LD:
                    Message message = responseMessage.getMessageJSON_LD();
                    messageTypes = message.getMetadata().getMessageTypes();
                    responseConversationId = message.getMetadata().getConversationId().get();
                    senderPlatformId = message.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString();

                    assertNotNull(message);
                    break;

                case JSON:
                    ObservationRes observationRes = (ObservationRes) responseMessage.getMessageJSON();
                    messageTypes = observationRes.getMessageTypes();
                    responseConversationId = observationRes.getConversationId();
                    senderPlatformId = observationRes.getSenderPlatform();

                    assertNotNull(observationRes);
                    break;
            }


            assertEquals("Observation response message conversationId should match expected conversationId",
                    conversationId, responseConversationId);
            assertTrue("Observation response messageTypes should contain OBSERVATION type",
                    messageTypes.contains(MessageTypesEnum.OBSERVATION));
            assertEquals("Observation response should contain correct PLATFORM_ID", PLATFORM_ID, senderPlatformId);
            logger.debug("Observation {} has been received.", i + 1);
        }
    }

    private void unsubscribe(String subscriptionId, Client.ResponseFormat responseFormat) throws Exception {
        Response response = request("mw2mw/subscriptions/" + subscriptionId).delete();
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();

        Set<MessageTypesEnum> messageTypes = null;

        ResponseMessage responseMessageWrapper = waitForResponseMessage(conversationId, responseFormat);
        String responseConversationId = null;
        String senderPlatformId = null;
        switch (responseFormat) {
            case JSON_LD:
                Message message = responseMessageWrapper.getMessageJSON_LD();
                messageTypes = message.getMetadata().getMessageTypes();
                responseConversationId = message.getMetadata().getConversationId().get();
                senderPlatformId = message.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString();
                break;

            case JSON:
                UnsubscribeRes unsubscribeRes = (UnsubscribeRes) responseMessageWrapper.getMessageJSON();
                messageTypes = unsubscribeRes.getMessageTypes();
                responseConversationId = unsubscribeRes.getConversationId();
                senderPlatformId = unsubscribeRes.getSenderPlatform();
                break;
        }


        assertEquals("Unsubscribe response message conversationId should match expected conversationId",
                conversationId, responseConversationId);
        assertEquals("Unsubscribe response code should be 202", RESPONSE_CODE_202, response.getStatus());
        assertTrue("Unsubscribe response messageTypes should contain RESPONSE type",
                messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue("Unsubscribe response messageTypes should contain UNSUBSCRIBE type",
                messageTypes.contains(MessageTypesEnum.UNSUBSCRIBE));
        assertEquals("Unsubscribe response should contain correct PLATFORM_ID", PLATFORM_ID, senderPlatformId);
    }

    private Service startClientListener(Client.ResponseFormat responseFormat) {
        Service spark = Service.ignite().port(CALLBACK_PORT);
        logger.debug("Client {} is listening on port {}.", CLIENT_ID, CALLBACK_PORT);

        spark.post("/endpoint", (request, response) -> {
            try {
                String content = request.body();
                logger.debug("New message pack has been received with content type {}:\n{}",
                        request.contentType(), content);

                assertTrue(content.startsWith("[") && content.endsWith("]"));

                if (responseFormat.equals(Client.ResponseFormat.JSON)) {
                    // JSON messages
                    assertEquals(request.contentType(), "application/json; charset=UTF-8");

                    // convert JSON string to JSONArray
                    JSONArray jsonArray = new JSONArray(content);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject messageJsonObject = jsonArray.getJSONObject(i);

                        String messageJsonString = messageJsonObject.toString();
                        BaseRes baseRes = objectMapper.readValue(messageJsonString, BaseRes.class);
                        String conversationId = baseRes.getConversationId();
                        if (!conversationsJson.containsKey(conversationId)) {
                            fail("Unexpected conversationId.");
                        }
                        conversationsJson.get(conversationId).add(ResponseMessageParser.castToMessageType(messageJsonString));
                    }

                } else if (responseFormat.equals(Client.ResponseFormat.JSON_LD)) {
                    // JSON_LD messages
                    assertEquals(request.contentType(), "application/ld+json; charset=UTF-8");
                    content = content.substring(1, content.length() - 2);

                    String[] rawMessagesArray = content.split("\\r?\\n,\\r?\\n");
                    logger.debug("Number of response messages received: {}", rawMessagesArray.length);
                    for (String rawMessage : rawMessagesArray) {
                        Message message = new Message(rawMessage);
                        String conversationId = message.getMetadata().getConversationId().get();
                        if (!conversationsJsonld.containsKey(conversationId)) {
                            fail("Unexpected conversationId.");
                        }
                        conversationsJsonld.get(conversationId).add(message);
                    }

                } else {
                    fail("Unexpected response format.");
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

    private ResponseMessage waitForResponseMessage(String conversationId, Client.ResponseFormat responseFormat) throws InterruptedException {
        assertNotNull(conversationId);

        switch (responseFormat) {
            case JSON:
                return new ResponseMessage(waitForJSONResponseMessage(conversationId), responseFormat);
            case JSON_LD:
                return new ResponseMessage(waitForJSONLDResponseMessage(conversationId), responseFormat);
            default:
                throw new RuntimeException("Unexpected response format.");
        }
    }

    private Message waitForJSONLDResponseMessage(String conversationId) throws InterruptedException {
        if (!conversationsJsonld.containsKey(conversationId)) {
            conversationsJsonld.put(conversationId, new LinkedBlockingQueue<>());
        }

        Message message = conversationsJsonld.get(conversationId).poll(TIMEOUT, TimeUnit.SECONDS);
        assertNotNull(message);
        assertEquals(message.getMetadata().getConversationId().get(), conversationId);
        assertFalse("ERROR response message received.",
                message.getMetadata().getMessageTypes().contains(MessageTypesEnum.ERROR));
        return message;
    }

    private BaseRes waitForJSONResponseMessage(String conversationId) throws InterruptedException {
        if (!conversationsJson.containsKey(conversationId)) {
            conversationsJson.put(conversationId, new LinkedBlockingQueue<>());
        }

        BaseRes message = conversationsJson.get(conversationId).poll(TIMEOUT, TimeUnit.SECONDS);
        assertNotNull(message);
        assertEquals(conversationId, message.getConversationId());
        assertFalse("ERROR response message received.",
                message.getMessageTypes().contains(MessageTypesEnum.ERROR));
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

    private Invocation.Builder request(String path, Map<String, Object[]> queryParams) {
        WebTarget target = target(path);

        for (Map.Entry<String, Object[]> queryParam : queryParams.entrySet()) {
            target = target.queryParam(queryParam.getKey(), queryParam.getValue());
        }
        return target.request();
    }

    private IoTDevice getIoTDevice(int i) {
        IoTDevice ioTDevice = new IoTDevice(DEVICE_ID_PREFIX + i);
        ioTDevice.setName("Device " + i);
        ioTDevice.setHostedBy(PLATFORM_ID);
        ioTDevice.setLocation("http://test.inter-iot.eu/TestLocation");
        return ioTDevice;
    }
}