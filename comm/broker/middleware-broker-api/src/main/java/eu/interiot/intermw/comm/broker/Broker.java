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
import eu.interiot.intermw.commons.interfaces.Configuration;

/**
 * A factory for retrieving {@link Publisher}, {@link Subscriber},
 * {@link Serializer}, {@link Topic} instances and so on
 *
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public interface Broker {

    /**
     * Creates a new {@link Publisher} given a {@link Topic} class
     *
     * @param topicClass The class of the {@link Topic} to use
     * @return A new {@link Publisher} instance to use for a {@link Topic}
     * @throws BrokerException
     * @see #createPublisher(String, Class)
     */
    public <M> Publisher<M> createPublisher(Class<? extends Topic<M>> topicClass) throws BrokerException;

    /**
     * Creates a new {@link Subscriber} given a {@link Topic} class
     *
     * @param topicClass The class of the {@link Topic} to use
     * @return A new {@link Subscriber} instance
     * @throws BrokerException
     * @see #createSubscriber(String, Class)
     */
    public <M> Subscriber<M> createSubscriber(Class<? extends Topic<M>> topicClass) throws BrokerException;

    /**
     * Creates a new {@link Publisher} given the <exchangeName> and the
     * <className> of the messages that are going to be published.
     *
     * This method for creating a {@link Publisher} is useful when the message
     * class is not known at compile time
     *
     * @param exchangeName The name of the exchange where to publish messages in the
     *                     broker
     * @param className    The Class name of the messages to be published. It will be
     *                     created by reflection
     * @return A new {@link Publisher} instance
     * @throws BrokerException
     */
    public <M> Publisher<M> createPublisher(String exchangeName, String className) throws BrokerException;

    /**
     * Creates a new {@link Publisher} given the <exchangeName> and the Class of
     * the messages that are going to be published.
     *
     * This is the preferred method to create a {@link Publisher}
     *
     * @param exchangeName The name of the exchange where to publish messages in the
     *                     broker
     * @param messageClass The Class of the messages to be published
     * @return A new {@link Publisher} instance
     * @throws BrokerException
     */
    public <M> Publisher<M> createPublisher(String exchangeName, Class<M> messageClass) throws BrokerException;

    /**
     * Creates a new {@link Subscriber} given the <exchangeName> and the
     * <className> of the messages that are going to be subscribed to
     *
     * @param exchangeName The name of the exchange where to subscribe messages from the
     *                     broker
     * @param className    the Class name of the messages to be subscribed to. It will be
     *                     created by reflection
     * @return A new {@link Subscriber} instance
     * @throws BrokerException
     */
    public <M> Subscriber<M> createSubscriber(String exchangeName, String className) throws BrokerException;

    /**
     * Creates a new {@link Subscriber} given the <exchangeName> and the Class
     * of the messages that are going to be subscribed to
     *
     * @param exchangeName The name of the exchange where to subscribe messages from the
     *                     broker
     * @param messageClass the Class of the messages to be subscribed to
     * @return A new {@link Subscriber} instance
     * @throws BrokerException
     */
    public <M> Subscriber<M> createSubscriber(String exchangeName, Class<M> messageClass) throws BrokerException;

    /**
     * The instance of {@link Serializer} that is going to be used for
     * publishing and subscribing to messages
     *
     * @return A {@link Serializer} insetance
     * @throws BrokerException
     */
    public <M> Serializer<M> createSerializer() throws BrokerException;

    /**
     * Gets the {@link Configuration} instance used to create this
     * {@link Broker} instance
     *
     * @return The {@link Configuration} instance
     */
    public Configuration getConfiguration();

    /**
     * The default {@link Topic} class to be used when no {@link Topic} are
     * registered in this {@link Broker} instance
     *
     * This {@link Topic} is the one used by the
     * {@link #createPublisher(String, Class)} and
     * {@link #createSubscriber(String, Class)} methods
     *
     * @return
     * @throws BrokerException
     * @see #createPublisher(String, Class)
     * @see #createSubscriber(String, Class)
     */
    public <M> Class<Topic<M>> getDefaultTopicClass() throws BrokerException;

    String getBrokerImplementation();
}