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
import eu.interiot.intermw.comm.broker.Service;
import eu.interiot.intermw.comm.broker.Topic;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * An abstract {@link Service} implementation
 *
 * @param <M> The message class this {@link Service} works with
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public abstract class AbstractService<M> implements Service<M> {

    private final Logger log = LoggerFactory.getLogger(AbstractService.class);

    protected Broker broker;
    protected Topic<M> topic;

    /**
     * Override for initializing the connection of this {@link Service} to the
     * message broker implementation
     *
     * @param broker The {@link Broker} instance
     * @throws BrokerException
     */
    protected abstract void initConnection(Broker broker) throws BrokerException;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Broker broker, Class<? extends Topic<M>> topicClass) throws BrokerException {
        this.init(broker);
        this.createTopic(topicClass);
        this.afterTopicCreated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Broker broker, String exchangeName, Class<M> messageClass) throws BrokerException {
        this.init(broker);
        this.createTopic(exchangeName, messageClass);
        this.afterTopicCreated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Topic<M> getTopic() {
        return this.topic;
    }

    /**
     * This is a hook for subclasses to perform anything after the {@link Topic}
     * has been created
     *
     * @throws BrokerException
     */
    protected void afterTopicCreated() throws BrokerException {
        this.createTopic(this.topic.getExchangeName());
    }

    private void init(Broker broker) throws BrokerException {
        this.broker = broker;
        this.initConnection(broker);

        if (log.isTraceEnabled()) {
            log.trace("new publisher inited");
        }
    }

    private void createTopic(Class<? extends Topic<M>> topicClass) throws BrokerException {
        try {
            topic = topicClass.getConstructor(Broker.class).newInstance(this.broker);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new BrokerException(e);
        }
    }

    private void createTopic(String exchangeName, Class<M> messageClass) throws BrokerException {
        Class<Topic<M>> topicClass = broker.getDefaultTopicClass();
        try {
            topic = topicClass.getConstructor(String.class, Broker.class, Class.class).newInstance(exchangeName,
                    this.broker, messageClass);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new BrokerException(e);
        }
    }
}
