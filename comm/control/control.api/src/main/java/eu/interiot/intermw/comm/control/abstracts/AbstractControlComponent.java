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
package eu.interiot.intermw.comm.control.abstracts;

import eu.interiot.intermw.comm.broker.*;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.comm.control.ControlComponent;
import eu.interiot.intermw.commons.ErrorReporter;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.model.enums.BrokerTopics;
import eu.interiot.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * INTER-IoT. Interoperability of IoT Platforms.
 * INTER-IoT is a R&D project which has received funding from the European
 * Union’s Horizon 2020 research and innovation programme under grant
 * agreement No 687283.
 * <p>
 * Copyright (C) 2016-2018, by (Author's company of this file):
 * - XLAB d.o.o.
 * <p>
 * <p>
 * For more information, contact:
 * - @author <a href="mailto:flavio.fuart@xlab.si">Flavio Fuart</a>
 * - Project coordinator:  <a href="mailto:coordinator@inter-iot.eu"></a>
 * <p>
 * <p>
 * This code is licensed under the EPL license, available at the root
 * application directory.
 */

/**
 * An abstract implementation of {@link eu.interiot.intermw.comm.control.ControlComponent} It is recommended that
 * instances of {@link eu.interiot.intermw.comm.control.ControlComponent} extend this class
 */

public abstract class AbstractControlComponent implements ControlComponent {
    private final static Logger logger = LoggerFactory.getLogger(AbstractControlComponent.class);

    private final Map<String, Publisher> publishers = new HashMap<String, Publisher>();
    private final Map<String, List<Subscriber>> subscribers = new HashMap<String, List<Subscriber>>();
    private ErrorReporter errorReporter;

    /**
     * The constructor. Creates a new instance of {@link eu.interiot.intermw.comm.control.ControlComponent}
     *
     * @throws MiddlewareException
     */
    public AbstractControlComponent() throws MiddlewareException {
        //FIXME (Flavio)- this is a quick fix to have correct dependenices.
        BrokerContext.getBroker();
        //handlers should be created in derived components

        Publisher<Message> errorPublisher = getPublisher(BrokerTopics.ERROR.getTopicName(), Message.class);
        errorReporter = new ErrorReporter(errorPublisher);
    }


    /**
     * Gets a {@link Publisher} instance for the given <code>topicName</code>
     * <p>
     * {@link Publisher} instances are cached, if a {@link Publisher} for a
     * topic has already been created, it will be used the cached instance
     * <p>
     * This is important to free resources later on
     *
     * @param topicName The topicName to create a {@link Publisher}
     * @return A new {@link Publisher} instance
     * @throws MiddlewareException Thrown when it is not possible to create the
     *                             {@link Publisher} instance
     */
    public <M> Publisher<M> getPublisher(String topicName, Class<M> messageClass) throws MiddlewareException {
        return getPublisher(topicName, messageClass, null);
    }

    /**
     * Gets a {@link Publisher} instance for the given <code>topicName</code>
     * <p>
     * {@link Publisher} instances are cached, if a {@link Publisher} for a
     * topic has already been created, it will be used the cached instance
     * <p>
     * This is important to free resources later on
     *
     * @param topicName            The topicName to create a {@link Publisher}
     * @param brokerImplementation The broker implementration: kafka, rabbitmq, ...
     * @return A new {@link Publisher} instance
     * @throws MiddlewareException Thrown when it is not possible to create the
     *                             {@link Publisher} instance
     */
    public <M> Publisher<M> getPublisher(String topicName, Class<M> messageClass, String brokerImplementation) throws MiddlewareException {
        Publisher<M> publisher;

        if (publishers.containsKey(topicName)) {
            publisher = publishers.get(topicName);
        } else {
            Broker broker = BrokerContext.getBroker(brokerImplementation);
            publisher = broker.createPublisher(topicName, messageClass);
            publishers.put(topicName, publisher);
            logger.debug("Create publisher for " + topicName);
        }

        return publisher;
    }

    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    /**
     * Creates a {@link Subscriber} for the given <code>topicName</code>
     * <p>
     * This {@link eu.interiot.intermw.comm.control.ControlComponent} implementation internally caches the
     * {@link Subscriber} instances to be able to free resources when they are
     * not needed anymore
     * <p>
     * More than one subscriber for <code>topicName</code> can exist
     *
     * @param topicName    The topicName where to subscribe
     * @param listener     A {@link Listener} instance to {@link Listener#handle(Object)}
     *                     messages
     * @param messageClass The Class of the messages to subscribe to
     * @throws MiddlewareException Thrown when it is not possible to create the
     *                             {@link Subscriber} instance
     */
    public <M> void subscribe(String topicName, Listener<M> listener, Class<M> messageClass) throws MiddlewareException {
        subscribe(topicName, listener, messageClass, null);
    }

    /**
     * Creates a {@link Subscriber} for the given <code>topicName</code>
     * <p>
     * This {@link eu.interiot.intermw.comm.control.ControlComponent} implementation internally caches the
     * {@link Subscriber} instances to be able to free resources when they are
     * not needed anymore
     * <p>
     * More than one subscriber for <code>topicName</code> can exist
     *
     * @param topicName            The topicName where to subscribe
     * @param listener             A {@link Listener} instance to {@link Listener#handle(Object)}
     *                             messages
     * @param messageClass         The Class of the messages to subscribe to
     * @param brokerImplementation The broker implementration: kafka, rabbitmq, ...
     * @throws MiddlewareException Thrown when it is not possible to create the
     *                             {@link Subscriber} instance
     */
    public <M> void subscribe(String topicName, Listener<M> listener, Class<M> messageClass, String brokerImplementation) throws MiddlewareException {
        try {
            logger.info("Subscribing to topic {}...", topicName);
            Broker broker = BrokerContext.getBroker(brokerImplementation);
            Subscriber<M> subscriber = broker.createSubscriber(topicName, messageClass);
            List<Subscriber> subscribersByTopic = subscribers.get(topicName);

            if (subscribersByTopic == null) {
                subscribersByTopic = new ArrayList<Subscriber>();
                subscribers.put(topicName, subscribersByTopic);
            }

            subscriber.subscribe(listener);
            subscribersByTopic.add(subscriber);
            logger.debug("Successfully subscribed to topic {} using {} broker.", topicName, brokerImplementation);

        } catch (Exception e) {
            throw new MiddlewareException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws MiddlewareException {
        for (Publisher<Message> publisher : publishers.values()) {
            publisher.cleanUp();
        }

        for (List<Subscriber> subscribersByTopic : subscribers.values()) {
            for (Subscriber subscriber : subscribersByTopic) {
                subscriber.cleanUp();
            }
        }
    }

    public void removePublisher(String topic) throws BrokerException {
        Publisher publisher = publishers.remove(topic);
        if (publisher != null)
            publisher.cleanUp();
    }


    /**
     * Free the resources (subscribers), associated with the topic
     *
     * @param topicName The topic name to unsubscribe
     * @throws MiddlewareException Thrown when it is not possible to destroy the {@link Subscriber} instance
     */
    public void unsubscribe(String topicName) throws BrokerException {
        List<Subscriber> subscribersByTopic = subscribers.remove(topicName);
        if (subscribersByTopic != null) {
            for (Subscriber subscriber : subscribersByTopic) {
                subscriber.cleanUp();
            }
        }
    }
}
