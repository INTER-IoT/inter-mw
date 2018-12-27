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

import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.exceptions.MessageException;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.payload.types.ObservationPayload;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class KPIPerformanceLogPTest {
    private static final Logger logger = LoggerFactory.getLogger(KPIPerformanceLabTest.class);
    private static final String CSV_DIRECTORY = System.getProperty("user.dir") + "/target/";
    private static final String INSTRUCTIONS_FILE = "liveInstructions.properties";
    private static int TEST_DURATION;
    private static String CLIENT_ID;
    private static String INTERMW_REST_API_URL_PREFIX;
    private Map<String, BlockingQueue<Message>> conversations = new HashMap<>();

    @Test
    public void test() throws Exception {
        logger.info("Reading properties...");
        Properties properties = KPITestUtils.loadProperties(INSTRUCTIONS_FILE);

        INTERMW_REST_API_URL_PREFIX = properties.getProperty("intermw-url-path");
        CLIENT_ID = properties.getProperty("intermw-client");
        TEST_DURATION = Integer.valueOf(properties.getProperty("test-duration"));
        int CLIENT_PORT = Integer.valueOf(properties.getProperty("client-port"));

        logger.info("Started KPI live Test.");
        startPullingResponseMessagesServerPush(CLIENT_PORT);

        String conversationId = subscribe(listDevices(listPlatforms()));
        waitForJSONLDResponseMessage(conversationId);

        checkObservations(conversationId);

        unsubscribe(conversationId);
        logger.info("Test finished.");
    }

    private List<String> listPlatforms() throws IOException {
        StringWriter writer = new StringWriter();
        List<String> platforms = new ArrayList<>();

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(INTERMW_REST_API_URL_PREFIX + "/mw2mw/platforms");
        httpGet.addHeader("Client-ID", CLIENT_ID);

        HttpResponse httpResponse = httpClient.execute(httpGet);
        IOUtils.copy(httpResponse.getEntity().getContent(), writer);
        JSONArray jsonArray = new JSONArray(writer.toString());

        for (int i = 0; i < jsonArray.length(); i++) {
            platforms.add(jsonArray.getJSONObject(i).getString("platformId"));
        }
        return platforms;
    }

    private List<String> listDevices(List<String> platforms) throws IOException {
        StringWriter writer = new StringWriter();
        List<String> devices = new ArrayList<>();

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(INTERMW_REST_API_URL_PREFIX + "/mw2mw/devices?" + URLEncoder.encode(platforms.get(0), "UTF-8"));
        httpGet.addHeader("Client-ID", CLIENT_ID);

        HttpResponse httpResponse = httpClient.execute(httpGet);
        IOUtils.copy(httpResponse.getEntity().getContent(), writer);
        JSONArray jsonResponseMessage = new JSONArray(writer.toString());

        for (int i = 0; i < jsonResponseMessage.length(); i++) {
            devices.add(jsonResponseMessage.getJSONObject(i).getString("deviceId"));
        }
        return devices;
    }

    private String subscribe(List<String> devices) throws Exception {
        StringWriter writer = new StringWriter();
        JSONObject jsonDevices = new JSONObject();
        jsonDevices.put("deviceIds", devices);

        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(INTERMW_REST_API_URL_PREFIX + "/mw2mw/subscriptions");
        httpPost.addHeader("Client-ID", CLIENT_ID);
        httpPost.setEntity(new StringEntity(jsonDevices.toString(), ContentType.APPLICATION_JSON));

        HttpResponse httpResponse = httpClient.execute(httpPost);
        IOUtils.copy(httpResponse.getEntity().getContent(), writer);

        JSONObject jsonResponseMessage = new JSONObject(writer.toString());
        return jsonResponseMessage.getString("conversationId");
    }

    private String unsubscribe(String conversationId) throws IOException {
        StringWriter writer = new StringWriter();

        HttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(INTERMW_REST_API_URL_PREFIX + "/mw2mw/subscriptions/" + conversationId);
        httpDelete.addHeader("Client-ID", CLIENT_ID);

        HttpResponse httpResponse = httpClient.execute(httpDelete);
        IOUtils.copy(httpResponse.getEntity().getContent(), writer);
        JSONObject jsonResponseMessage = new JSONObject(writer.toString());

        return jsonResponseMessage.getString("conversationId");
    }

    private void checkObservations(String conversationId) throws Exception {
        File file = new File(CSV_DIRECTORY + "/KPILogPTestRaw.csv");
        KPITestUtils.CreateCSVFile(file);

        final PrintWriter writer = new PrintWriter(file);
        writer.println("Message time, Message consumed time, Diff, DeviceId");
        logger.info("Checking observations.");

        long durationMillis = (TEST_DURATION + 30) * 1000;
        long timeStart = System.currentTimeMillis();
        long timeEnd = System.currentTimeMillis();

        while ((timeEnd - timeStart) < durationMillis) {
            Message message = waitForJSONLDResponseMessage(conversationId);

            if (message.getMetadata().getMessageTypes().contains(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION)) {
                long timeMetadata = message.getMetadata().getDateTimeStamp().get().getTime().getTime();
                long timeNow = System.currentTimeMillis();
                long millisString = timeNow - timeMetadata;

                String deviceId = null;
                ObservationPayload observationPayload = message.getPayloadAsGOIoTPPayload().asObservationPayload();
                for (EntityID observationEntityID : observationPayload.getObservations()) {
                    deviceId = String.valueOf(observationPayload.getMadeBySensor(observationEntityID));
                }

                writer.println(timeMetadata + ", " + timeNow + ", " + millisString + ", " + deviceId);

                timeEnd = System.currentTimeMillis();
            } else {
                logger.debug("Received message with message type: {}", message.getMetadata().getMessageTypes().toString());
            }
        }
        writer.close();
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

        return conversations.get(conversationId).poll(Integer.MAX_VALUE, TimeUnit.SECONDS);
    }
}