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
package eu.interiot.intermw.bridge.abstracts;

import eu.interiot.intermw.bridge.BridgeContext;
import eu.interiot.intermw.bridge.BridgeController;
import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.bridge.model.Bridge;
import eu.interiot.intermw.comm.broker.*;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * An abstract implementation of {@link BridgeController} It is recommended that
 * instances of {@link BridgeController} extend this class
 *
 * @author <a href="mailto:mllorente@prodevelop.es">Miguel A. Llorente</a>
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
// FIXME fix generics issues?
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractBridgeController implements BridgeController {
    private final static Logger logger = LoggerFactory.getLogger(AbstractBridgeController.class);

    /*
     * Only properties in the configuration file starting with PROP_PREFIX will
     * be used in this BridgeController instance
     *
     */
    private final static String PROP_PREFIX = "bridge-";
    protected final Platform platform;

    protected final Bridge bridge;
    protected Properties properties;
    protected Broker broker;

    // FIXME think about having a central registry of publishers/subscribers for
    // InterMwApi, since this seems something that needs to be managed
    // everywhere
    // Maybe the registry can be created at the broker API level
    private final Map<String, Publisher<Message>> publishers = new HashMap<String, Publisher<Message>>();
    private final Map<String, List<Subscriber>> subscribersPerTopic = new HashMap<String, List<Subscriber>>();

    /**
     * The constructor. Creates a new instance of {@link BridgeController}
     *
     * It automatically gets a
     * {@link Broker} instance and loads
     * {@link Properties} instance from the
     * {@link InterMwApi#getBridgesConfiguration()} extracting only the
     * properties that match {@link #PROP_PREFIX}
     *
     * @param bridge A {@link Bridge} instance
     * @throws BridgeException Some instances are loaded by reflection so if some Exception
     *                         is caught during the process, it is wrapped into a
     *                         {@link BridgeException}
     */
    public AbstractBridgeController(Platform platform, Bridge bridge) throws BridgeException {
        try {
            this.bridge = bridge;
            this.platform = platform;
            //FIXME (Flavio)- this is a quick fix to have correct dependenices.
            //(Miguel A.) - This may mean that we have too much classes (7) called 'Context' in intermw.
            this.broker = BrokerContext.getBroker();
            this.properties = BridgeContext.getConfiguration().getPropertiesWithPrefix(PROP_PREFIX);
        } catch (Exception e) {
            throw new BridgeException(e);
        }
    }

    /**
     * Gets a {@link Publisher} instance for the given <code>topicName</code>
     *
     * {@link Publisher} instances are cached, if a {@link Publisher} for a
     * topic has already been created, it will be used the cached instance
     *
     * This is important to free resources later on
     *
     * @param topicName The topicName to create a {@link Publisher}
     * @return A new {@link Publisher} instance
     * @throws BridgeException Thrown when it is not possible to create the
     *                         {@link Publisher} instance
     */
    public Publisher<Message> getPublisher(String topicName) throws BridgeException {
        try {
            Publisher<Message> publisher;

            if (publishers.containsKey(topicName)) {
                publisher = publishers.get(topicName);
            } else {
                publisher = broker.createPublisher(topicName, Message.class);
                publishers.put(topicName, publisher);
            }

            return publisher;
        } catch (Exception e) {
            throw new BridgeException(e);
        }
    }

    /**
     * Creates a {@link Subscriber} for the given <code>topicName</code>
     *
     * This {@link BridgeController} implementation internally caches the
     * {@link Subscriber} instances to be able to free resources when they are
     * not needed anymore
     *
     * More than one subscriber for <code>topicName</code> can exist
     *
     * @param topicName The topicName where to subscribe
     * @param listener  A {@link Listener} instance to {@link Listener#handle(Object)}
     *                  messages
     * @param _class    The Class of the messages to subscribe to
     * @throws BridgeException Thrown when it is not possible to create the
     *                         {@link Subscriber} instance
     */
    public void subscribe(String topicName, Listener listener, Class _class) throws BridgeException {
        try {
            Subscriber<?> subscriber = broker.createSubscriber(topicName, _class);
            subscriber.subscribe(listener);

            if (!subscribersPerTopic.containsKey(topicName)) {
                subscribersPerTopic.put(topicName, new ArrayList<>());
            }
            List<Subscriber> subscribers = subscribersPerTopic.get(topicName);
            subscribers.add(subscriber);

        } catch (Exception e) {
            throw new BridgeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws BridgeException, BrokerException {
        try {
            for (Publisher<Message> publisher : publishers.values()) {
                publisher.cleanUp();
            }

            for (List<Subscriber> subscribers : subscribersPerTopic.values()) {
                for (Subscriber subscriber : subscribers) {
                    subscriber.cleanUp();
                }
            }

        } catch (Exception e) {
            throw new BridgeException(String.format("Failed to destroy bridge controller for the platform %s.",
                    platform.getPlatformId()), e);
        }
    }
}
