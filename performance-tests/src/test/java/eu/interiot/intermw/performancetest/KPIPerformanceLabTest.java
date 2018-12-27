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
package eu.interiot.intermw.performancetest;

import eu.interiot.intermw.api.InterMWInitializer;
import eu.interiot.intermw.api.model.PlatformCreateDeviceInput;
import eu.interiot.intermw.api.model.RegisterClientInput;
import eu.interiot.intermw.api.model.RegisterPlatformInput;
import eu.interiot.intermw.api.model.SubscribeInput;
import eu.interiot.intermw.api.rest.model.MwAsyncResponse;
import eu.interiot.intermw.api.rest.resource.InterMwApiREST;
import eu.interiot.intermw.api.rest.resource.InterMwExceptionMapper;
import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.model.Client;
import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.integrationtests.utils.TestUtils;
import eu.interiot.message.Message;
import eu.interiot.message.exceptions.MessageException;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.json.JSONArray;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KPIPerformanceLabTest extends JerseyTest {
    private static final Logger logger = LoggerFactory.getLogger(KPIPerformanceLabTest.class);
    private static final String CONFIG_FILE = "intermw.properties";
    private static final String PLATFORM_ID = "http://test.inter-iot.eu/test-platform1";
    private static final String CLIENT_ID = "myclient";
    private static final String DEVICE_ID_PREFIX = "http://test.inter-iot.eu/device";
    private static final String INSTRUCTIONS_FILE = "instructions.properties";
    private static final String CSV_DIRECTORY = System.getProperty("user.dir") + "/target/";
    private static final int TIMEOUT = 30;
    private Map<String, BlockingQueue<Message>> conversations = new HashMap<>();

    @BeforeClass
    public static void beforeClass() throws Exception {
        DefaultConfiguration conf = new DefaultConfiguration(CONFIG_FILE);
        TestUtils.clearRabbitMQ(conf);
        TestUtils.clearParliament(conf);
        TestUtils.uploadAlignments(conf);

        InterMWInitializer.initialize();
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
        Properties properties = KPITestUtils.loadProperties(INSTRUCTIONS_FILE);

        int experimentNumber = Integer.valueOf(properties.getProperty("test"));
        int frequency = Integer.valueOf(properties.getProperty("test-message-frequency"));
        int testDuration = Integer.valueOf(properties.getProperty("test-duration"));
        int platformNumber = Integer.valueOf(properties.getProperty("test-platforms"));
        boolean alignments = Boolean.valueOf(properties.getProperty("test-alignments"));

        startPullingResponseMessagesServerPush(5000);
        registerClient();
        for (int i = 1; i <= platformNumber; i++) {
            registerPlatform(i, alignments);
        }
        registerDevices(platformNumber);

        MWPerformanceTestBridge.setMessageGenerationInterval(1000 / frequency);
        String conversationId = subscribe(platformNumber);
        waitForJSONLDResponseMessage(conversationId);
        checkObservations(conversationId, testDuration, experimentNumber, alignments);
    }

    private void registerClient() {
        RegisterClientInput input = new RegisterClientInput();
        input.setClientId(CLIENT_ID);
        input.setResponseDelivery(Client.ResponseDelivery.SERVER_PUSH);
        input.setCallbackUrl("http://localhost:5000");
        input.setResponseFormat(Client.ResponseFormat.JSON_LD);
        input.setReceivingCapacity(1000);
        Entity<RegisterClientInput> entity = Entity.json(input);

        request("mw2mw/clients").post(entity);
    }

    private void registerPlatform(int platformNumber, boolean alignments) throws Exception {
        RegisterPlatformInput input = new RegisterPlatformInput();
        input.setPlatformId(PLATFORM_ID + platformNumber);
        input.setName("InterMW Test Platform " + platformNumber);
        input.setType("http://inter-iot.eu/MWPerformanceTestPlatform");
        input.setBaseEndpoint("http://localhost:4568");
        input.setLocationId("http://test.inter-iot.eu/TestLocation");

        if (alignments) {
            input.setDownstreamInputAlignmentName("");
            input.setDownstreamInputAlignmentVersion("");
            input.setDownstreamOutputAlignmentName("Test_Downstream_Alignment");
            input.setDownstreamOutputAlignmentVersion("1.0.1");

            input.setUpstreamInputAlignmentName("Test_Upstream_Alignment");
            input.setUpstreamInputAlignmentVersion("1.0.1");
            input.setUpstreamOutputAlignmentName("");
            input.setUpstreamOutputAlignmentVersion("");
        }

        Entity<RegisterPlatformInput> entity = Entity.json(input);

        Response response = request("mw2mw/platforms").post(entity);
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        String conversationId = mwAsyncResponse.getConversationId();

        waitForJSONLDResponseMessage(conversationId);
    }

    private void registerDevices(int platformNumber) throws Exception {
        for (int i = 1; i <= platformNumber; i++) {
            List<IoTDevice> devices = new ArrayList<>();
            IoTDevice ioTDevice = getIoTDevice(i);
            devices.add(ioTDevice);
            PlatformCreateDeviceInput input = new PlatformCreateDeviceInput(devices);

            Entity<PlatformCreateDeviceInput> entity = Entity.json(input);
            Response response = request("mw2mw/devices").post(entity);
            MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
            String conversationId = mwAsyncResponse.getConversationId();

            waitForJSONLDResponseMessage(conversationId);
        }
    }

    private String subscribe(int platformNumber) {
        SubscribeInput input = new SubscribeInput();
        for (int i = 1; i <= platformNumber; i++) {
            input.addIoTDevice(DEVICE_ID_PREFIX + i);
        }
        Entity<SubscribeInput> entity = Entity.json(input);
        Response response = request("mw2mw/subscriptions").post(entity);
        MwAsyncResponse mwAsyncResponse = response.readEntity(MwAsyncResponse.class);
        return mwAsyncResponse.getConversationId();
    }

    private void checkObservations(String conversationId, int duration, int experimentNumber, boolean alignments) throws Exception {
        File file;
        if (alignments) {
            file = new File(CSV_DIRECTORY + "/KPILabTestWithIPSM" + experimentNumber + "Raw.csv");
        } else {
            file = new File(CSV_DIRECTORY + "/KPILabTest" + experimentNumber + "Raw.csv");
        }
        KPITestUtils.CreateCSVFile(file);

        final PrintWriter writer = new PrintWriter(file);
        writer.println("Message time, Message consumed time, Diff");
        logger.info("Started writing csv file KPITests" + experimentNumber + "Raw.csv");

        long durationMillis = (duration + TIMEOUT) * 1000;
        long durationMillisCheck = duration * 1000;
        long timeStart = System.currentTimeMillis();
        long timeEnd = System.currentTimeMillis();
        long consumedMessagesCounter = 0;

        while ((timeEnd - timeStart) < durationMillis) {
            Message message = waitForJSONLDResponseMessage(conversationId);
            if (message.getMetadata().getMessageTypes().contains(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION)) {
                consumedMessagesCounter++;

                long timeMetadata = message.getMetadata().getDateTimeStamp().get().getTime().getTime();
                long timeNow = System.currentTimeMillis();
                long millisString = timeNow - timeMetadata;
                writer.println(timeMetadata + ", " + timeNow + ", " + millisString);

                timeEnd = System.currentTimeMillis();
                if ((timeEnd - timeStart) > durationMillisCheck) {
                    writer.close();
                    logger.info("Test lasted {} ms. Test bridge generated {} observations and client read {} of those observations.",
                            (timeEnd - timeStart), MWPerformanceTestBridge.getGeneratedMessageCounter(), consumedMessagesCounter);
                    break;
                }
            } else {
                logger.debug("Received message with message type: {}", message.getMetadata().getMessageTypes().toString());
            }
        }
    }

    private void startPullingResponseMessagesServerPush(int port) throws Exception {
        Server server = new Server(port);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
                StringWriter writer = new StringWriter();
                IOUtils.copy(request.getInputStream(), writer);

                try {
                    Message message = new Message(writer.toString());
                    addMessageToConversations(message);
                } catch (MessageException e) {
                    logger.debug("Received BULK Message \n {} \n splitting it into multiple messages...", writer.toString());
                    try {
                        JSONArray messages = new JSONArray(writer.toString());
                        for (int i = 0; i < messages.length(); i++) {
                            Message message = new Message(messages.get(i).toString());
                            addMessageToConversations(message);
                        }
                    } catch (MessageException e1) {
                        logger.warn("Failed to convert JSON-LD message to Message object. \n {}", writer.toString());
                    }
                }
            }
        });
        server.start();
    }

    private void addMessageToConversations(Message message) throws MessageException {
        String conversationId = message.getMetadata().getConversationId().get();
        if (!conversations.containsKey(conversationId)) {
            throw new MessageException("Unexpected conversationId.");
        }
        logger.debug("New message has been retrieved of type {} with conversationId {}.",
                message.getMetadata().getMessageTypes(), conversationId);
        conversations.get(conversationId).add(message);
    }

    private Message waitForJSONLDResponseMessage(String conversationId) throws InterruptedException {
        if (!conversations.containsKey(conversationId)) {
            conversations.put(conversationId, new LinkedBlockingQueue<>());
        }

        return conversations.get(conversationId).poll(TIMEOUT, TimeUnit.SECONDS);
    }

    private Invocation.Builder request(String path) {
        return target(path)
                .request()
                .header("Client-ID", CLIENT_ID);
    }

    private IoTDevice getIoTDevice(int i) {
        IoTDevice ioTDevice = new IoTDevice(DEVICE_ID_PREFIX + i);
        ioTDevice.setName("Device " + i);
        ioTDevice.setHostedBy(PLATFORM_ID + i);
        ioTDevice.setLocation("http://test.inter-iot.eu/TestLocation");
        return ioTDevice;
    }
}