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

/**
 * An interface to be implemented by any {@link Broker} service
 *
 * @param <M> The message class this services works with
 * @author <a href="mailto:aromeu@prodevelop.es">Alberto Romeu</a>
 */
public interface Service<M> {

    /**
     * Initializes this {@link Service} instance
     *
     * (Creates and configures the connection, etc.)
     *
     * @param broker     A {@link Broker} instance
     * @param topicClass The {@link Topic} class attached to this service
     * @throws BrokerException When something went wrong
     */
    public void init(Broker broker, Class<? extends Topic<M>> topicClass) throws BrokerException;

    /**
     * Initializes this {@link Service} instance
     *
     * @param broker       A {@link Broker} instance
     * @param exchangeName The name of the exchange for the {@link Topic}
     * @param messageClass The name of Class of the messages this {@link Service} is
     *                     going to work with
     * @throws BrokerException When something went wrong
     */
    public void init(Broker broker, String exchangeName, Class<M> messageClass) throws BrokerException;

    /**
     * Cleans the resources created by this {@link Service} (connections,
     * queues, etc.)
     *
     * @throws BrokerException
     */
    public void cleanUp() throws BrokerException;

    /**
     * Gets the {@link Topic} instance this {@link Service} is working with
     *
     * @return
     */
    public Topic<M> getTopic();

    /**
     * Mandatory method to be implemented by {@link Service} implementations to
     * create a topic in the broker.
     *
     * New topics are created automatically by {@link Service} implementations
     * when needed
     *
     * @param name The name of the topic to create
     * @throws BrokerException When something went wrong
     */
    public void createTopic(String name) throws BrokerException;

    /**
     * Mandatory method to be implemented by {@link Service} implementations to
     * delete a topic in the broker.
     *
     * It is responsability of a client of the {@link Service} implementation to
     * call this method when a topic has to be deleted
     *
     * @param name The name of the topic to create
     * @throws BrokerException When something went wrong
     */
    public void deleteTopic(String name) throws BrokerException;
}
