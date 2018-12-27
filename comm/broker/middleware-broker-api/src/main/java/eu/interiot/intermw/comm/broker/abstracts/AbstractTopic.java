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
package eu.interiot.intermw.comm.broker.abstracts;

import eu.interiot.intermw.comm.broker.Broker;
import eu.interiot.intermw.comm.broker.Routing;
import eu.interiot.intermw.comm.broker.Serializer;
import eu.interiot.intermw.comm.broker.Topic;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.comm.broker.exceptions.EmptyTopicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.util.List;

/**
 * An abstract {@link Topic} implementation
 *
 * @param <M> The message class this {@link Topic} is tied to
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public abstract class AbstractTopic<M> implements Topic<M> {

    private final static Logger log = LoggerFactory.getLogger(AbstractTopic.class);

    protected M message;
    protected Broker broker;
    protected Routing<M> routing;

    private Serializer<M> json;
    private Class<M> messageClass;

    private String exchangeName;

    /**
     * Creates a new {@link Topic} given a {@link Broker} instance
     *
     * @param broker The {@link Broker} instance
     * @throws BrokerException
     */
    public AbstractTopic(Broker broker) throws BrokerException {
        this.json = broker.createSerializer();

        this.broker = broker;
    }

    /**
     * Creates a new {@link Topic} instance
     *
     * @param exchangeName The name of the message broker exchange (or topic)
     * @param broker       The {@link Broker} instance
     * @param messageClass The message class this {@link Topic} works with
     * @throws BrokerException
     */
    public AbstractTopic(String exchangeName, Broker broker, Class<M> messageClass) throws BrokerException {
        this(broker);

        this.messageClass = messageClass;
        this.exchangeName = exchangeName;
    }

    // /**
    // * Creates a new {@link Topic} instance
    // *
    // * @param exchangeName
    // * The name of the message broker exchange (or topic)
    // * @param broker
    // * The {@link Broker} instance
    // * @param routing
    // * The {@link Routing}instance
    // * @param messageClass
    // * The message class this {@link Topic} works with
    // * @throws BrokerException
    // */
    // public AbstractTopic(String exchangeName, Broker broker,
    // Routing<M> routing, Class<M> messageClass)
    // throws BrokerException {
    // this(exchangeName, broker, messageClass);
    // this.routing = routing;
    // }

    /**
     * {@inheritDoc}
     */
    @Override
    public String serialize() throws BrokerException {
        if (message == null) {
            throw new EmptyTopicException();
        }
        return json.serialize(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public M deserialize(String message, Class<M> type) throws BrokerException {
        return json.deserialize(message, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public M getMessage() throws EmptyTopicException {
        if (message == null) {
            throw new EmptyTopicException();
        }
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMessage(M message) throws EmptyTopicException {
        if (message == null) {
            throw new EmptyTopicException();
        }
        this.message = message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getPublishingRoutings() {
        List<String> routings = null;
        if (this.routing != null) {
            try {
                routings = this.routing.getPublishingRoutings(this, null);
            } catch (BrokerException e) {
                log.error(e.getMessage(), e);
            }
        }

        return routings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getSubscribingRoutings() {
        List<String> routings = null;
        if (this.routing != null) {
            try {
                routings = this.routing.getSubscribingRoutings(this);
            } catch (BrokerException e) {
                log.error(e.getMessage(), e);
            }
        }

        return routings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Broker getBroker() {
        return this.broker;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Class<M> getMessageClass() {
        if (this.messageClass == null) {
            this.messageClass = (Class<M>) ((ParameterizedType) getClass().getGenericSuperclass())
                    .getActualTypeArguments()[0];
        }

        return this.messageClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExchangeName() {
        return this.exchangeName;
    }
}
