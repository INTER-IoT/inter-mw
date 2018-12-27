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
import eu.interiot.intermw.comm.broker.Service;
import eu.interiot.intermw.comm.broker.abstracts.AbstractService;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

import java.util.Date;

/**
 * An abstract {@link Service} implementation for RabbitMQ
 *
 * @param <M> The message class this {@link Service} works with
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public class AbstractRabbitService<M> extends AbstractService<M> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractRabbitService.class);
    private static final int RABBITMQ_CONNECT_TIMEOUT = 30;

    protected RabbitTemplate template;

    protected RabbitAdmin admin;
    protected SimpleMessageListenerContainer container;
    protected final static boolean DEFAULT_DURABILITY = true;
    protected final static boolean DEFAULT_AUTODELETE = false;
    protected final static MessageDeliveryMode DEFAULT_MESSAGE_DELIVERY_MODE = MessageDeliveryMode.NON_PERSISTENT;
    protected final static boolean DEFAULT_QUEUE_DURABILITY = false;
    protected final static boolean DEFAULT_QUEUE_EXCLUSIVE = false;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initConnection(Broker broker) throws BrokerException {
        template = new RabbitTemplate(
                ResourceManager.getInstance(broker.getConfiguration()).getConnectionFactory());
        admin = new RabbitAdmin(ResourceManager.getInstance(broker.getConfiguration()).getConnectionFactory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createTopic(String name) throws BrokerException {
        TopicExchange exchange = new TopicExchange(name, DEFAULT_DURABILITY, DEFAULT_AUTODELETE);
        Date startTime = new Date();
        do {
            try {
                admin.declareExchange(exchange);
                break;

            } catch (AmqpConnectException | AmqpIOException e) {
                logger.warn("Failed to connect to RabbitMQ server: {}", e.getMessage());
                if (new Date().getTime() - startTime.getTime() > RABBITMQ_CONNECT_TIMEOUT * 1000) {
                    throw e;
                }
            }

            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                throw new BrokerException("Interrupted while trying to connect to RabbitMQ.", e);
            }

        } while (true);
    }

    @Override
    public void deleteTopic(String name) throws BrokerException {
        admin.deleteExchange(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanUp() throws BrokerException {
        if (container != null) {
            container.destroy();
        }
    }
}