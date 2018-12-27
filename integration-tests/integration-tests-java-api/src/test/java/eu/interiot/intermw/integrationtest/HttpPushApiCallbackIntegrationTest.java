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

import eu.interiot.intermw.comm.arm.HttpPushApiCallback;
import eu.interiot.intermw.comm.broker.rabbitmq.QueueImpl;
import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.Client;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.ID.PropertyID;
import eu.interiot.message.Message;
import eu.interiot.message.managers.URI.URIManagerINTERMW;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import eu.interiot.message.payload.types.ObservationPayload;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import spark.Service;

import java.net.URL;

import static org.junit.Assert.*;

public class HttpPushApiCallbackIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(HttpPushApiCallbackIntegrationTest.class);
    private static final String CONFIG_FILE = "intermw.properties";
    private static final String CLIENT_ID1 = "myclient1";
    private static final String CLIENT_ID2 = "myclient2";
    private static final int CALLBACK_PORT1 = 7070;
    private static final int CALLBACK_PORT2 = 7071;
    private static PropertyID PROPERTY_ID = new PropertyID(URIManagerINTERMW.PREFIX.intermw + "observationNumber");
    private static final int TIMEOUT = 15;
    private int observationNumber = 1;
    private int numReceivedObservations1 = 0;
    private int numReceivedObservations2 = 0;
    private Configuration conf;

    @Before
    public void setUp() throws Exception {
        conf = new DefaultConfiguration(CONFIG_FILE);
        CachingConnectionFactory cf = new CachingConnectionFactory(conf.getRabbitmqHostname(), conf.getRabbitmqPort());
        cf.setUsername(conf.getRabbitmqUsername());
        cf.setPassword(conf.getRabbitmqPassword());
        RabbitAdmin rabbitAdmin = new RabbitAdmin(cf);
        String[] queuesToDelete = {"client-myclient1", "client-myclient2"};
        for (String queueToDelete : queuesToDelete) {
            rabbitAdmin.deleteQueue(queueToDelete);
        }
        logger.debug("RabbitMQ has been cleared.");
    }

    @Test
    public void testHttpPushApiCallback() throws Exception {
        Service spark1 = Service.ignite().port(CALLBACK_PORT1);
        logger.debug("Client1 is listening on port {}.", CALLBACK_PORT1);

        Service spark2 = Service.ignite().port(CALLBACK_PORT2);
        logger.debug("Client2 is listening on port {}.", CALLBACK_PORT2);

        spark1.post("/endpoint", (request, response) -> {
            try {
                assertEquals("application/ld+json; charset=UTF-8", request.contentType());
                String content = request.body();
                content = content.substring(1, content.length() - 2);
                String[] messageStrings = content.split("\\r?\\n,\\r?\\n");
                logger.debug("New messages pack received containing {} messages.", messageStrings.length);

                for (String messageString : messageStrings) {
                    Message message = new Message(messageString);
                    numReceivedObservations1++;
                    EntityID entityID = new EntityID("http://inter-iot.eu/test");
                    int observationNumber = (Integer)
                            message.getPayload().getAllDataPropertyAssertionsForEntity(entityID, PROPERTY_ID).iterator().next();
                    assertEquals(numReceivedObservations1, observationNumber);
                }

                response.status(204);
                return "";
            } catch (Error e) {
                logger.error("Failed to process message: " + e.getMessage(), e);
                throw e;
            }
        });

        spark2.post("/endpoint", (request, response) -> {
            try {
                assertEquals("application/ld+json; charset=UTF-8", request.contentType());
                String content = request.body();
                content = content.substring(1, content.length() - 2);
                String[] messageStrings = content.split("\\r?\\n,\\r?\\n");
                logger.debug("New messages pack received containing {} messages.", messageStrings.length);

                for (String messageString : messageStrings) {
                    Message message = new Message(messageString);
                    numReceivedObservations2++;
                    EntityID entityID = new EntityID("http://inter-iot.eu/test");
                    int observationNumber = (Integer)
                            message.getPayload().getAllDataPropertyAssertionsForEntity(entityID, PROPERTY_ID).iterator().next();
                    assertEquals(numReceivedObservations2, observationNumber);
                }

                response.status(204);
                return "";
            } catch (Error e) {
                logger.error("Failed to process message: " + e.getMessage(), e);
                throw e;
            }
        });

        Client client1 = new Client();
        client1.setClientId(CLIENT_ID1);
        client1.setResponseDelivery(Client.ResponseDelivery.SERVER_PUSH);
        client1.setCallbackUrl(new URL("http://localhost:" + CALLBACK_PORT1 + "/endpoint"));
        client1.setReceivingCapacity(5);
        client1.setResponseFormat(Client.ResponseFormat.JSON_LD);

        Client client2 = new Client();
        client2.setClientId(CLIENT_ID2);
        client2.setResponseDelivery(Client.ResponseDelivery.SERVER_PUSH);
        client2.setCallbackUrl(new URL("http://localhost:" + CALLBACK_PORT2 + "/endpoint"));
        client2.setReceivingCapacity(5);
        client2.setResponseFormat(Client.ResponseFormat.JSON_LD);

        QueueImpl queue = new QueueImpl();
        HttpPushApiCallback apiCallback1 = new HttpPushApiCallback(client1, queue, conf);
        HttpPushApiCallback apiCallback2 = new HttpPushApiCallback(client2, queue, conf);

        for (int i = 0; i < 50; i++) {
            Message message = createObservationMessage();
            apiCallback1.handle(message);
            apiCallback2.handle(message);
        }

        for (int i = 0; i < 50; i++) {
            Message message = createObservationMessage();
            apiCallback1.handle(message);
            apiCallback2.handle(message);
            Thread.sleep(10);
        }

        long startTime = System.currentTimeMillis();
        do {
            Thread.sleep(100);
            if (System.currentTimeMillis() - startTime > TIMEOUT * 1000) {
                fail("Timeout waiting for observation messages.");
            }
        } while (numReceivedObservations1 < 100 || numReceivedObservations2 < 100);

        assertTrue(numReceivedObservations1 == 100 && numReceivedObservations2 == 100);

        apiCallback1.stop();
        apiCallback2.stop();
    }

    private Message createObservationMessage() {
        Message message = new Message();
        message.getMetadata().setClientId(CLIENT_ID1);
        message.getMetadata().addMessageType(MessageTypesEnum.OBSERVATION);

        ObservationPayload observationPayload = new ObservationPayload();
        EntityID entityID = new EntityID("http://inter-iot.eu/test");
        observationPayload.setDataPropertyAssertionForEntity(entityID, PROPERTY_ID, observationNumber);
        message.setPayload(observationPayload);

        observationNumber++;

        return message;
    }

    private void manualTest() throws Exception {
        conf = new DefaultConfiguration(CONFIG_FILE);

        Client client = new Client();
        client.setClientId(CLIENT_ID1);
        client.setResponseDelivery(Client.ResponseDelivery.SERVER_PUSH);
        client.setCallbackUrl(new URL("http://localhost:" + CALLBACK_PORT1 + "/endpoint"));
        client.setReceivingCapacity(10);
        client.setResponseFormat(Client.ResponseFormat.JSON_LD);

        QueueImpl queue = new QueueImpl();
        HttpPushApiCallback apiCallback = new HttpPushApiCallback(client, queue, conf);

        for (int i = 0; i < 50; i++) {
            Message message = createObservationMessage();
            apiCallback.handle(message);
        }

        for (int i = 0; i < 50; i++) {
            Message message = createObservationMessage();
            apiCallback.handle(message);
            Thread.sleep(25);
        }

        // update HttpPushApiCallback object
        client.setCallbackUrl(new URL("http://localhost:" + CALLBACK_PORT2 + "/endpoint"));
        client.setReceivingCapacity(1);
        apiCallback.update(client);
        for (int i = 0; i < 10; i++) {
            Message message = createObservationMessage();
            apiCallback.handle(message);
        }

        Thread.sleep(30000);
        apiCallback.stop();
    }

    public static void main(String[] args) throws Exception {
        new HttpPushApiCallbackIntegrationTest().manualTest();
    }
}
