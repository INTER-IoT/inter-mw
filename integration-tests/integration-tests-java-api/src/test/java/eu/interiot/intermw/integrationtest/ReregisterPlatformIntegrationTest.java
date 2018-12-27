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
import eu.interiot.intermw.api.InterMwApiImpl;
import eu.interiot.intermw.api.exception.NotFoundException;
import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.model.Client;
import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.integrationtests.utils.TestUtils;
import eu.interiot.message.Message;
import eu.interiot.message.exceptions.MessageException;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ReregisterPlatformIntegrationTest {
    private static final String CONFIG_FILE = "intermw.properties";
    private static final String PLATFORM_ID = "http://test.inter-iot.eu/test-platform1";
    private static final String CLIENT_ID = "myclient";
    private static final String DEVICE_ID_PREFIX = "http://test.inter-iot.eu/device";
    private static final String LOCATION = "http://test.inter-iot.eu/TestLocation";
    private static final int TIMEOUT = 30;
    private static final Logger logger = LoggerFactory.getLogger(ReregisterPlatformIntegrationTest.class);
    private DefaultConfiguration conf;
    private InterMwApiImpl interMwApi;
    private Map<String, BlockingQueue<Message>> conversations = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        conf = new DefaultConfiguration(CONFIG_FILE);
        TestUtils.clearParliament(conf);
        TestUtils.clearRabbitMQ(conf);

        InterMWInitializer.initialize();
    }

    @Test
    public void testPlatformReregistration() throws Exception {
        logger.info("Platform re-registration integration test has started.");
        interMwApi = new InterMwApiImpl(conf);

        registerClient();

        // register platform and subscribe, iteration #1
        registerPlatform();
        registerDevices();
        String conversationId = subscribe();
        checkObservations(conversationId);
        unsubscribe(conversationId);
        unregisterPlatform();

        // register platform and subscribe, iteration #2
        registerPlatform();
        conversationId = subscribe();
        checkObservations(conversationId);
        unsubscribe(conversationId);
        unregisterPlatform();

        unregisterClient();
        logger.info("Platform re-registration integration test finished successfully.");
    }

    private void registerClient() throws Exception {
        Client client = new Client();
        client.setClientId(CLIENT_ID);
        client.setResponseDelivery(Client.ResponseDelivery.CLIENT_PULL);
        client.setResponseFormat(Client.ResponseFormat.JSON_LD);

        interMwApi.registerClient(client);
        startPullingResponseMessages(CLIENT_ID);

        Client client1 = interMwApi.getClient(CLIENT_ID);
        assertThat(client1.getClientId(), is(CLIENT_ID));
    }

    private void registerPlatform() throws Exception {
        Platform platform = new Platform();
        platform.setPlatformId(PLATFORM_ID);
        platform.setClientId(CLIENT_ID);
        platform.setName("InterMW Test Platform");
        platform.setType("http://inter-iot.eu/MWTestPlatform");
        platform.setBaseEndpoint(new URL("http://localhost:4568"));
        platform.setLocationId(LOCATION);

        String encryptedPassword = Hashing.sha256()
                .hashString("somepassword", StandardCharsets.UTF_8)
                .toString();
        platform.setUsername("interiotuser");
        platform.setEncryptedPassword(encryptedPassword);
        platform.setEncryptionAlgorithm("SHA-256");

        String conversationId = interMwApi.registerPlatform(platform);
        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.containsAll(EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.PLATFORM_REGISTER)));

        // get platforms
        List<Platform> platforms = interMwApi.listPlatforms();
        assertThat(platforms.size(), is(1));
        assertThat(platforms.get(0).getPlatformId(), is(PLATFORM_ID));
    }

    private void unregisterPlatform() throws Exception {
        String conversationId = interMwApi.removePlatform(CLIENT_ID, PLATFORM_ID);
        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.containsAll(EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.PLATFORM_UNREGISTER)));

        // get platforms
        List<Platform> platforms = interMwApi.listPlatforms();
        assertThat(platforms.size(), is(0));
    }

    private void registerDevices() throws Exception {
        List<IoTDevice> devices = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            IoTDevice ioTDevice = new IoTDevice(DEVICE_ID_PREFIX + i);
            ioTDevice.setName("Device #" + i);
            ioTDevice.setHostedBy(PLATFORM_ID);
            ioTDevice.setLocation(LOCATION);
            devices.add(ioTDevice);
        }
        String conversationId = interMwApi.platformCreateDevices(CLIENT_ID, devices);

        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.containsAll(EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.PLATFORM_CREATE_DEVICE)));
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
        assertTrue(messageTypes.containsAll(EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.SUBSCRIBE)));
        return conversationId;
    }

    private void checkObservations(String conversationId) throws Exception {

        for (int i = 0; i < 6; i++) {
            Message observationMessage = waitForResponseMessage(conversationId);
            Set<MessageTypesEnum> messageTypes = observationMessage.getMetadata().getMessageTypes();
            assertTrue(messageTypes.equals(EnumSet.of(MessageTypesEnum.OBSERVATION)));
            assertThat(observationMessage.getMetadata().asPlatformMessageMetadata().getSenderPlatformId().get().toString(), is(PLATFORM_ID));
        }
    }

    private void unsubscribe(String subscriptionId) throws Exception {
        String conversationId = interMwApi.unsubscribe(CLIENT_ID, subscriptionId);
        Message responseMessage = waitForResponseMessage(conversationId);
        Set<MessageTypesEnum> messageTypes = responseMessage.getMetadata().getMessageTypes();
        assertTrue(messageTypes.containsAll(EnumSet.of(MessageTypesEnum.RESPONSE, MessageTypesEnum.UNSUBSCRIBE)));
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

    private void startPullingResponseMessages(String clientId) throws IOException, MessageException, InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        Message message = interMwApi.retrieveResponseMessage(clientId, -1);
                        String conversationId = message.getMetadata().getConversationId().get();
                        if (!conversations.containsKey(conversationId)) {
                            fail("Unexpected conversationId.");
                        }
                        logger.debug("New message received with conversationId {} and type {}.",
                                conversationId, message.getMetadata().getMessageTypes());
                        conversations.get(conversationId).add(message);
                    } catch (Exception e) {
                        logger.error("Failed to retrieve message.", e);
                        fail();
                    }
                } while (true);
            }
        };
        new Thread(runnable).start();
    }

    private Message waitForResponseMessage(String conversationId) throws InterruptedException {
        if (!conversations.containsKey(conversationId)) {
            conversations.put(conversationId, new LinkedBlockingQueue<>());
        }

        Message message = conversations.get(conversationId).poll(TIMEOUT, TimeUnit.SECONDS);
        assertNotNull(message);
        assertThat(message.getMetadata().getConversationId().get(), is(conversationId));
        return message;
    }
}
