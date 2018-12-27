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
package eu.interiot.intermw.comm.broker.rabbitmq;

import eu.interiot.intermw.comm.broker.BrokerContext;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.message.Message;
import eu.interiot.message.exceptions.MessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class QueueImpl extends AbstractRabbitService {

    private Logger log = LoggerFactory.getLogger(QueueImpl.class);

    public QueueImpl() throws MiddlewareException {
        this.broker = BrokerContext.getBroker("rabbitmq");
        super.initConnection(broker);
    }

    public String createQueue(String clientId) throws BrokerException {

        ConnectionFactory cf = ResourceManager.getInstance(broker.getConfiguration()).getConnectionFactory();
        RabbitAdmin rabbitAdmin = new RabbitAdmin(cf);
        String queueName = getQueueName(clientId);

        if (rabbitAdmin.getQueueProperties(queueName) == null) {
            Queue queue = new org.springframework.amqp.core.Queue(queueName, true, false, false);
            rabbitAdmin.declareQueue(queue);
            log.debug("A queue {} has been created in RabbitMQ for the client {}.", queueName, clientId);
        } else {
            log.debug("Queue {} already exists.", queueName);
        }
        return queueName;
    }

    public void deleteQueue(String clientId) throws BrokerException {

        ConnectionFactory cf = ResourceManager.getInstance(broker.getConfiguration()).getConnectionFactory();
        RabbitAdmin rabbitAdmin = new RabbitAdmin(cf);
        String queueName = getQueueName(clientId);
        rabbitAdmin.deleteQueue(queueName);
        log.debug("The queue {} has been deleted.", queueName);
    }

    public Message consumeMessage(String clientId) throws IOException, MessageException {
        String queueName = getQueueName(clientId);
        org.springframework.amqp.core.Message rabbitMessage = template.receive(queueName);
        if (rabbitMessage != null) {
            String messageString = new String(rabbitMessage.getBody(), StandardCharsets.UTF_8);
            return new Message(messageString);
        } else {
            return null;
        }
    }

    public Message consumeMessage(String clientId, long timeoutMillis) throws IOException, MessageException {
        String queueName = getQueueName(clientId);
        org.springframework.amqp.core.Message rabbitMessage = template.receive(queueName, timeoutMillis);
        if (rabbitMessage != null) {
            String messageString = new String(rabbitMessage.getBody(), StandardCharsets.UTF_8);
            return new Message(messageString);
        } else {
            return null;
        }
    }

    public List<String> consumeMessages(String clientId, int limit) throws IOException, MessageException {
        String queueName = getQueueName(clientId);
        List<String> messages = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            org.springframework.amqp.core.Message rabbitMessage = template.receive(queueName);
            if (rabbitMessage != null) {
                String messageString = new String(rabbitMessage.getBody(), StandardCharsets.UTF_8);
                messages.add(messageString);
            } else {
                break;
            }
        }

        return messages;
    }

    public List<String> consumeMessagesBlocking(String clientId, int limit) {
        String queueName = getQueueName(clientId);
        if (limit < 1) {
            limit = 1;
        }
        List<String> messages = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            org.springframework.amqp.core.Message rabbitMessage;
            if (i == 0) {
                rabbitMessage = template.receive(queueName, -1);
            } else {
                rabbitMessage = template.receive(queueName);
            }

            if (rabbitMessage == null) {
                break;
            }
            String messageString = new String(rabbitMessage.getBody(), StandardCharsets.UTF_8);
            messages.add(messageString);
        }

        return messages;
    }

    public void sendMessage(Message message, String clientId) throws IOException {
        String queueName = getQueueName(clientId);
        String messageString = message.serializeToJSONLD();
        org.springframework.amqp.core.Message rabbitMessage = MessageBuilder.withBody(messageString.getBytes()).build();
        template.send(queueName, rabbitMessage);
        log.debug("Message has been published to an exchange with routing key {}.", queueName);
    }

    public void sendMessage(String serializedMessage, String clientId) {
        String queueName = getQueueName(clientId);
        org.springframework.amqp.core.Message rabbitMessage = MessageBuilder.withBody(serializedMessage.getBytes()).build();
        template.send(queueName, rabbitMessage);
        log.debug("Message has been published to an exchange with routing key {}.", queueName);
    }

    private String getQueueName(String clientId) {
        return "client-" + clientId.replaceAll("[:/#]+", "_");
    }
}
