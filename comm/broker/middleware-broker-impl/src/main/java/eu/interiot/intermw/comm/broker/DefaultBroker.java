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
package eu.interiot.intermw.comm.broker;

import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.comm.broker.exceptions.ErrorCodes;
import eu.interiot.intermw.commons.interfaces.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * A default {@link Broker} implementation
 *
 * It uses reflection to create instances of the classes annotated as
 * {@link Publisher}, {@link Subscriber}, {@link Topic}, etc.
 *
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 * @see eu.interiot.intermw.comm.broker.annotations
 */
@eu.interiot.intermw.comm.broker.annotations.Broker
public class DefaultBroker implements Broker {
    private final Logger log = LoggerFactory.getLogger(DefaultBroker.class);

    private final Configuration configuration;
    private final String brokerImplementation;

    /**
     * Creates a new {@link DefaultBroker} given a {@link Configuration}
     *
     * @param configuration The {@link Configuration} instance
     * @throws BrokerException
     */
    public DefaultBroker(Configuration configuration, String brokerImplementation) throws BrokerException {
        log.debug("Create default broker with configuration " + configuration.getClass().getName());
        this.configuration = configuration;
        this.brokerImplementation = brokerImplementation;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <M> Publisher<M> createPublisher(Class<? extends Topic<M>> topicClass) throws BrokerException {
        try {
            Publisher<M> publisher = (Publisher<M>) BrokerContext.getPublisherClass(brokerImplementation).newInstance();
            publisher.init(this, topicClass);
            return publisher;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BrokerException(e);
        } catch (BrokerException e) {
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <M> Subscriber<M> createSubscriber(Class<? extends Topic<M>> topicClass) throws BrokerException {
        try {
            Subscriber<M> subscriber = (Subscriber<M>) BrokerContext.getSubscriberClass(brokerImplementation).newInstance();
            subscriber.init(this, topicClass);
            return subscriber;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BrokerException(e);
        } catch (BrokerException e) {
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <M> Serializer<M> createSerializer() throws BrokerException {
        try {
            return (Serializer<M>) BrokerContext.getSerializer().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BrokerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <M> Subscriber<M> createSubscriber(String exchangeName, Class<M> messageClass) throws BrokerException {
        try {
            Subscriber<M> subscriber = (Subscriber<M>) BrokerContext.getSubscriberClass(brokerImplementation).newInstance();
            subscriber.init(this, exchangeName, messageClass);
            return subscriber;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BrokerException(e);
        } catch (BrokerException e) {
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <M> Subscriber<M> createSubscriber(String exchangeName, String className) throws BrokerException {
        Class<M> messageClass;
        try {
            messageClass = (Class<M>) Class.forName(className);
            return this.createSubscriber(exchangeName, messageClass);
        } catch (Exception e) {
            throw new BrokerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <M> Publisher<M> createPublisher(String exchangeName, Class<M> messageClass) throws BrokerException {
        try {
            Publisher<M> publisher = (Publisher<M>) BrokerContext.getPublisherClass(brokerImplementation).newInstance();
            publisher.init(this, exchangeName, messageClass);
            return publisher;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BrokerException(e);
        } catch (BrokerException e) {
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <M> Publisher<M> createPublisher(String exchangeName, String className) throws BrokerException {
        Class<M> messageClass;
        try {
            messageClass = (Class<M>) Class.forName(className);
            return this.createPublisher(exchangeName, messageClass);
        } catch (Exception e) {
            throw new BrokerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <M> Class<Topic<M>> getDefaultTopicClass() throws BrokerException {
        Class<? extends Topic<?>> topicClass = null;
        Set<Class<? extends Topic<Object>>> keySet = BrokerContext.getTopics().keySet();
        for (Class<? extends Topic<Object>> key : keySet) {
            eu.interiot.intermw.comm.broker.annotations.Topic annotation = key
                    .getAnnotation(eu.interiot.intermw.comm.broker.annotations.Topic.class);
            if (annotation != null && annotation.isDefault()) {
                topicClass = key;
                break;
            }
        }

        if (topicClass == null) {
            throw new BrokerException(ErrorCodes.DEFAULT_TOPIC_EXCEPTION.errorCode,
                    ErrorCodes.DEFAULT_TOPIC_EXCEPTION.errorDescription);
        }

        return (Class<Topic<M>>) topicClass;
    }

    @Override
    public String getBrokerImplementation() {
        return brokerImplementation;
    }
}
