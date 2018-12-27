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
import eu.interiot.intermw.integrationtests.utils.TestUtils;
import eu.interiot.message.Message;
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gasper Vrhovsek
 */
public class AddOrRemoveDeviceIntegrationTest extends JerseyTest {

    private static final String CONFIG_FILE = "intermw.properties";
    private static final String PLATFORM_ID = "http://test.inter-iot.eu/test-platform_add_remove_discovery";
    private static final String CLIENT_ID = "myclient";
    private static final int TIMEOUT = 30;
    private static final Logger logger = LoggerFactory.getLogger(AddOrRemoveDeviceIntegrationTest.class);
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
    public void clearParliament() throws Exception {
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
        logger.info("---- CLIENT = " + client.getClientId());

        startPullingResponseMessages();
        Client.ResponseFormat responseFormat = client.getResponseFormat();

        registerPlatform(responseFormat);

        Thread.sleep(10000);

        retrieveDevices();
        logger.info("AddOrRemoveDeviceIntegrationTest finished successfully.");
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
        assertEquals(5, devices.size());

        // TODO more asserts
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
