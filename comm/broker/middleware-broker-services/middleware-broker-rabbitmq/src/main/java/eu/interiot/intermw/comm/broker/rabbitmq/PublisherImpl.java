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

import eu.interiot.intermw.comm.broker.Broker;
import eu.interiot.intermw.comm.broker.Publisher;
import eu.interiot.intermw.comm.broker.Topic;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.comm.broker.rabbitmq.util.Constants;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * An implementation of the {@link Publisher} interface for RabbitMQ message bus
 *
 * @param <M> The messages class this {@link Publisher} works with
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
@eu.interiot.intermw.comm.broker.annotations.Publisher(broker = "rabbitmq")
public class PublisherImpl<M> extends AbstractRabbitService<M> implements Publisher<M> {

    private Logger log = LoggerFactory.getLogger(PublisherImpl.class);
    public final static String DEFAULT_ROUTING_KEY = Constants.DEFAULT_ROUTING_KEY;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Broker broker, List<eu.interiot.intermw.comm.broker.Queue> queues, String exchangeName,
                     Class<M> messageClass) throws BrokerException {
        super.init(broker, exchangeName, messageClass);

        initQueues(broker, queues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Broker broker, List<eu.interiot.intermw.comm.broker.Queue> queues,
                     Class<Topic<M>> topicClass) throws BrokerException {
        super.init(broker, topicClass);

        initQueues(broker, queues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(M message) throws BrokerException {
        try {
            topic.setMessage(message);
            List<String> routingKeys = topic.getPublishingRoutings();
            String messageId = null;
            Set<URIManagerMessageMetadata.MessageTypesEnum> messageTypes = null;
            if (message instanceof eu.interiot.message.Message) {
                messageId = ((eu.interiot.message.Message) message).getMetadata().getMessageID().orElse(null);
                messageTypes = ((eu.interiot.message.Message) message).getMetadata().getMessageTypes();
            }

            log.debug("Publishing message {} of type {} to exchange {} with routing key {}...", messageId, messageTypes,
                    topic.getExchangeName(), DEFAULT_ROUTING_KEY);

            if (routingKeys == null || routingKeys.isEmpty()) {
                this.send(topic, DEFAULT_ROUTING_KEY);
            } else {
                for (String routingKey : routingKeys) {
                    send(topic, routingKey);
                }
            }
        } catch (Exception e) {
            throw new BrokerException(String.format(
                    "Failed to publish message to the exchange %s.", topic.getExchangeName()), e);
        }
    }

    protected void initQueues(Broker broker, List<eu.interiot.intermw.comm.broker.Queue> queues)
            throws BrokerException {
        ConnectionFactory cf = ResourceManager.getInstance(broker.getConfiguration()).getConnectionFactory();
        if (queues == null || queues.isEmpty()) {
            template = new RabbitTemplate(cf);
        } else {
            RabbitAdmin admin = new RabbitAdmin(cf);
            Queue queue = null;

            String qn = null;
            for (eu.interiot.intermw.comm.broker.Queue queueConfiguration : queues) {
                qn = queueConfiguration.getQueueName();
                if (!StringUtils.isEmpty(qn)) {
                    if (admin.getQueueProperties(qn) == null) {
                        queue = new Queue(qn, queueConfiguration.isDurable(), queueConfiguration.isExclusive(),
                                queueConfiguration.autoDelete());
                        admin.declareQueue(queue);
                        log.debug("spring-rabbitmq publisher created, queue_name = '" + qn + "'");
                    }
                }
            }
        }
    }

    protected void send(Topic<M> topic, String routingKey) throws BrokerException {
        try {
            template.convertAndSend(topic.getExchangeName(), routingKey, topic.serialize(), new MessagePostProcessor() {
                @Override
                public Message postProcessMessage(Message message) throws AmqpException {
                    message.getMessageProperties().setDeliveryMode(DEFAULT_MESSAGE_DELIVERY_MODE);
                    return message;
                }
            });
        } catch (Exception e) {
            throw new BrokerException(String.format(
                    "Failed to publish message to the exchange %s.", topic.getExchangeName()), e);
        }
    }
}
