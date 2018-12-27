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
import com.google.common.io.Resources;
import eu.interiot.intermw.api.InterMWInitializer;
import eu.interiot.intermw.api.InterMwApiImpl;
import eu.interiot.intermw.api.exception.BadRequestException;
import eu.interiot.intermw.api.exception.ConflictException;
import eu.interiot.intermw.api.exception.NotFoundException;
import eu.interiot.intermw.api.model.ActuationInput;
import eu.interiot.intermw.api.model.UpdateClientInput;
import eu.interiot.intermw.comm.ipsm.IPSMApiClient;
import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.ActuationResult;
import eu.interiot.intermw.commons.model.Client;
import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.integrationtests.utils.TestUtils;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.managers.URI.URIManagerINTERMW;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import eu.interiot.message.payload.types.IoTDevicePayload;
import org.apache.commons.io.Charsets;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

import static eu.interiot.intermw.commons.model.enums.IoTDeviceType.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class MwJavaApiIntegrationTest {
    private static final String CONFIG_FILE = "intermw.properties";
    private static final String PLATFORM_ID = "http://test.inter-iot.eu/test-platform1";
    private static final String PLATFORM_TYPE = "http://inter-iot.eu/MWTestPlatform";
    private static final String CLIENT_ID = "myclient";
    private static final String DEVICE_ID_PREFIX = "http://test.inter-iot.eu/device";
    private static final String LOCATION = "http://test.inter-iot.eu/TestLocation";
    private static final String ACTUATOR_ID = "http://test.inter-iot.eu/actuator";
    private static final String ACTUATOR_LOCAL_ID = "actuator";
    private static final String PROPERTY_ID = "http://test.inter-iot.eu/property";
    private static final String RESULT_VALUE = "value1";
    private static final String RESULT_UNIT = "http://test.inter-iot.eu/unit/celsius";
    private static final int TIMEOUT = 30;
    private static final Logger logger = LoggerFactory.getLogger(MwJavaApiIntegrationTest.class);
    private static final Set<MessageTypesEnum> UNEXPECTED_MESSAGE_TYPES = new HashSet<>();
    private static Configuration conf;
    private static InterMwApiImpl interMwApi;
    private Map<String, BlockingQueue<Message>> conversations;

    @BeforeClass
    public static void setUp() throws Exception {
        // Add message types which can come at any unexpected time - synchronous responses for now
        UNEXPECTED_MESSAGE_TYPES.add(MessageTypesEnum.QUERY);

        conf = new DefaultConfiguration(CONFIG_FILE);
        TestUtils.clearRabbitMQ(conf);
        TestUtils.clearParliament(conf);

        InterMWInitializer.initialize();
        interMwApi = new InterMwApiImpl(conf);
    }

    @Before
    public void before() throws Exception {
        TestUtils.clearParliament(conf);
    }

    @After
    public void after() throws Exception {
        if (conversations != null) {
            for (String conversation : conversations.keySet()) {
                if (!conversations.get(conversation).isEmpty()) {
                    fail("conversations map is not empty.");
                }
            }
            conversations.clear();
        }
        TestUtils.checkIfRabbitQueuesEmpty(conf);
    }

    @Test
    public void testWithoutAlignments() throws Exception {
        logger.info("testWithoutAlignments started.");

        Platform platform = new Platform();
        platform.setPlatformId(PLATFORM_ID);
        platform.setClientId(CLIENT_ID);
        platform.setName("InterMW Test Platform");
        platform.setType(PLATFORM_TYPE);
        platform.setBaseEndpoint(new URL("http://localhost:4568"));
        platform.setLocationId(LOCATION);
        String encryptedPassword = Hashing.sha256()
                .hashString("somepassword", StandardCharsets.UTF_8)
                .toString();
        platform.setUsername("interiotuser");
        platform.setEncryptedPassword(encryptedPassword);
        platform.setEncryptionAlgorithm("SHA-256");

        registerClient();
        Thread pullingThread = startPullingResponseMessages(CLIENT_ID);

        listPlatformTypes();

        registerPlatform(platform);
        updatePlatform(platform);

        registerDevices();
        updateDevices();
        actuate();
        query();
        String subscriptionId = subscribe();
        checkObservations(subscriptionId);
        unsubscribe(subscriptionId);

        deleteDevices();

        unregisterPlatform();
        unregisterClient();

        pullingThread.interrupt();

        logger.info("testWithoutAlignments finished successfully.");
    }

    @Test
    public void testWithAlignments() throws Exception {
        logger.info("testWithAlignments started.");

        uploadAlignments();

        Platform platform = new Platform();
        platform.setPlatformId(PLATFORM_ID);
        platform.setClientId(CLIENT_ID);
        platform.setName("InterMW Test Platform");
        platform.setType(PLATFORM_TYPE);
        platform.setBaseEndpoint(new URL("http://localhost:4568"));
        platform.setLocationId(LOCATION);
        String encryptedPassword = Hashing.sha256()
                .hashString("somepassword", StandardCharsets.UTF_8)
                .toString();
        platform.setUsername("interiotuser");
        platform.setEncryptedPassword(encryptedPassword);
        platform.setEncryptionAlgorithm("SHA-256");

        // downstream alignment
        platform.setDownstreamInputAlignmentName("");
        platform.setDownstreamInputAlignmentVersion("");
        platform.setDownstreamOutputAlignmentName("Test_Downstream_Alignment");
        platform.setDownstreamOutputAlignmentVersion("1.0");

        // upstream alignment
        platform.setUpstreamInputAlignmentName("Test_Upstream_Alignment");
        platform.setUpstreamInputAlignmentVersion("1.0");
        platform.setUpstreamOutputAlignmentName("");
        platform.setUpstreamOutputAlignmentVersion("");

        registerClient();
        Thread pullingThread = startPullingResponseMessages(CLIENT_ID);

        listPlatformTypes();

        registerPlatform(platform);
        updatePlatform(platform);

        registerDevices();
        updateDevices();
        actuate();
        query();
        String subscriptionId = subscribe();
        checkObservations(subscriptionId);
        unsubscribe(subscriptionId);

        deleteDevices();

        unregisterPlatform();
        unregisterClient();

        pullingThread.interrupt();

        logger.info("testWithAlignments finished successfully.");
    }

    private void uploadAlignments() throws Exception {
        logger.debug("Uploading test alignments to IPSM...");

        IPSMApiClient ipsmApiClient = new IPSMApiClient(conf.getIPSMApiBaseUrl());

        String downstreamAlignmentData = Resources.toString(
                Resources.getResource("alignments/test-downstream-alignment.rdf"), Charsets.UTF_8);
        ipsmApiClient.uploadAlignment(downstreamAlignmentData);

        // upload alignment with version set to 1.0.1
        downstreamAlignmentData = downstreamAlignmentData.replace("<exmo:version>1.0</exmo:version>", "<exmo:version>1.0.1</exmo:version>");
        ipsmApiClient.uploadAlignment(downstreamAlignmentData);

        String upstreamAlignmentData = Resources.toString(
                Resources.getResource("alignments/test-upstream-alignment.rdf"), Charsets.UTF_8);
        ipsmApiClient.uploadAlignment(upstreamAlignmentData);

        // upload alignment with version set to 1.0.1
        upstreamAlignmentData = upstreamAlignmentData.replace("<exmo:version>1.0</exmo:version>", "<exmo:version>1.0.1</exmo:version>");
        ipsmApiClient.uploadAlignment(upstreamAlignmentData);

        logger.debug("Alignments have been uploaded successfully.");
    }

    private void registerClient() throws Exception {
        Client client = new Client();
        client.setClientId(CLIENT_ID);
        client.setResponseDelivery(Client.ResponseDelivery.CLIENT_PULL);
        client.setResponseFormat(Client.ResponseFormat.JSON_LD);

        interMwApi.registerClient(client);

        // client is already registered, check that exception is thrown
        try {
            interMwApi.registerClient(client);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof ConflictException);
        }

        // update client
        UpdateClientInput updateClientInput = new UpdateClientInput();
        updateClientInput.setResponseFormat(Client.ResponseFormat.JSON);
        interMwApi.updateClient(CLIENT_ID, updateClientInput);
        Client client1 = interMwApi.getClient(CLIENT_ID);
        assertEquals(Client.ResponseFormat.JSON, client1.getResponseFormat());

        // update client back to old values
        updateClientInput.setResponseFormat(Client.ResponseFormat.JSON_LD);
        interMwApi.updateClient(CLIENT_ID, updateClientInput);
        client1 = interMwApi.getClient(CLIENT_ID);
        assertEquals(Client.ResponseFormat.JSON_LD, client1.getResponseFormat());
    }

    private void listPlatformTypes() throws Exception {
        String conversationId = interMwApi.listPlatformTypes(CLIENT_ID);
        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.containsAll(EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.LIST_SUPPORTED_PLATFORM_TYPES)));
        assertFalse(responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().isPresent());

        EntityID entityID = new EntityID(URIManagerINTERMW.PREFIX.intermw + "platform-types");
        Set<EntityID> platformTypes = responseMessage.getPayload().getEntityTypes(entityID);
        assertThat(platformTypes.size(), is(1));
        assertThat(platformTypes.iterator().next().toString(), is("http://inter-iot.eu/MWTestPlatform"));
    }

    private void registerPlatform(Platform platform) throws Exception {
        String conversationId = interMwApi.registerPlatform(platform);
        String platformId = platform.getPlatformId();

        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue(messageTypes.contains(MessageTypesEnum.PLATFORM_REGISTER));
        assertEquals(responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString(), platformId);

        Platform platform1 = interMwApi.getPlatform(platformId);
        assertThat(platform1.getPlatformId(), is(platform.getPlatformId()));
        assertThat(platform1.getName(), is(platform.getName()));
        assertThat(platform1.getType(), is(platform.getType()));

        // list platforms
        List<Platform> platforms = interMwApi.listPlatforms();
        assertEquals(platforms.size(), 1);
        assertEquals(platforms.get(0).getPlatformId(), platformId);
    }

    private void updatePlatform(Platform platform) throws Exception {
        platform.setName("InterMW Test Platform 1");
        String encryptedPassword = Hashing.sha256()
                .hashString("somepassword1", StandardCharsets.UTF_8)
                .toString();
        platform.setEncryptedPassword(encryptedPassword);
        if (!platform.getDownstreamOutputAlignmentVersion().isEmpty()) {
            platform.setDownstreamOutputAlignmentVersion("1.0.1");
        }
        if (!platform.getUpstreamInputAlignmentVersion().isEmpty()) {
            platform.setUpstreamInputAlignmentVersion("1.0.1");
        }

        String conversationId = interMwApi.updatePlatform(platform);
        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.containsAll(EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.PLATFORM_UPDATE)));
        assertThat(responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString(), is(PLATFORM_ID));

        // try to update platform type
        platform.setType("TEST");
        try {
            interMwApi.updatePlatform(platform);
            fail();

        } catch (BadRequestException e) {
            // this is correct behavior
        }

        Platform platform1 = interMwApi.getPlatform(PLATFORM_ID);
        assertThat(platform1.getName(), is(platform.getName()));
        assertThat(platform1.getEncryptedPassword(), is(platform.getEncryptedPassword()));
    }

    private void unregisterPlatform() throws Exception {
        String conversationId = interMwApi.removePlatform(CLIENT_ID, PLATFORM_ID);
        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue(messageTypes.contains(MessageTypesEnum.PLATFORM_UNREGISTER));
        assertEquals(responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString(), PLATFORM_ID);
    }

    private void registerDevices() throws Exception {
        List<IoTDevice> devices = new ArrayList<>();
        for (int i = 1; i <= 14; i++) {
            IoTDevice ioTDevice = new IoTDevice(DEVICE_ID_PREFIX + i);
            ioTDevice.setName("Device " + i);
            ioTDevice.setHostedBy(PLATFORM_ID);
            ioTDevice.setLocation(LOCATION);
            devices.add(ioTDevice);
        }
        String conversationId = interMwApi.platformCreateDevices(CLIENT_ID, devices);

        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue(messageTypes.contains(MessageTypesEnum.PLATFORM_CREATE_DEVICE));
        assertEquals(responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString(), PLATFORM_ID);
    }

    private void registerDeviceHierarchy() throws Exception {
        // Hub device, hosts sensors and actuators
        String hubDeviceId = DEVICE_ID_PREFIX + "hub";
        String hubDeviceName = "Hub device";
        String actuatorName = "Hub attached actuator";
        String sensorName = "Hub attached sensor";
        String standaloneSensorName = "Standalone sensor";
        String standaloneActuatorName = "Standalone actuator";
        List<IoTDevice> devices = new ArrayList<>();

        IoTDevice hubDevice = new IoTDevice(hubDeviceId, hubDeviceName, PLATFORM_ID, LOCATION, EnumSet.of(DEVICE));
        devices.add(hubDevice);

        // Sensor device, hosted on hub
        IoTDevice sensorDevice =
                new IoTDevice(DEVICE_ID_PREFIX + "sensor", sensorName, hubDeviceId, LOCATION, EnumSet.of(SENSOR));
        devices.add(sensorDevice);

        // Actuator device, hosted on hub
        IoTDevice actuatorDevice =
                new IoTDevice(DEVICE_ID_PREFIX + "actuator", actuatorName, hubDeviceId, LOCATION, EnumSet.of(ACTUATOR));
        devices.add(actuatorDevice);

        // Standalone sensor
        IoTDevice standaloneSensor =
                new IoTDevice(DEVICE_ID_PREFIX + "standalone_sensor", standaloneSensorName, PLATFORM_ID, LOCATION, EnumSet.of(DEVICE, SENSOR));
        devices.add(standaloneSensor);

        // Standalone actuator
        IoTDevice standaloneActuator =
                new IoTDevice(DEVICE_ID_PREFIX + "standalone_actuator", standaloneActuatorName, PLATFORM_ID, LOCATION, EnumSet.of(DEVICE, ACTUATOR));
        devices.add(standaloneActuator);

        String conversationId = interMwApi.platformCreateDevices(CLIENT_ID, devices);
        Message responseMessage = waitForResponseMessage(conversationId);
        assertNotNull(responseMessage);

        List<IoTDevice> ioTDevices = interMwApi.listDevices(CLIENT_ID, PLATFORM_ID);
        List<IoTDevice> ioTDevicesOnHubDevice = interMwApi.listDevices(CLIENT_ID, hubDeviceId);

        assertNotNull(ioTDevices);
        assertNotNull(ioTDevicesOnHubDevice);
        assertEquals("IoTDevices on platform should be 3", 3, ioTDevices.size());
        assertEquals("IoTDevices on device hub should be 2", 2, ioTDevicesOnHubDevice.size());

        // Group by id, so we can check each device individually
        Map<String, IoTDevice> iotDevicesById = new HashMap<>();
        for (IoTDevice device : ioTDevices) {
            iotDevicesById.put(device.getDeviceId(), device);
        }
        for (IoTDevice device : ioTDevicesOnHubDevice) {
            iotDevicesById.put(device.getDeviceId(), device);
        }

        IoTDevice hubDeviceAfter = iotDevicesById.get(hubDevice.getDeviceId());
        IoTDevice sensorDeviceAfter = iotDevicesById.get(sensorDevice.getDeviceId());
        IoTDevice actuatorDeviceAfter = iotDevicesById.get(actuatorDevice.getDeviceId());
        IoTDevice standaloneSensorAfter = iotDevicesById.get(standaloneSensor.getDeviceId());
        IoTDevice standaloneActuatorAfter = iotDevicesById.get(standaloneActuator.getDeviceId());

        assertNotNull(hubDeviceAfter);
        assertNotNull(sensorDeviceAfter);
        assertNotNull(actuatorDeviceAfter);
        assertNotNull(standaloneSensorAfter);
        assertNotNull(standaloneActuatorAfter);

        assertTrue(hubDeviceAfter.getDeviceTypes().contains(DEVICE));
        assertTrue(sensorDeviceAfter.getDeviceTypes().contains(SENSOR));
        assertTrue(actuatorDeviceAfter.getDeviceTypes().contains(ACTUATOR));
        assertTrue(standaloneSensorAfter.getDeviceTypes().contains(DEVICE));
        assertTrue(standaloneSensorAfter.getDeviceTypes().contains(SENSOR));
        assertTrue(standaloneActuatorAfter.getDeviceTypes().contains(DEVICE));
        assertTrue(standaloneActuatorAfter.getDeviceTypes().contains(ACTUATOR));
    }

    private void actuate() throws Exception {
        ActuationInput actuationInput = new ActuationInput();

        actuationInput.setActuatorId(ACTUATOR_ID + 1);
        actuationInput.setActuatorLocalId(ACTUATOR_LOCAL_ID + 1);
        Set<ActuationResult> actuationResultSet = new HashSet<>();

        actuationResultSet.add(new ActuationResult(PROPERTY_ID + 0, RESULT_VALUE + 0, RESULT_UNIT));
        actuationResultSet.add(new ActuationResult(PROPERTY_ID + 1, RESULT_VALUE + 1, RESULT_UNIT));
        actuationResultSet.add(new ActuationResult(PROPERTY_ID + 2, RESULT_VALUE + 2, RESULT_UNIT));

        actuationInput.setActuationResultSet(actuationResultSet);

        String conversationId = interMwApi.actuate(CLIENT_ID, DEVICE_ID_PREFIX + "1", actuationInput);

        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue(messageTypes.contains(MessageTypesEnum.ACTUATION));
        assertEquals(responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString(), PLATFORM_ID);
    }

    private void query() throws MiddlewareException, InterruptedException, IOException, ExecutionException, TimeoutException {
        Message syncResponse = interMwApi.getAllSensorData(CLIENT_ID, PLATFORM_ID);
        IoTDevicePayload syncIoTDevicePayload = syncResponse.getPayloadAsGOIoTPPayload().asIoTDevicePayload();
        Set<EntityID> syncIoTDevices = syncIoTDevicePayload.getIoTDevices();
        assertEquals("Sensor query should return correct amount of sensors in sync response", 10, syncIoTDevices.size());
    }

    private void updateDevices() throws Exception {
        // update devices 11 and 12
        List<IoTDevice> devicesToUpdate = new ArrayList<>();
        for (int i = 11; i <= 12; i++) {
            IoTDevice ioTDevice = new IoTDevice(DEVICE_ID_PREFIX + i);
            ioTDevice.setName("Device " + i + " updated");
            ioTDevice.setHostedBy(PLATFORM_ID);
            ioTDevice.setLocation("http://test.inter-iot.eu/TestLocation1");
            ioTDevice.addDeviceType(DEVICE);
            devicesToUpdate.add(ioTDevice);
        }

        String conversationId = interMwApi.platformUpdateDevices(CLIENT_ID, devicesToUpdate);

        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue(messageTypes.contains(MessageTypesEnum.PLATFORM_UPDATE_DEVICE));
        assertEquals(responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString(), PLATFORM_ID);

        // delete devices 13 and 14
        List<String> deviceIdsToDelete = new ArrayList<>();
        deviceIdsToDelete.add(DEVICE_ID_PREFIX + "13");
        deviceIdsToDelete.add(DEVICE_ID_PREFIX + "14");
        String conversationId1 = interMwApi.platformDeleteDevices(CLIENT_ID, deviceIdsToDelete);
        Message responseMessage1 = waitForResponseMessage(conversationId1);
        Set<MessageTypesEnum> messageTypes1 = responseMessage1.getMetadata().getMessageTypes();
        assertTrue(messageTypes1.contains(MessageTypesEnum.RESPONSE));
        assertTrue(messageTypes1.contains(MessageTypesEnum.PLATFORM_DELETE_DEVICE));
        assertEquals(responseMessage1.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString(), PLATFORM_ID);

        // retrieve devices
        List<IoTDevice> devices = interMwApi.listDevices(CLIENT_ID, PLATFORM_ID);
        assertEquals(12, devices.size());
        Map<String, IoTDevice> deviceMap = new HashMap<>();
        for (IoTDevice device : devices) {
            deviceMap.put(device.getDeviceId(), device);
        }
        assertEquals(deviceMap.get(DEVICE_ID_PREFIX + "11").getName(), "Device 11 updated");
        assertEquals(deviceMap.get(DEVICE_ID_PREFIX + "12").getName(), "Device 12 updated");
        assertFalse(deviceMap.containsKey(DEVICE_ID_PREFIX + "13"));
        assertFalse(deviceMap.containsKey(DEVICE_ID_PREFIX + "14"));
    }

    private String subscribe() throws Exception {
        List<String> deviceIds = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            String deviceId = DEVICE_ID_PREFIX + i;
            deviceIds.add(deviceId);
        }

        String conversationId = interMwApi.subscribe(CLIENT_ID, deviceIds);

        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue(messageTypes.contains(MessageTypesEnum.SUBSCRIBE));
        assertEquals(responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString(), PLATFORM_ID);
        return conversationId;
    }

    private void checkObservations(String conversationId) throws Exception {

        for (int i = 0; i < 6; i++) {
            Message observationMessage = waitForResponseMessage(conversationId);
            Set<MessageTypesEnum> messageTypes = observationMessage.getMetadata().getMessageTypes();
            assertFalse(messageTypes.contains(MessageTypesEnum.RESPONSE));
            assertTrue(messageTypes.contains(MessageTypesEnum.OBSERVATION));
            assertEquals(observationMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString(), PLATFORM_ID);
        }
    }

    private void unsubscribe(String subscriptionId) throws Exception {
        String conversationId = interMwApi.unsubscribe(CLIENT_ID, subscriptionId);
        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.contains(MessageTypesEnum.RESPONSE));
        assertTrue(messageTypes.contains(MessageTypesEnum.UNSUBSCRIBE));
        assertEquals(responseMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString(), PLATFORM_ID);
    }

    private void unregisterClient() throws Exception {
        interMwApi.removeClient(CLIENT_ID);

        try {
            interMwApi.removeClient(CLIENT_ID);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof NotFoundException);
        }
    }

    private Thread startPullingResponseMessages(String clientId) {
        conversations = new HashMap<>();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        Message message = interMwApi.retrieveResponseMessage(clientId, -1);
                        if (message == null) {
                            break; // thread was interrupted
                        }
                        String conversationId = message.getMetadata().getConversationId().get();
                        if (!conversations.containsKey(conversationId)) {
                            logger.error("Message with unexpected conversationId received:\n{}", message.serializeToJSONLD());

                            if (!Collections.disjoint(UNEXPECTED_MESSAGE_TYPES, message.getMetadata().getMessageTypes())) {
                                conversations.put(conversationId, new LinkedBlockingQueue<>());
                            } else {
                                fail("Message with unexpected conversationId received.");
                            }
                        }
                        logger.debug("New message received with conversationId {} and type {}.",
                                conversationId, message.getMetadata().getMessageTypes());
                        conversations.get(conversationId).add(message);
                    } catch (Exception e) {
                        logger.error("Failed to retrieve message.", e);
                        fail();
                    }
                } while (!Thread.interrupted());
                logger.debug("Thread for pulling response messages has stopped.");
            }
        };
        Thread respMessagesPullingThread = new Thread(runnable);
        respMessagesPullingThread.start();
        return respMessagesPullingThread;
    }

    private Message waitForResponseMessage(String conversationId) throws InterruptedException {
        logger.debug("Waiting for response message with conversationId {}...", conversationId);
        if (!conversations.containsKey(conversationId)) {
            logger.debug("Added conversationId {} to conversations map.", conversationId);
            conversations.put(conversationId, new LinkedBlockingQueue<>());
        }

        Message message = conversations.get(conversationId).poll(TIMEOUT, TimeUnit.SECONDS);
        assertNotNull(message);
        assertEquals(message.getMetadata().getConversationId().get(), conversationId);

        if (message.getMetadata().getMessageTypes().contains(MessageTypesEnum.ERROR)) {
            fail("ERROR response message received: " + message.getMetadata().asErrorMessageMetadata().printErrorDescription());
        }
        return message;
    }

    private void deleteDevices() throws Exception {
        List<String> deviceIds = new ArrayList<>();
        deviceIds.add("http://test.inter-iot.eu/device1");
        deviceIds.add("http://test.inter-iot.eu/device2");
        deviceIds.add("http://test.inter-iot.eu/device3");
        deviceIds.add("http://test.inter-iot.eu/device4");
        deviceIds.add("http://test.inter-iot.eu/device5");
        deviceIds.add("http://test.inter-iot.eu/device6");
        deviceIds.add("http://test.inter-iot.eu/device7");
        deviceIds.add("http://test.inter-iot.eu/device8");
        deviceIds.add("http://test.inter-iot.eu/device9");
        deviceIds.add("http://test.inter-iot.eu/device10");
        deviceIds.add("http://test.inter-iot.eu/device11");
        deviceIds.add("http://test.inter-iot.eu/device12");

        try {
            String conversationId = interMwApi.platformDeleteDevices(CLIENT_ID, deviceIds);
            Message responseMessage = waitForResponseMessage(conversationId);
            Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
            assertTrue(messageTypes.contains(MessageTypesEnum.RESPONSE));
            assertTrue(messageTypes.contains(MessageTypesEnum.PLATFORM_DELETE_DEVICE));

        } catch (MiddlewareException | BadRequestException e) {
            logger.error("Failed to delete devices", e);
            fail();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
