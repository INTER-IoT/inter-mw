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
package eu.interiot.intermw.comm.arm;

import eu.interiot.intermw.comm.broker.rabbitmq.QueueImpl;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.ApiCallback;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.Client;
import eu.interiot.message.Message;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

public class HttpPushApiCallback implements ApiCallback<Message> {
    private static final Logger logger = LoggerFactory.getLogger(HttpPushApiCallback.class);
    private static final int SOCKET_TIMEOUT = 10 * 1000;
    private static final int MAX_NUMBER_OF_CONNECTIONS = 10;
    private static final int RETRY_DELAY = 15 * 1000;
    private Configuration configuration;
    private QueueImpl queue;
    private Client client;
    private URI callbackUri;
    private Thread messageSenderThread;

    public HttpPushApiCallback(Client client, QueueImpl queue, Configuration configuration) throws Exception {
        this.client = client;
        this.queue = queue;
        this.configuration = configuration;
        this.callbackUri = client.getCallbackUrl().toURI();

        String queueName = queue.createQueue(client.getClientId());

        MessageSender messageSender = new MessageSender();
        messageSenderThread = new Thread(messageSender);
        messageSenderThread.start();

        logger.debug("HttpPushApiCallback has been created successfully. Response messages will be sent to the client endpoint {}. " +
                "If delivery fails, messages will be kept in the RabbitMQ queue {}.", callbackUri, queueName);
    }

    @Override
    public void handle(Message message) throws MiddlewareException {
        String serializedMessage = ResponseMessageParser.convertMessage(message, client.getResponseFormat());
        queue.sendMessage(serializedMessage, client.getClientId());
    }

    @Override
    public void update(Client client) throws Exception {
        this.client = client;
        this.callbackUri = client.getCallbackUrl().toURI();
    }

    @Override
    public void stop() {
        logger.debug("Stopping HttpPushApiCallback for the client {}...", client.getClientId());
        messageSenderThread.interrupt();
        logger.debug("HttpPushApiCallback for the client {} has been stopped.", client.getClientId());
    }

    private class MessageSender implements Runnable {
        private HttpClient httpclient;

        public MessageSender() {
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setMaxTotal(MAX_NUMBER_OF_CONNECTIONS);

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(SOCKET_TIMEOUT)
                    .build();

            httpclient = HttpClientBuilder
                    .create()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    .build();
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                StringBuilder sb = new StringBuilder();
                int numberOfMessages;
                try {
                    List<String> serializedMessages = queue.consumeMessagesBlocking(client.getClientId(),
                            client.getReceivingCapacity());
                    numberOfMessages = serializedMessages.size();
                    sb.append("[\n");
                    Iterator<String> iterator = serializedMessages.iterator();
                    while (iterator.hasNext()) {
                        sb.append(iterator.next());
                        if (iterator.hasNext()) {
                            sb.append(",\n");
                        }
                    }
                    sb.append("]");

                } catch (Exception e) {
                    logger.error("Failed to consume messages from RabbitMQ queue for the client " + client.getClientId()
                            + ": " + e.getMessage(), e);
                    break;
                }

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        send(sb.toString());
                        logger.debug("{} message(s) has been sent successfully to the client {} using endpoint {}.",
                                numberOfMessages, client.getClientId(), callbackUri);
                        break;

                    } catch (Exception e) {
                        logger.debug("Failed to push message(s) to the client {} using endpoint {}: {}",
                                client.getClientId(), callbackUri, e.getMessage());
                        try {
                            Thread.sleep(RETRY_DELAY);
                        } catch (InterruptedException e1) {
                            return;
                        }
                    }
                }
            }
            logger.debug("MessageSender thread for the client {} has ended.", client.getClientId());
        }

        private void send(String data) throws IOException, MiddlewareException {
            HttpPost httpPost = new HttpPost(callbackUri);
            try {
                StringEntity entity = new StringEntity(data, getContentType(client.getResponseFormat()));
                httpPost.setEntity(entity);

                HttpResponse response = httpclient.execute(httpPost);
                EntityUtils.consume(response.getEntity());

            } finally {
                httpPost.reset();
            }
        }

        private ContentType getContentType(Client.ResponseFormat responseFormat) throws MiddlewareException {
            switch (responseFormat) {
                case JSON_LD:
                    return ContentType.create("application/ld+json", Charset.forName("UTF-8"));
                case JSON:
                    return ContentType.APPLICATION_JSON;
                default:
                    throw new MiddlewareException(String.format("Unsupported response format: %s.", responseFormat));
            }
        }
    }
}
